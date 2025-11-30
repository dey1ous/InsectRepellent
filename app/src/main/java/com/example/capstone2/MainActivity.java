package com.example.capstone2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.capstone2.database.AppDatabase;
import com.example.capstone2.databinding.ActivityMainBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private AppDatabase db;
    private volatile int latestCount = 0;

    // UI State Holders
    private String latestSystemStatus = "OFF";
    private boolean latestLiquidStatus = false;

    // Fragments
    private HomeFragment homeFragment;
    private WifiConnectFragment wifiFragment;
    private HistoryFragment historyFragment;
    private SettingsFragment settingsFragment;

    // Broadcast Receiver to get data from Service
    private final BroadcastReceiver mqttReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (InsectMonitorService.ACTION_MQTT_UPDATE.equals(intent.getAction())) {

                String status = intent.getStringExtra("status");
                boolean liquid = intent.getBooleanExtra("liquid_low", false);
                boolean detected = intent.getBooleanExtra("detected", false);

                // Update UI Variables
                latestSystemStatus = status;
                latestLiquidStatus = liquid;

                // If detected, we should re-fetch the total count from DB
                if (detected) {
                    fetchLatestCount();
                }

                // Update Home Fragment if it is currently visible
                if (homeFragment != null && homeFragment.isAdded()) {
                    homeFragment.updateSystemStatus(latestSystemStatus);
                    homeFragment.updateLiquidStatus(latestLiquidStatus);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);

        // Setup Fragments
        homeFragment = new HomeFragment();
        wifiFragment = new WifiConnectFragment();
        historyFragment = new HistoryFragment();
        settingsFragment = new SettingsFragment();

        replaceFragment(homeFragment, "HOME_FRAGMENT");
        setupBottomNavigation();
        setupFloatingButton();

        // 1. START THE BACKGROUND SERVICE
        startInsectService();

        // 2. Initial Data Fetch
        fetchLatestCount();
    }

    private void startInsectService() {
        Intent serviceIntent = new Intent(this, InsectMonitorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 1. Register Receiver (Existing code)
        IntentFilter filter = new IntentFilter(InsectMonitorService.ACTION_MQTT_UPDATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(mqttReceiver, filter);

        // ⭐ NEW CODE: Load the last known state from storage
        android.content.SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        latestSystemStatus = prefs.getString("LAST_STATUS", "OFF"); // Load saved status
        latestLiquidStatus = prefs.getBoolean("LAST_LIQUID", false); // Load saved liquid

        // 2. Refresh UI with these loaded values
        fetchLatestCount();

        // Force HomeFragment to update immediately
        if (homeFragment != null && homeFragment.isAdded()) {
            homeFragment.updateSystemStatus(latestSystemStatus);
            homeFragment.updateLiquidStatus(latestLiquidStatus);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister Receiver to save resources when app is minimized
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mqttReceiver);
    }

    // --- DATA & UTILS ---

    public void fetchLatestCount() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                Integer count = db.detectionDao().getTodayTotalCount("Deodeus");
                latestCount = (count != null) ? count : 0;
                runOnUiThread(() -> {
                    if (homeFragment != null && homeFragment.isAdded()) {
                        homeFragment.updateInsectCount(latestCount);
                    }
                });
            } catch (Exception e) {
                Log.e("MainActivity", "DB fetch error", e);
            }
        });
    }

    // ⭐ ADDED THESE METHODS SO HOME FRAGMENT CAN READ STATUS
    public String getLatestSystemStatus() {
        return latestSystemStatus;
    }

    public boolean getLatestLiquidStatus() {
        return latestLiquidStatus;
    }

    // --- NAVIGATION ---

    private void setupBottomNavigation() {
        binding.bottomNavigationView.setBackground(null);
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.home) replaceFragment(homeFragment, "HOME_FRAGMENT");
            else if (id == R.id.bluetooth) replaceFragment(wifiFragment, "WIFI_FRAGMENT");
            else if (id == R.id.history) replaceFragment(historyFragment, "HISTORY_FRAGMENT");
            else if (id == R.id.settings) replaceFragment(settingsFragment, "SETTINGS_FRAGMENT");
            return true;
        });
    }

    private void setupFloatingButton() {
        FloatingActionButton fab = findViewById(R.id.fab_refresh);
        fab.setOnClickListener(v -> {
            // Restart service ensures connection is fresh
            startInsectService();
            Toast.makeText(this, "Refreshed Connection", Toast.LENGTH_SHORT).show();
        });
    }

    private void replaceFragment(Fragment fragment, String tag) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.frame_layout, fragment, tag);
        ft.commitAllowingStateLoss();
    }
}