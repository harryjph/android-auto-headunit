package ca.yyx.hu;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import net.hockeyapp.android.UpdateManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;

import ca.yyx.hu.usb.UsbDeviceCompat;
import ca.yyx.hu.utils.Settings;
import ca.yyx.hu.utils.SystemUI;

public class SettingsActivity extends Activity {
    private Settings mSettings;
    private DeviceAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mSettings = new Settings(this);
        mAdapter = new DeviceAdapter(this, mSettings);

        RecyclerView recyclerView = (RecyclerView) findViewById(android.R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mAdapter);

        UpdateManager.register(this);
    }

    private ArrayList<UsbDeviceCompat> createDeviceList(final Set<String> allowDevices) {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
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
                    return 1;
                }
                if (rhs.isInAccessoryMode()) {
                    return -1;
                }
                if (allowDevices.contains(lhs.getUniqueName())) {
                    if (allowDevices.contains(rhs.getUniqueName())) {
                        return lhs.getUniqueName().compareTo(rhs.getUniqueName());
                    }
                    return 1;
                }
                if (allowDevices.contains(rhs.getUniqueName())) {
                    return 1;
                }
                return lhs.getUniqueName().compareTo(rhs.getUniqueName());
            }
        });

        return list;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        SystemUI.hide(getWindow().getDecorView());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Set<String> allowDevices = mSettings.getAllowedDevices();
        mAdapter.setData(createDeviceList(allowDevices), allowDevices);
    }

    private static class DeviceViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView subtitle;
        final Button button;

        DeviceViewHolder(View itemView) {
            super(itemView);
            this.title = (TextView) itemView.findViewById(android.R.id.text1);
            this.subtitle = (TextView) itemView.findViewById(android.R.id.text2);
            this.button = (Button) itemView.findViewById(android.R.id.button1);
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
            holder.title.setText(device.getUniqueName());
            holder.subtitle.setText(device.getDeviceName());
            if (device.isInAccessoryMode()) {
                holder.button.setText(R.string.allowed);
                holder.button.setTextColor(mContext.getResources().getColor(R.color.material_green_700));
                holder.button.setEnabled(false);
            } else {
                if (mAllowedDevices.contains(device.getUniqueName())) {
                    holder.button.setText(R.string.allowed);
                    holder.button.setTextColor(mContext.getResources().getColor(R.color.material_green_700));
                } else {
                    holder.button.setText(R.string.ignored);
                    holder.button.setTextColor(mContext.getResources().getColor(R.color.material_orange_700));
                }
                holder.button.setTag(position);
                holder.button.setEnabled(true);
                holder.button.setOnClickListener(this);
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
            if (mAllowedDevices.contains(device.getUniqueName())) {
                mAllowedDevices.remove(device.getUniqueName());
            } else {
                mAllowedDevices.add(device.getUniqueName());
            }
            mSettings.allowDevices(mAllowedDevices);
            notifyDataSetChanged();
        }

        void setData(ArrayList<UsbDeviceCompat> deviceList, Set<String> allowedDevices)
        {
            mAllowedDevices = allowedDevices;
            mDeviceList = deviceList;
            notifyDataSetChanged();
        }
    }
}
