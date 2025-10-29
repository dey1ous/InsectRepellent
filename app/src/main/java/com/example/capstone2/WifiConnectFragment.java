package com.example.capstone2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView; // Required for txtStatus
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class WifiConnectFragment extends Fragment {

    // UI Components matching the XML
    private EditText ipAddressInput;
    private Button btnConnectIP;
    private Button btnDisconnect;
    private TextView txtStatus;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // NOTE: Ensure your fragment XML file is correctly linked here.
        View view = inflater.inflate(R.layout.fragment_bluetooth, container, false);

        // 1. Initialize UI Elements
        ipAddressInput = view.findViewById(R.id.ipAddressInput);
        btnConnectIP = view.findViewById(R.id.btnConnectIP);
        btnDisconnect = view.findViewById(R.id.btnDisconnect);
        txtStatus = view.findViewById(R.id.txtStatus);

        // 2. Set Default IP and Status
        if (getActivity() instanceof MainActivity) {
            // Load the current IP being polled by MainActivity
            ipAddressInput.setText(((MainActivity) getActivity()).getConnectedIP());
            // Update UI based on MainActivity's current state
            updateStatusText();
        } else {
            // Set a common ESP32 default if context isn't ready
            ipAddressInput.setText("192.168.4.1");
        }

        // 3. Set Listeners
        btnConnectIP.setOnClickListener(v -> connectToIP());
        btnDisconnect.setOnClickListener(v -> disconnectDevice());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update status whenever the fragment becomes visible
        updateStatusText();
    }

    // ------------------ CONNECT / DISCONNECT LOGIC ------------------

    private void connectToIP() {
        String ipAddress = ipAddressInput.getText().toString().trim();

        if (ipAddress.isEmpty()) {
            showToast("Please enter the ESP32 IP address.");
            return;
        }

        // Basic IP format validation
        if (!ipAddress.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) {
            showToast("Invalid IP format.");
            return;
        }

        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();

            // 1. Set the new IP in MainActivity
            activity.setConnectedIP(ipAddress);

            // 2. Start the scheduled data polling
            activity.startPollingData();

            showToast("Polling attempts started for " + ipAddress);
            updateStatusText();
        }
    }

    private void disconnectDevice() {
        if (getActivity() instanceof MainActivity) {
            // Stop the scheduled data polling
            ((MainActivity) getActivity()).stopPollingData();
            showToast("Polling stopped.");
            updateStatusText();
        }
    }

    // ------------------ UI UTILITIES ------------------

    /**
     * Updates the status TextView based on MainActivity's polling state.
     */
    // ⭐ Helper method to reflect the current polling status
    private void updateStatusText() {
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();

            // 🚨 FIX HERE: Call the public method isPolling() instead of accessing the private variable directly.
            boolean isPollingNow = activity.isPolling();

            String ip = activity.getConnectedIP();

            if (isPollingNow) {
                txtStatus.setText("Status: Polling Data at " + ip);
                txtStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else {
                txtStatus.setText("Status: Not Polling. Last IP: " + ip);
                txtStatus.setTextColor(getResources().getColor(android.R.color.black));
            }
        }
    }

    private void showToast(String msg) {
        if (getContext() != null) {
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }
}