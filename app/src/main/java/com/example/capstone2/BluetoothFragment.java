package com.example.capstone2;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bluetooth, container, false);

        listView = view.findViewById(R.id.listViewDevices);
        btnScan = view.findViewById(R.id.btnScan);
        btnDisconnect = view.findViewById(R.id.btnDisconnect);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        db = AppDatabase.getInstance(requireContext());

        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1);
        listView.setAdapter(adapter);

        requestBluetoothPermissions();

        btnScan.setOnClickListener(v -> loadPairedDevices());
        btnDisconnect.setOnClickListener(v -> disconnectDevice());

        listView.setOnItemClickListener((parent, view1, position, id) -> {
            if (devicesList.isEmpty() || position >= devicesList.size()) {
                showToast("No valid device selected");
                return;
            }

            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                requestBluetoothPermissions();
                showToast("Bluetooth permission required");
                return;
            }

            BluetoothDevice device = devicesList.get(position);
            if (device == null) {
                showToast("Invalid device");
                return;
            }

            String name = (device.getName() != null) ? device.getName() : "Unknown Device";
            String mac = device.getAddress();
            showToast("Connecting to " + name);

            // Save device to database if not exists
            new Thread(() -> {
                Device existing = db.deviceDao().findByMac(mac);
                if (existing == null) {
                    db.deviceDao().insert(new Device(name, mac));
                }
            }).start();

            // Connect via MainActivity
            ((MainActivity) requireActivity()).connectToDevice(mac);
        });

        loadPairedDevices();
        return view;
    }

    private void requestBluetoothPermissions() {
        requestPermissions(new String[]{
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION
        }, REQUEST_BT_PERMISSIONS);
    }

    private boolean hasPermission(String perm) {
        return ActivityCompat.checkSelfPermission(requireContext(), perm)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void loadPairedDevices() {
        if (bluetoothAdapter == null) {
            showToast("Bluetooth not supported");
            return;
        }

        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            requestBluetoothPermissions();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            return;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        adapter.clear();
        devicesList.clear();

        if (pairedDevices != null && !pairedDevices.isEmpty()) {
            for (BluetoothDevice device : pairedDevices) {
                String name = (device.getName() != null) ? device.getName() : "Unnamed";
                adapter.add("🟢 " + name + "\n" + device.getAddress());
                devicesList.add(device);
            }
        } else {
            adapter.add("No paired devices found");
        }
    }

    private void disconnectDevice() {
        ((MainActivity) requireActivity()).disconnectFromDevice();
        showToast("Disconnected");
    }

    private void showToast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BT_PERMISSIONS) {
            boolean granted = true;
            for (int result : grantResults)
                if (result != PackageManager.PERMISSION_GRANTED) granted = false;
            if (!granted) showToast("Bluetooth permission denied.");
        }
    }
}
