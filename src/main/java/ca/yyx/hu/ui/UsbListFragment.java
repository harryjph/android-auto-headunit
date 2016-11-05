package ca.yyx.hu.ui;

import android.app.Fragment;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

import ca.yyx.hu.R;
import ca.yyx.hu.aap.AapService;
import ca.yyx.hu.connection.UsbDeviceCompat;
import ca.yyx.hu.connection.UsbModeSwitch;
import ca.yyx.hu.connection.UsbReceiver;
import ca.yyx.hu.utils.Settings;

/**
 * @author algavris
 * @date 05/11/2016.
 */

public class UsbListFragment extends BaseFragment implements UsbReceiver.Listener {
    private DeviceAdapter mAdapter;
    private Settings mSettings;
    private UsbReceiver mUsbReceiver;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        RecyclerView recyclerView = (RecyclerView) inflater.inflate(R.layout.fragment_list, container, false);

        Context context = getContext();

        mSettings = new Settings(context);
        mAdapter = new DeviceAdapter(context, mSettings);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(mAdapter);

        mUsbReceiver = new UsbReceiver(this);

        return recyclerView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Set<String> allowDevices = mSettings.getAllowedDevices();
        mAdapter.setData(createDeviceList(allowDevices), allowDevices);
        registerReceiver(mUsbReceiver, UsbReceiver.createFilter());
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
    }

    @Override
    public void onUsbDetach(UsbDevice device) {
        Set<String> allowDevices = mSettings.getAllowedDevices();
        mAdapter.setData(createDeviceList(allowDevices), allowDevices);
    }

    @Override
    public void onUsbAttach(UsbDevice device) {
        Set<String> allowDevices = mSettings.getAllowedDevices();
        mAdapter.setData(createDeviceList(allowDevices), allowDevices);
    }

    @Override
    public void onUsbPermission(boolean granted, boolean connect, UsbDevice device) {
        Set<String> allowDevices = mSettings.getAllowedDevices();
        mAdapter.setData(createDeviceList(allowDevices), allowDevices);
    }

    private static class DeviceViewHolder extends RecyclerView.ViewHolder {
        final Button allowButton;
        final Button startButton;

        DeviceViewHolder(View itemView) {
            super(itemView);
            this.startButton = (Button) itemView.findViewById(android.R.id.button2);
            this.allowButton = (Button) itemView.findViewById(android.R.id.button1);
        }
    }

    private static class DeviceAdapter extends RecyclerView.Adapter<DeviceViewHolder> implements View.OnClickListener {
        private final Context mContext;
        private Set<String> mAllowedDevices;
        private ArrayList<UsbDeviceCompat> mDeviceList;
        private final Settings mSettings;


        DeviceAdapter(Context context, Settings settings) {
            mContext = context;
            mDeviceList = new ArrayList<>();
            mSettings = settings;
        }

        @Override
        public DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.list_item_device, parent, false);
            return new DeviceViewHolder(view);
        }

        @Override
        public void onBindViewHolder(DeviceViewHolder holder, int position) {
            UsbDeviceCompat device = mDeviceList.get(position);

            holder.startButton.setText(Html.fromHtml(String.format(
                    Locale.US, "<b>%1$s</b><br/>%2$s",
                    device.getUniqueName(), device.getDeviceName()
            )));
            holder.startButton.setTag(position);
            holder.startButton.setOnClickListener(this);

            if (device.isInAccessoryMode()) {
                holder.allowButton.setText(R.string.allowed);
                holder.allowButton.setTextColor(mContext.getResources().getColor(R.color.material_green_700));
                holder.allowButton.setEnabled(false);
            } else {
                if (mAllowedDevices.contains(device.getUniqueName())) {
                    holder.allowButton.setText(R.string.allowed);
                    holder.allowButton.setTextColor(mContext.getResources().getColor(R.color.material_green_700));
                } else {
                    holder.allowButton.setText(R.string.ignored);
                    holder.allowButton.setTextColor(mContext.getResources().getColor(R.color.material_orange_700));
                }
                holder.allowButton.setTag(position);
                holder.allowButton.setEnabled(true);
                holder.allowButton.setOnClickListener(this);
            }
        }

        @Override
        public int getItemCount() {
            return mDeviceList.size();
        }

        @Override
        public void onClick(View v) {
            int position = (int) v.getTag();
            UsbDeviceCompat device = mDeviceList.get(position);
            if (v.getId() == android.R.id.button1) {
                if (mAllowedDevices.contains(device.getUniqueName())) {
                    mAllowedDevices.remove(device.getUniqueName());
                } else {
                    mAllowedDevices.add(device.getUniqueName());
                }
                mSettings.allowDevices(mAllowedDevices);
                notifyDataSetChanged();
            } else {
                if (device.isInAccessoryMode()) {
                    mContext.startService(AapService.createIntent(device.getWrappedDevice(), mContext));
                } else {
                    UsbModeSwitch usbMode = new UsbModeSwitch((UsbManager) mContext.getSystemService(Context.USB_SERVICE));
                    if (usbMode.switchMode(device.getWrappedDevice())) {
                        Toast.makeText(mContext, "Success", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(mContext, "Failed", Toast.LENGTH_SHORT).show();
                    }
                    notifyDataSetChanged();
                }
            }
        }

        void setData(ArrayList<UsbDeviceCompat> deviceList, Set<String> allowedDevices) {
            mAllowedDevices = allowedDevices;
            mDeviceList = deviceList;
            notifyDataSetChanged();
        }
    }

    private ArrayList<UsbDeviceCompat> createDeviceList(final Set<String> allowDevices) {
        UsbManager manager = (UsbManager) getContext().getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> devices = manager.getDeviceList();
        ArrayList<UsbDeviceCompat> list = new ArrayList<>(devices.size());

        for (String name : devices.keySet()) {
            UsbDevice device = devices.get(name);
            UsbDeviceCompat compat = new UsbDeviceCompat(device);
            list.add(compat);
        }

        Collections.sort(list, new Comparator<UsbDeviceCompat>() {
            @Override
            public int compare(UsbDeviceCompat lhs, UsbDeviceCompat rhs) {
                if (lhs.isInAccessoryMode()) {
                    return -1;
                }
                if (rhs.isInAccessoryMode()) {
                    return 1;
                }
                if (allowDevices.contains(lhs.getUniqueName())) {
                    return -1;
                }
                if (allowDevices.contains(rhs.getUniqueName())) {
                    return 1;
                }
                return lhs.getUniqueName().compareTo(rhs.getUniqueName());
            }
        });

        return list;
    }
}
