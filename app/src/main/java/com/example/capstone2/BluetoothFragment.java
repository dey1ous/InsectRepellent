package com.example.capstone2;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.capstone2.database.AppDatabase;
import com.example.capstone2.entities.Device;

import java.util.ArrayList;
import java.util.Set;

public class BluetoothFragment extends Fragment {

    private static final int REQUEST_BT_PERMISSIONS = 101;

    private BluetoothAdapter bluetoothAdapter;
    private ListView listView;
    private Button btnScan, btnDisconnect;
    private ArrayAdapter<String> adapter;
    private final ArrayList<BluetoothDevice> devicesList = new ArrayList<>();
    private AppDatabase db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bluetooth, container, false);

        listView = view.findViewById(R.id.listViewDevices);
        btnScan = view.findViewById(R.id.btnScan);
        btnDisconnect = view.findViewById(R.id.btnDisconnect);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        db = AppDatabase.getInstance(requireContext());

        // ⭐ RECOMMENDATION: Robustness - Check context before creating adapter
        Context context = getContext();
        if (context != null) {
            adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1);
            listView.setAdapter(adapter);
        }

        btnScan.setOnClickListener(v -> {
            // ⭐ RECOMMENDATION: Streamlined Logic - Check permissions before loading
            if (hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                loadPairedDevices();
            } else {
                showToast("Bluetooth permissions are required to scan.");
                requestBluetoothPermissions(); // Ask again if needed
            }
        });

        btnDisconnect.setOnClickListener(v -> disconnectDevice());

        listView.setOnItemClickListener((parent, view1, position, id) -> {
            if (devicesList.isEmpty() || position >= devicesList.size()) {
                showToast("No valid device selected");
                return;
            }

            BluetoothDevice device = devicesList.get(position);
            String name = (device.getName() != null) ? device.getName() : "Unknown Device";
            String mac = device.getAddress();
            showToast("Connecting to " + name);

            // ⭐ RECOMMENDATION: Performance - Use the shared executor for database operations
            AppDatabase.databaseWriteExecutor.execute(() -> {
                Device existing = db.deviceDao().findByMac(mac);
                if (existing == null) {
                    db.deviceDao().insert(new Device(name, mac));
                }
            });

            // Connect via MainActivity (ensure it has permission first)
            if (getActivity() instanceof MainActivity && hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                ((MainActivity) getActivity()).connectToDevice(mac);
            } else {
                showToast("Cannot connect without Bluetooth permissions.");
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // ⭐ RECOMMENDATION: Lifecycle awareness - Load devices when fragment is visible
        // This ensures the list is populated if permissions were already granted.
        if (hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            loadPairedDevices();
        } else {
            // If permissions aren't granted when the screen is shown, ask for them.
            requestBluetoothPermissions();
        }
    }

    private void requestBluetoothPermissions() {
        requestPermissions(new String[]{
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION
        }, REQUEST_BT_PERMISSIONS);
    }

    private boolean hasPermission(String perm) {
        if (getContext() == null) {
            return false;
        }
        return ActivityCompat.checkSelfPermission(getContext(), perm) == PackageManager.PERMISSION_GRANTED;
    }

    private void loadPairedDevices() {
        if (bluetoothAdapter == null) {
            showToast("Bluetooth is not supported on this device.");
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            // Prompt user to enable Bluetooth
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            showToast("Please enable Bluetooth and try again.");
            return;
        }

        // This check is required by the system
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            showToast("Cannot load devices without permission.");
            return;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        adapter.clear();
        devicesList.clear();

        if (pairedDevices != null && !pairedDevices.isEmpty()) {
            for (BluetoothDevice device : pairedDevices) {
                String name = (device.getName() != null) ? device.getName() : "Unnamed Device";
                adapter.add("🟢 " + name + "\n" + device.getAddress());
                devicesList.add(device);
            }
        } else {
            // Consider moving this to strings.xml for best practice
            adapter.add("No paired devices found. Please pair a device in your phone's Bluetooth settings first.");
        }
    }

    private void disconnectDevice() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).disconnectFromDevice();
        }
    }

    private void showToast(String msg) {
        // ⭐ RECOMMENDATION: Robustness - Check context before showing a Toast
        if (getContext() != null) {
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BT_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            // ⭐ RECOMMENDATION: Streamlined Logic - Load devices only after permissions are granted.
            if (allGranted) {
                loadPairedDevices();
            } else {
                showToast("Bluetooth permission denied. Cannot show devices.");
            }
        }
    }
}