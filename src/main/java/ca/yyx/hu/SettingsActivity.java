package ca.yyx.hu;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

import ca.yyx.hu.aap.AapProjectionActivity;
import ca.yyx.hu.aap.AapService;
import ca.yyx.hu.connection.UsbDeviceCompat;
import ca.yyx.hu.connection.UsbModeSwitch;
import ca.yyx.hu.connection.UsbReceiver;
import ca.yyx.hu.utils.Settings;
import ca.yyx.hu.utils.SystemUI;

public class SettingsActivity extends Activity implements UsbReceiver.Listener {
    private Settings mSettings;
    private DeviceAdapter mAdapter;
    private BroadcastReceiver mUsbReceiver;
    private View mVideoButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mVideoButton = findViewById(R.id.video_button);
        mVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent aapIntent = new Intent(SettingsActivity.this, AapProjectionActivity.class);
                aapIntent.putExtra(AapProjectionActivity.EXTRA_FOCUS, true);
                startActivity(aapIntent);
            }
        });

        findViewById(R.id.exit_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(new Intent(SettingsActivity.this, AapService.class));
                finish();
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        findViewById(R.id.menu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMenuDialog();
            }
        });


        findViewById(R.id.wifi).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startService(AapService.createIntent("10.0.0.11", SettingsActivity.this));
            }
        });

        mSettings = new Settings(this);
        mAdapter = new DeviceAdapter(this, mSettings);

        RecyclerView recyclerView = (RecyclerView) findViewById(android.R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mAdapter);

        mUsbReceiver = new UsbReceiver(this);

        UpdateManager.register(this);
    }

    private void showMenuDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.menu)
                .setItems(R.array.menu_items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0)
                        {
                            startActivity(new Intent(SettingsActivity.this, VideoTestActivity.class));
                        }
                    }
                }).create();
        dialog.show();
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
        registerReceiver(mUsbReceiver, UsbReceiver.createFilter());
        if (App.get(this).transport().isAlive()) {
            mVideoButton.setEnabled(true);
        } else {
            mVideoButton.setEnabled(false);
        }
        CrashManager.register(this);
    }

    @Override
    protected void onPause() {
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
}
