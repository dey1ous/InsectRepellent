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
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "HC05_DEBUG";
    private static final UUID HC05_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_BT_PERMISSIONS = 101;

    private ActivityMainBinding binding;
    private AppDatabase db;
    private BluetoothAdapter adapter;
    private BluetoothSocket socket;

    private boolean isReading = false;
    private String connectedMac;
    private int latestCount = 0;

    private HomeFragment homeFragment;
    private BluetoothFragment bluetoothFragment;
    private HistoryFragment historyFragment;
    private SettingsFragment settingsFragment;

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

    public void connectToDevice(String macAddress) {
        if (adapter == null) {
            showToast("Bluetooth not supported.");
            return;
        }

        if (!adapter.isEnabled()) {
            showToast("Please enable Bluetooth first.");
            return;
        }

        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            requestBluetoothPermissions();
            return;
        }

        connectedMac = macAddress;

        new Thread(() -> {
            try {
                BluetoothDevice device = adapter.getRemoteDevice(macAddress);
                connectWithFallback(device);
            } catch (IllegalArgumentException e) {
                runOnUiThread(() -> showToast("Invalid MAC address."));
            }
        }).start();
    }

    private void connectWithFallback(BluetoothDevice device) {
        try {
            closeSocket();
            if (adapter.isDiscovering()) adapter.cancelDiscovery();

            // Primary socket attempt
            socket = device.createRfcommSocketToServiceRecord(HC05_UUID);
            socket.connect();
            runOnUiThread(() -> showToast("✅ Connected to " + device.getName()));
            startReading(socket);

        } catch (IOException e) {
            Log.w(TAG, "Primary failed, trying fallback: " + e.getMessage());
            try {
                Method m = device.getClass().getMethod("createRfcommSocket", int.class);
                BluetoothSocket fallback = (BluetoothSocket) m.invoke(device, 1);
                fallback.connect();
                socket = fallback;
                runOnUiThread(() -> showToast("✅ Connected (fallback)"));
                startReading(socket);
            } catch (Exception ex) {
                Log.e(TAG, "Fallback failed: " + ex.getMessage());
                runOnUiThread(() -> showToast("Connection failed. Try again."));
                closeSocket();
            }
        }
    }

    private void startReading(BluetoothSocket socket) {
        isReading = true;
        new Thread(() -> {
            try (InputStream in = socket.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {

                String line;
                while (isReading && (line = reader.readLine()) != null) {
                    handleIncomingData(line.trim());
                }

            } catch (IOException e) {
                Log.e(TAG, "Disconnected: " + e.getMessage());
                runOnUiThread(() -> showToast("Device disconnected."));
                isReading = false;
            }
        }).start();
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
                    new Thread(() -> db.detectionDao().insert(detection)).start();
                    runOnUiThread(() -> homeFragment.updateInsectCount(count));
                }
            } else {
                int code = Integer.parseInt(line);
                runOnUiThread(() -> {
                    if (code == 0 || code == 1) homeFragment.updateSystemStatus(code);
                    else if (code >= 3 && code <= 5) homeFragment.updateLiquidStatus(code);
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Parse error: " + line + " → " + e.getMessage());
        }
    }

    // ---------------------- DISCONNECT ---------------------- //

    public void disconnectFromDevice() {
        try {
            if (socket != null) {
                socket.close();
                socket = null;
                isReading = false;
                showToast("Disconnected from device");
            } else {
                showToast("No active connection");
            }
        } catch (IOException e) {
            showToast("Error disconnecting");
        }
    }

    private void closeSocket() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
        socket = null;
        isReading = false;
    }

    // ---------------------- UI ---------------------- //

    private void setupBottomNavigation() {
        binding.bottomNavigationView.setBackground(null);
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.home) {
                replaceFragment(homeFragment, "HOME_FRAGMENT");
                fetchLatestCount();
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
            historyFragment.onResume();
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
            boolean granted = true;
            for (int result : grantResults)
                if (result != PackageManager.PERMISSION_GRANTED) granted = false;

            if (granted && connectedMac != null) connectToDevice(connectedMac);
            else showToast("Bluetooth permission denied.");
        }
    }

    private void showToast(String msg) {
        runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isReading = false;
        closeSocket();
    }

    // ---------------------- FETCH COUNT ---------------------- //

    private void fetchLatestCount() {
        if (connectedMac == null) return;
        new Thread(() -> {
            try {
                int count = db.detectionDao().getTodayTotalCount(connectedMac);
                latestCount = count;
                runOnUiThread(() -> homeFragment.updateInsectCount(count));
            } catch (Exception e) {
                Log.e(TAG, "DB fetch error", e);
            }
        }).start();
    }
}
