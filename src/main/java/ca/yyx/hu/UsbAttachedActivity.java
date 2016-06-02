package ca.yyx.hu;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.widget.Toast;

import java.util.HashMap;

import ca.yyx.hu.usb.UsbAccessoryConnection;
import ca.yyx.hu.usb.UsbDeviceCompat;
import ca.yyx.hu.usb.UsbModeSwitch;
import ca.yyx.hu.utils.IntentUtils;
import ca.yyx.hu.utils.Settings;
import ca.yyx.hu.utils.Utils;

/**
 * @author algavris
 * @date 30/05/2016.
 */
public class UsbAttachedActivity extends Activity {

    private UsbManager mUsbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.logd("USB Intent: " + getIntent());

        UsbDevice device = IntentUtils.getDevice(getIntent());
        if (device == null) {
            Utils.loge("No USB device");
            finish();
            return;
        }

        if (UsbDeviceCompat.isInAccessoryMode(device)) {
            Utils.loge("Usb in accessory mode");
            HeadUnitActivity.start(device, this);
            finish();
            return;
        }

        UsbDeviceCompat deviceCompat = new UsbDeviceCompat(device);
        Settings settings = new Settings(this);
        if (!settings.isConnectingDevice(deviceCompat)) {
            Utils.logd("Skipping device " + deviceCompat.getUniqueName());
            finish();
            return;
        }

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        UsbModeSwitch usbMode = new UsbModeSwitch(mUsbManager);
        Utils.logd("Switching USB device to accessory mode " + deviceCompat.getUniqueName());
        Toast.makeText(this, "Switching USB device to accessory mode " + deviceCompat.getUniqueName(), Toast.LENGTH_SHORT).show();
        if (usbMode.switchMode(device)) {
            Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
        }

        finish();
        // Wait for onNewIntent
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        UsbDevice device = IntentUtils.getDevice(getIntent());
        if (device == null) {
            Utils.loge("No USB device");
            finish();
            return;
        }

        Utils.logd(UsbDeviceCompat.getUniqueName(device));

        if (UsbDeviceCompat.isInAccessoryMode(device)) {
            Utils.loge("Usb in accessory mode");
            HeadUnitActivity.start(device, this);
        }

        finish();
    }
}
