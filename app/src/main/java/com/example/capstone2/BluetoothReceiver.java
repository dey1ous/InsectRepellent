package com.example.capstone2;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class BluetoothReceiver extends BroadcastReceiver {

    public interface DeviceFoundListener {
        void onDeviceFound(BluetoothDevice device);
    }

    private final DeviceFoundListener listener;

    public BluetoothReceiver(DeviceFoundListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null) listener.onDeviceFound(device);
        }
    }

    public static IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        return filter;
    }
}
