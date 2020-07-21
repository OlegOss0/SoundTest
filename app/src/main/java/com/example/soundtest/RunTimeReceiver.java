package com.example.soundtest;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

public class RunTimeReceiver extends BroadcastReceiver implements SimpleBroadcastReceiver {

    public static final String USB_DEVICE_ATTACHED = UsbManager.ACTION_USB_DEVICE_ATTACHED;
    public static final String USB_DEVICE_DETACHED = UsbManager.ACTION_USB_DEVICE_DETACHED;
    private static final int REFRESH_DEVICE_LIST_DELAY = 5000;

    private static final String TAG = RunTimeReceiver.class.getSimpleName();
    private static final boolean PRINT_LOG = false;
    private MainActivity mainActivity;

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (action.equals(USB_DEVICE_ATTACHED)) {
            onUsbDeviceAttached(intent);
        } else if (action.equals(USB_DEVICE_DETACHED)) {
            onUsbDeviceDetached(intent);
        }
    }

    private void onUsbDeviceAttached(Intent intent) {
        final UsbDevice connectedDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        Log.i(TAG, "USB device attached: " + (connectedDevice != null ? connectedDevice.toString() : ""));
        App.getHandler().postDelayed(() ->{
            mainActivity.initUI(true);
        },REFRESH_DEVICE_LIST_DELAY);
    }

    private void onUsbDeviceDetached(Intent intent) {
        final UsbDevice detachedDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        Log.d(TAG, "USB device detached: " + (detachedDevice != null ? detachedDevice.toString() : ""));
        App.getHandler().postDelayed(() ->{
            mainActivity.initUI(true);
        },REFRESH_DEVICE_LIST_DELAY);
    }

    @Override
    public void register(MainActivity mainActivity) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(USB_DEVICE_ATTACHED);
        intentFilter.addAction(USB_DEVICE_DETACHED);
        App.getAppContext().registerReceiver(this, intentFilter);
        this.mainActivity = mainActivity;
    }

    @Override
    public void unregister() {
        try {
            mainActivity = null;
            App.getAppContext().unregisterReceiver(this);
        } catch (Exception e) {

        }
    }
}
