package ca.yyx.hu;

import android.app.Application;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.support.v4.content.LocalBroadcastManager;

import ca.yyx.hu.usb.UsbAccessoryConnection;
import ca.yyx.hu.usb.UsbDeviceCompat;
import ca.yyx.hu.usb.UsbReceiver;
import ca.yyx.hu.utils.IntentUtils;
import ca.yyx.hu.utils.Utils;

/**
 * @author algavris
 * @date 30/05/2016.
 */

public class App extends Application implements UsbReceiver.Listener {

    private UsbAccessoryConnection mUsbAccessoryConnection;
    private UsbReceiver mUsbReceiver;

    public static App get(Context context)
    {
        return (App)context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mUsbAccessoryConnection = new UsbAccessoryConnection(usbManager);

        mUsbReceiver = new UsbReceiver(this);
        registerReceiver(mUsbReceiver, UsbReceiver.createFilter());
    }

    public boolean connect(UsbDeviceCompat device) throws UsbAccessoryConnection.UsbOpenException {
        if (mUsbAccessoryConnection.isConnected())
        {
            if (mUsbAccessoryConnection.isDeviceRunning(device)) {
                Utils.logd("Device already connected");
                return true;
            }
        }
        return mUsbAccessoryConnection.connect(device);
    }

    @Override
    public void onUsbDetach(UsbDeviceCompat deviceCompat) {
        if (mUsbAccessoryConnection.isDeviceRunning(deviceCompat)) {
            mUsbAccessoryConnection.disconnect();
            LocalBroadcastManager.getInstance(this).sendBroadcast(IntentUtils.ACTION_DISCONNECT);
        }
    }

    @Override
    public void onUsbAttach(UsbDeviceCompat deviceCompat) {
//        if (deviceCompat.isInAccessoryMode()) {
//            HeadUnitActivity.start(deviceCompat.getWrappedDevice(), this);
//        }
    }

    @Override
    public void onUsdPermission(boolean granted, boolean connect, UsbDeviceCompat deviceCompat) {

    }

    public UsbAccessoryConnection connection() {
        return mUsbAccessoryConnection;
    }
}
