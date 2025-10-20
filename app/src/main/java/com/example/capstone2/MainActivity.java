package com.example.capstone2;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.capstone2.database.AppDatabase;
import com.example.capstone2.databinding.ActivityMainBinding;
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
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "HC05_DEBUG";
    private static final UUID HC05_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final int REQUEST_BT_PERMISSIONS = 101;
    private static final int MAX_RETRIES = 5;

    private ActivityMainBinding binding;
    private AppDatabase db;
    private BluetoothAdapter adapter;
    private BluetoothSocket socket;

    private volatile boolean isReading = false;
    private String connectedMac;
    private int latestCount = 0;

    // ⭐ FIX: Add a flag to prevent multiple simultaneous connection attempts.
    private final AtomicBoolean isConnecting = new AtomicBoolean(false);

    private HomeFragment homeFragment;
    private BluetoothFragment bluetoothFragment;
    private HistoryFragment historyFragment;
    private SettingsFragment settingsFragment;

    public String getConnectedMac() {
        return connectedMac;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        adapter = BluetoothAdapter.getDefaultAdapter();

        homeFragment = new HomeFragment();
        bluetoothFragment = new BluetoothFragment();
        historyFragment = new HistoryFragment();
        settingsFragment = new SettingsFragment();

        replaceFragment(homeFragment, "HOME_FRAGMENT");
        setupBottomNavigation();
        setupFloatingButton();
    }

    // ---------------------- BLUETOOTH ---------------------- //

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
    public void connectToDevice(String macAddress) {
        // ⭐ FIX: Check if a connection is already in progress.
        if (isConnecting.get()) {
            showToast("Connection already in progress...");
            return;
        }

        if (adapter == null) {
            showToast("Bluetooth not supported.");
            return;
        }
        if (!adapter.isEnabled()) {
            showToast("Please enable Bluetooth first.");
            return;
        }
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            connectedMac = macAddress; // Set mac before asking
            requestBluetoothPermissions();
            return;
        }

        connectedMac = macAddress;

        // ⭐ FIX: Set the flag to true BEFORE starting the connection process.
        isConnecting.set(true);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                Thread.sleep(50);
                BluetoothDevice device = adapter.getRemoteDevice(macAddress);
                connectWithRetries(device);
            } catch (IllegalArgumentException | InterruptedException e) {
                isConnecting.set(false); // ⭐ FIX: Reset the flag on failure.
                runOnUiThread(() -> showToast("Invalid MAC address or thread interrupted."));
            }
        });
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
    private void connectWithRetries(BluetoothDevice device) {
        closeSocket();
        cancelDiscovery();

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                socket = device.createInsecureRfcommSocketToServiceRecord(HC05_UUID);
                Log.d(TAG, "Connecting... Attempt " + attempt);
                socket.connect();

                runOnUiThread(() -> showToast("✅ Connected to " + (device.getName() != null ? device.getName() : "Unknown Device")));
                isConnecting.set(false); // ⭐ FIX: Reset the flag on successful connection.
                startReading(socket);
                return;

            } catch (IOException e) {
                Log.w(TAG, "Connection Attempt " + attempt + " failed: " + e.getMessage());
                try {
                    if (socket != null) socket.close();
                } catch (IOException closeIgnored) {
                    // Ignored
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    Log.e(TAG, "Retry loop interrupted.");
                    runOnUiThread(() -> showToast("Connection attempt was cancelled."));
                    isConnecting.set(false); // ⭐ FIX: Reset the flag if interrupted.
                    return;
                }
            }
        }

        Log.e(TAG, "Connection failed after all retries.");
        runOnUiThread(() -> showToast("Connection failed. Please ensure the device is on and paired."));
        isConnecting.set(false); // ⭐ FIX: Reset the flag after all retries fail.
        closeSocket();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void cancelDiscovery() {
        if (adapter != null && adapter.isDiscovering()) {
            adapter.cancelDiscovery();
        }
    }
    private void startReading(BluetoothSocket connectedSocket) {
        isReading = true;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try (InputStream in = connectedSocket.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while (isReading && (line = reader.readLine()) != null) {
                    handleIncomingData(line.trim());
                }
            } catch (IOException e) {
                if(isReading) {
                    Log.e(TAG, "Disconnected: " + e.getMessage());
                    runOnUiThread(() -> showToast("Device disconnected."));
                }
            } finally {
                closeSocket();
            }
        });
    }

    private void handleIncomingData(String line) {
        try {
            if (line.contains(",")) {
                String[] parts = line.split(",");
                int code = Integer.parseInt(parts[0].trim());
                int count = Integer.parseInt(parts[1].trim());

                if (code == 2) {
                    latestCount = count;
                    String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                    Detection detection = new Detection(connectedMac, timestamp, count);

                    AppDatabase.databaseWriteExecutor.execute(() -> db.detectionDao().insert(detection));

                    if(homeFragment != null && homeFragment.isAdded()) {
                        runOnUiThread(() -> homeFragment.updateInsectCount(count));
                    }
                }
            } else {
                int code = Integer.parseInt(line);
                if(homeFragment != null && homeFragment.isAdded()) {
                    runOnUiThread(() -> {
                        if (code == 0 || code == 1) homeFragment.updateSystemStatus(code);
                        else if (code >= 3 && code <= 5) homeFragment.updateLiquidStatus(code);
                    });
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Parse error: " + line + " → " + e.getMessage());
        }
    }

    // ---------------------- DISCONNECT ---------------------- //

    public void disconnectFromDevice() {
        isReading = false;
        closeSocket();
        showToast("Disconnected from device");
    }

    private void closeSocket() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        } finally {
            socket = null;
            isReading = false;
        }
    }

    // ---------------------- UI & NAVIGATION ---------------------- //

    private void setupBottomNavigation() {
        binding.bottomNavigationView.setBackground(null);
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.home) {
                replaceFragment(homeFragment, "HOME_FRAGMENT");
            } else if (id == R.id.bluetooth) {
                replaceFragment(bluetoothFragment, "BLUETOOTH_FRAGMENT");
            } else if (id == R.id.history) {
                replaceFragment(historyFragment, "HISTORY_FRAGMENT");
            } else if (id == R.id.settings) {
                replaceFragment(settingsFragment, "SETTINGS_FRAGMENT");
            }
            return true;
        });
    }

    private void setupFloatingButton() {
        FloatingActionButton fab = findViewById(R.id.fab_refresh);
        fab.setOnClickListener(v -> refreshCurrentFragment());
    }

    private void refreshCurrentFragment() {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.frame_layout);
        if (current instanceof HomeFragment) {
            fetchLatestCount();
            showToast("Home refreshed");
        } else if (current instanceof HistoryFragment) {
            // Let onResume handle the refresh for History
            showToast("History refreshed");
        } else {
            showToast("Nothing to refresh");
        }
    }

    private void replaceFragment(Fragment fragment, String tag) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.frame_layout, fragment, tag);
        ft.commitAllowingStateLoss();
    }

    // ---------------------- PERMISSIONS ---------------------- //

    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, REQUEST_BT_PERMISSIONS);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, REQUEST_BT_PERMISSIONS);
        }
    }

    private boolean hasPermission(String perm) {
        return ActivityCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BT_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                if (connectedMac != null) {
                    connectToDevice(connectedMac);
                }
            } else {
                showToast("Bluetooth permissions are required to connect.");
            }
        }
    }

    private void showToast(String msg) {
        runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectFromDevice();
    }

    // ---------------------- DATA FETCHING ---------------------- //

    public void fetchLatestCount() {
        if (connectedMac == null) return;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // Ensure DAO returns a primitive or handles null
                Integer count = db.detectionDao().getTodayTotalCount(connectedMac);
                latestCount = (count != null) ? count : 0;
                if(homeFragment != null && homeFragment.isAdded()){
                    runOnUiThread(() -> homeFragment.updateInsectCount(latestCount));
                }
            } catch (Exception e) {
                Log.e(TAG, "DB fetch error", e);
            }
        });
    }
}