package ca.yyx.hu;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.widget.Toast;

import java.util.HashMap;

import ca.yyx.hu.usb.UsbAccessoryConnection;
import ca.yyx.hu.usb.UsbDeviceCompat;
import ca.yyx.hu.usb.UsbReceiver;
import ca.yyx.hu.utils.Settings;
import ca.yyx.hu.utils.Utils;

/**
 * @author algavris
 * @date 30/05/2016.
 */
public class UsbAttachedActivity extends Activity {

    private UsbManager mUsbManager;
    private UsbAccessoryConnection mUsbAccessoryConnection;
    private UsbDeviceCompat mUsbDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent == null)
        {
            Utils.loge("No intent");
            finish();
            return;
        }

        UsbDevice device = intent.<UsbDevice>getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (device == null) {
            Utils.loge("No USB device");
            finish();
            return;
        }

        if (UsbDeviceCompat.isInAccessoryMode(device)) {
            Utils.loge("Starting with accessory");
            HeadUnitActivity.start(device, this);
            finish();
            return;
        }

        mUsbDevice = new UsbDeviceCompat(device);
        Settings settings = new Settings(this);
        if (!settings.isConnectingDevice(mUsbDevice))
        {
            Utils.logd("Skipping device " + mUsbDevice.getUniqueName());
            finish();
            return;
        }

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mUsbAccessoryConnection = new UsbAccessoryConnection(mUsbManager);
        connect(mUsbDevice);
        finish();
    }

    private void connect(UsbDeviceCompat deviceCompat) {
        Toast.makeText(this, "Connecting to USB device " + deviceCompat.getUniqueName(), Toast.LENGTH_SHORT).show();

        try {
            mUsbAccessoryConnection.switchMode(deviceCompat);
        } catch (UsbAccessoryConnection.UsbOpenException e) {
            Toast.makeText(this, "["  + deviceCompat.getUniqueName() + "] Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

}
