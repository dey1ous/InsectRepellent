package com.example.capstone2;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class WifiConnectFragment extends Fragment {

    // UI Components
    private Button btnConnectMQTT;
    private Button btnDisconnect;
    private TextView txtStatus;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wifi_connect, container, false);

        // 1. Initialize UI Elements
        btnConnectMQTT = view.findViewById(R.id.btnConnectIP); // Ensure this ID matches your XML
        btnDisconnect = view.findViewById(R.id.btnDisconnect);
        txtStatus = view.findViewById(R.id.txtStatus);

        // 2. Set Listeners
        btnConnectMQTT.setOnClickListener(v -> connectToBroker());
        btnDisconnect.setOnClickListener(v -> disconnectDevice());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check if service is running when we open this screen
        updateStatusText();
    }

    // ------------------ CONNECT / DISCONNECT LOGIC ------------------

    private void connectToBroker() {
        if (getContext() == null) return;

        // Start the Service directly
        Intent serviceIntent = new Intent(getContext(), InsectMonitorService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getContext().startForegroundService(serviceIntent);
        } else {
            getContext().startService(serviceIntent);
        }

        showToast("Starting Insect Monitor Service...");

        // Update UI after a short delay to allow service to start
        txtStatus.postDelayed(this::updateStatusText, 500);
    }

    private void disconnectDevice() {
        if (getContext() == null) return;

        // Stop the Service
        Intent serviceIntent = new Intent(getContext(), InsectMonitorService.class);
        getContext().stopService(serviceIntent);

        showToast("Service Stopped.");
        updateStatusText();
    }

    // ------------------ UI UTILITIES ------------------

    private void updateStatusText() {
        boolean isRunning = isServiceRunning(InsectMonitorService.class);

        if (isRunning) {
            txtStatus.setText("Status: Monitoring Active\n(Service Running)");
            txtStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            btnConnectMQTT.setEnabled(false); // Disable connect button if already running
            btnDisconnect.setEnabled(true);
        } else {
            txtStatus.setText("Status: Stopped");
            txtStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            btnConnectMQTT.setEnabled(true);
            btnDisconnect.setEnabled(false);
        }
    }

    /**
     * Helper method to check if the InsectMonitorService is currently running in background.
     */
    private boolean isServiceRunning(Class<?> serviceClass) {
        if (getContext() == null) return false;

        ActivityManager manager = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            // Check through running services
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void showToast(String msg) {
        if (getContext() != null) {
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }
}