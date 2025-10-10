package com.example.capstone2;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private String deviceQr;
    private BluetoothSocket socket;
    private boolean readingData = true;

    private static final String HC05_MAC = "06:A3:B2:B7:CD:B9";
    private static final UUID HC05_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private AppDatabase db;
    private HomeFragment homeFragment;
    private int latestCount = 0; // Store latest insect count

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        deviceQr = getIntent().getStringExtra("DEVICE_QR");

        if (deviceQr != null) {
            Toast.makeText(this, "Device QR: " + deviceQr, Toast.LENGTH_LONG).show();
        }

        // Initialize HomeFragment
        homeFragment = new HomeFragment();
        replaceFragment(homeFragment, "HOME_FRAGMENT");

        // Fetch latest count from database
        fetchLatestCount();

        // Bottom navigation logic
        binding.bottomNavigationView.setBackground(null);
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.home) {
                replaceFragment(homeFragment, "HOME_FRAGMENT");
                homeFragment.updateInsectCount(latestCount); // Update UI
            } else if (itemId == R.id.about) {
                replaceFragment(new AboutFragment(), "ABOUT_FRAGMENT");
            } else if (itemId == R.id.history) {
                replaceFragment(new HistoryFragment(), "HISTORY_FRAGMENT");
            } else if (itemId == R.id.settings) {
                replaceFragment(new SettingsFragment(), "SETTINGS_FRAGMENT");
            }
            return true;
        });

        // Refresh button logic
        FloatingActionButton fab = findViewById(R.id.fab_refresh);
        fab.setOnClickListener(v -> {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.frame_layout);
            if (currentFragment instanceof HomeFragment) {
                fetchLatestCount(); // Refresh from DB
                Toast.makeText(this, "Home refreshed", Toast.LENGTH_SHORT).show();
            }
        });

        // Start Bluetooth connection
        connectToBluetooth();
    }

    /**
     * Fetches the latest insect count from the database and updates HomeFragment
     */
    private void fetchLatestCount() {
        new Thread(() -> {
            try {
                int count = db.detectionDao().getTodayTotalCount(deviceQr != null ? deviceQr : "UNKNOWN");
                latestCount = count;

                runOnUiThread(() -> {
                    if (homeFragment != null) {
                        homeFragment.updateInsectCount(latestCount);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void replaceFragment(Fragment fragment, String tag) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        Fragment current = fm.findFragmentById(R.id.frame_layout);
        if (current != null && current != fragment) ft.hide(current);

        if (!fragment.isAdded()) ft.add(R.id.frame_layout, fragment, tag);
        else ft.show(fragment);

        ft.commitAllowingStateLoss();
    }

    private void connectToBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, 1);
            return;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(HC05_MAC);

        new Thread(() -> {
            try {
                socket = device.createRfcommSocketToServiceRecord(HC05_UUID);
                socket.connect();

                InputStream inputStream = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                runOnUiThread(() -> Toast.makeText(this, "Connected to HC-05", Toast.LENGTH_SHORT).show());

                while (readingData) {
                    String line = reader.readLine();
                    if (line != null && !line.isEmpty()) {
                        int insectCount;
                        try {
                            insectCount = Integer.parseInt(line.trim());
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                            continue;
                        }

                        // Insert detection into DB
                        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                .format(new Date());
                        Detection detection = new Detection(
                                deviceQr != null ? deviceQr : "UNKNOWN",
                                timestamp,
                                insectCount
                        );
                        new Thread(() -> {
                            db.detectionDao().insert(detection);
                            fetchLatestCount(); // Fetch updated total after insertion
                        }).start();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Connection failed: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        readingData = false;
        try {
            if (socket != null) socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
