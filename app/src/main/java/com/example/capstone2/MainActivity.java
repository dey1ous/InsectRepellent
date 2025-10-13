package com.example.capstone2;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.capstone2.database.AppDatabase;
import com.example.capstone2.databinding.ActivityMainBinding;
import com.example.capstone2.entities.Device;
import com.example.capstone2.entities.Detection;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Main Activity
 * Handles fragment navigation, Bluetooth connection, and data syncing.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private AppDatabase db;

    private String macAddress;       // MAC address from QR or DB
    private BluetoothSocket socket;  // Active HC-05 socket
    private boolean readingData = false;

    private static final UUID HC05_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Fragments
    private HomeFragment homeFragment;
    private AboutFragment aboutFragment;
    private HistoryFragment historyFragment;
    private SettingsFragment settingsFragment;

    private int latestCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);

        // Initialize fragments
        homeFragment = new HomeFragment();
        aboutFragment = new AboutFragment();
        historyFragment = new HistoryFragment();
        settingsFragment = new SettingsFragment();

        // Load initial fragment
        replaceFragment(homeFragment, "HOME_FRAGMENT");

        // Try to get MAC address from QR
        macAddress = getIntent().getStringExtra("DEVICE_QR");
        if (macAddress != null && !macAddress.isEmpty()) {
            saveDeviceIfNew(macAddress);
            Toast.makeText(this, "Scanned device: " + macAddress, Toast.LENGTH_LONG).show();
            connectToBluetooth(macAddress);
        } else {
            // Load saved MAC from DB if available
            new Thread(() -> {
                Device savedDevice = db.deviceDao().getRegisteredDevice();
                runOnUiThread(() -> {
                    if (savedDevice != null) {
                        macAddress = savedDevice.getQrCode();
                        Toast.makeText(this, "Loaded saved device: " + macAddress, Toast.LENGTH_LONG).show();
                        connectToBluetooth(macAddress);
                    } else {
                        Toast.makeText(this, "No device found. Please scan QR first.", Toast.LENGTH_LONG).show();
                    }
                });
            }).start();
        }

        fetchLatestCount();

        // Bottom Navigation
        binding.bottomNavigationView.setBackground(null);
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.home) {
                replaceFragment(homeFragment, "HOME_FRAGMENT");
                homeFragment.updateInsectCount(latestCount);
            } else if (id == R.id.about) {
                replaceFragment(aboutFragment, "ABOUT_FRAGMENT");
            } else if (id == R.id.history) {
                replaceFragment(historyFragment, "HISTORY_FRAGMENT");
            } else if (id == R.id.settings) {
                replaceFragment(settingsFragment, "SETTINGS_FRAGMENT");
            }
            return true;
        });

        // Floating Refresh Button → Refresh current fragment only
        FloatingActionButton fab = findViewById(R.id.fab_refresh);
        fab.setOnClickListener(v -> refreshCurrentFragment());
    }

    /**
     * Save the MAC address to Room DB if not already saved.
     */
    private void saveDeviceIfNew(String qrCode) {
        new Thread(() -> {
            Device existing = db.deviceDao().getRegisteredDevice();
            if (existing == null || !existing.getQrCode().equals(qrCode)) {
                Device device = new Device(qrCode); // Pass only the String
                db.deviceDao().insert(device);
                Log.d("DB_SAVE", "Device MAC saved: " + qrCode);
            }
        }).start();
    }

    /**
     * Refreshes only the currently active fragment.
     */
    private void refreshCurrentFragment() {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.frame_layout);
        if (current instanceof HomeFragment) {
            fetchLatestCount();
            Toast.makeText(this, "Home refreshed", Toast.LENGTH_SHORT).show();
        } else if (current instanceof AboutFragment) {
            aboutFragment.onResume();
            Toast.makeText(this, "About refreshed", Toast.LENGTH_SHORT).show();
        } else if (current instanceof HistoryFragment) {
            historyFragment.onResume();
            Toast.makeText(this, "History refreshed", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Nothing to refresh here", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Replace fragments smoothly.
     */
    private void replaceFragment(Fragment fragment, String tag) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.frame_layout, fragment, tag);
        ft.commitAllowingStateLoss();
    }

    /**
     * Connect to HC-05 Bluetooth dynamically using MAC from QR or DB.
     */
    private void connectToBluetooth(String mac) {
        mac = mac.replace("MAC:", "").trim();
        if (mac.length() == 12 && !mac.contains(":")) {
            mac = mac.replaceAll("(.{2})(?!$)", "$1:");
        }

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
            }, 1);
            return;
        }

        BluetoothDevice device;
        try {
            device = adapter.getRemoteDevice(mac);
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, "Invalid MAC: " + mac, Toast.LENGTH_LONG).show();
            return;
        }

        String finalMac = mac;
        new Thread(() -> {
            try {
                socket = device.createRfcommSocketToServiceRecord(HC05_UUID);
                adapter.cancelDiscovery();
                socket.connect();

                runOnUiThread(() ->
                        Toast.makeText(this, "✅ Connected to HC-05: " + finalMac, Toast.LENGTH_SHORT).show()
                );

                startReading(socket);

            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Connection failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    /**
     * Reads serial data from HC-05 and updates UI + DB.
     */
    private void startReading(BluetoothSocket socket) {
        readingData = true;

        try {
            InputStream inputStream = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            while (readingData) {
                String line = reader.readLine();
                if (line == null || line.isEmpty()) continue;
                line = line.trim();

                if (line.contains(",")) {
                    // Example: "2,5" (detection code, count)
                    String[] parts = line.split(",");
                    int code = Integer.parseInt(parts[0].trim());
                    int count = Integer.parseInt(parts[1].trim());

                    if (code == 2) {
                        latestCount = count;
                        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                        Detection detection = new Detection(macAddress, timestamp, count);

                        new Thread(() -> db.detectionDao().insert(detection)).start();
                        runOnUiThread(() -> homeFragment.updateInsectCount(count));
                    }

                } else {
                    // Single codes for status/liquid
                    int code = Integer.parseInt(line);
                    if (code == 0 || code == 1) {
                        runOnUiThread(() -> homeFragment.updateSystemStatus(code));
                    } else if (code == 3 || code == 4 || code == 5) {
                        runOnUiThread(() -> homeFragment.updateLiquidStatus(code));
                    }
                }
            }
        } catch (Exception e) {
            Log.e("BT_READ", "Error reading data: " + e.getMessage());
        }
    }

    /**
     * Fetch today's detection count.
     */
    private void fetchLatestCount() {
        new Thread(() -> {
            try {
                int count = db.detectionDao().getTodayTotalCount(macAddress != null ? macAddress : "UNKNOWN");
                latestCount = count;
                runOnUiThread(() -> {
                    if (homeFragment != null) homeFragment.updateInsectCount(latestCount);
                });
            } catch (Exception e) {
                Log.e("DB_FETCH", "Error fetching count", e);
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        readingData = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
