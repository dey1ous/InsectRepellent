package com.example.capstone2;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.capstone2.database.AppDatabase;
import com.example.capstone2.databinding.ActivityMainBinding;
import com.example.capstone2.entities.Detection;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

// ⭐ NEW IMPORTS FOR NETWORKING
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ESP32_WIFI_SERVER";
    private static final int DATA_POLL_INTERVAL_SECONDS = 3;

    private ActivityMainBinding binding;
    private AppDatabase db;

    // ⭐ WIFI VARIABLES REPLACING BLUETOOTH
    private String connectedIP = "192.168.4.1"; // Default IP
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    // 'isPolling' is private, but accessed via the public isPolling() method below.
    private volatile boolean isPolling = false;

    private HomeFragment homeFragment;
    private WifiConnectFragment wifiFragment;
    private HistoryFragment historyFragment;
    private SettingsFragment settingsFragment;

    private int latestCount = 0;

    // ---------------------- FRAGMENT ACCESS METHODS (FIXES ERRORS) ---------------------- //

    /**
     * FIX 1: Allows fragments to set the IP address for the HTTP requests.
     * Called by WifiConnectFragment when the user hits 'Connect'.
     * @param ip The new IP address entered by the user.
     */
    public void setConnectedIP(String ip) {
        this.connectedIP = ip;
    }

    /**
     * Allows fragments to read the connection status.
     * @return true if the scheduled data fetching is currently active.
     */
    public boolean isPolling() {
        return isPolling;
    }

    // Existing getter:
    public String getConnectedIP() {
        return connectedIP;
    }

    // ---------------------- LIFECYCLE & SETUP ---------------------- //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);

        homeFragment = new HomeFragment();
        wifiFragment = new WifiConnectFragment();
        historyFragment = new HistoryFragment();
        settingsFragment = new SettingsFragment();

        replaceFragment(homeFragment, "HOME_FRAGMENT");
        setupBottomNavigation();
        setupFloatingButton();

        // Start polling automatically when the app starts
        startPollingData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPollingData();
    }

    // ---------------------- WIFI / HTTP COMMUNICATION ---------------------- //

    /**
     * Sends a command (e.g., "ON", "OFF") to the ESP32 server asynchronously.
     * @param command The API route (e.g., "ON")
     */
    public void sendCommand(final String command) {
        scheduler.execute(() -> {
            String urlString = "http://" + connectedIP + "/" + command;
            Log.d(TAG, "Sending command: " + urlString);
            try {
                makeHttpRequest(urlString, false);
            } catch (Exception e) {
                Log.e(TAG, "Failed to send command " + command, e);
                runOnUiThread(() -> showToast("Command failed: " + e.getMessage()));
            }
        });
    }

    /**
     * Starts a scheduled task to continuously request data from the ESP32.
     */
    public void startPollingData() {
        // Only start if it's not already running
        if (isPolling) return;
        isPolling = true;

        // Schedules a task to run every DATA_POLL_INTERVAL_SECONDS
        scheduler.scheduleAtFixedRate(this::fetchLatestData, 0, DATA_POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
        runOnUiThread(() -> showToast("Started polling data from " + connectedIP));
    }

    /**
     * Stops the scheduled data fetching task.
     */
    public void stopPollingData() {
        if (!isPolling) return;
        scheduler.shutdownNow();
        isPolling = false;
        runOnUiThread(() -> showToast("Stopped polling."));
    }

    /**
     * Fetches the JSON data string from the ESP32's /data endpoint.
     */
    private void fetchLatestData() {
        String urlString = "http://" + connectedIP + "/data";
        String jsonResponse = null;

        try {
            jsonResponse = makeHttpRequest(urlString, true);
            if (jsonResponse != null) {
                handleIncomingJSON(jsonResponse);
            }
        } catch (Exception e) {
            // Note: This error is common if the ESP32 is offline or the IP is wrong
            Log.e(TAG, "Data fetch error from " + urlString + ": " + e.getMessage());
        }
    }

    /**
     * Core method to execute the HTTP GET request.
     * @param urlString The full URL to request.
     * @param readResponse Whether to read the response body (true for /data, false for commands)
     * @return The response body string or null on failure.
     */
    private String makeHttpRequest(String urlString, boolean readResponse) throws Exception {
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setConnectTimeout(2000);
            urlConnection.setReadTimeout(2000);

            int responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                if (readResponse) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    reader.close();
                    return result.toString();
                } else {
                    return "Command Sent";
                }
            } else {
                Log.w(TAG, "Server responded with code: " + responseCode + " at " + urlString);
                return null;
            }
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    // ---------------------- DATA PARSING & UI UPDATE ---------------------- //

    /**
     * Parses the JSON string received from the ESP32 and updates the UI/Database.
     * Expected JSON structure: {"detection": 1, "status": "ON", "capacity": 85}
     */
    private void handleIncomingJSON(String json) {
        try {
            final JSONObject jsonObject = new JSONObject(json);

            // 1. Get and process sensor data
            final int currentDetection = jsonObject.getInt("detection");
            final String deviceStatus = jsonObject.getString("status");
            final int liquidCapacity = jsonObject.getInt("capacity");

            // ⭐ LOGIC for Database (Only save detection if one occurred)
            if (currentDetection == 1) {
                // IMPORTANT: This logic assumes 'currentDetection' is 1 only when a *new* insect is detected.
                latestCount = latestCount + 1;
                final String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                final Detection detection = new Detection(connectedIP, timestamp, latestCount);

                // Save to database on background thread
                AppDatabase.databaseWriteExecutor.execute(() -> db.detectionDao().insert(detection));
            }

            // 2. Update UI on the main thread
            if (homeFragment != null && homeFragment.isAdded()) {
                runOnUiThread(() -> {
                    homeFragment.updateInsectCount(latestCount);
                    homeFragment.updateSystemStatus(deviceStatus.equals("ON") ? 1 : 0);
                    homeFragment.updateLiquidStatus(liquidCapacity);
                });
            }

        } catch (Exception e) {
            Log.e(TAG, "JSON/Parse error: " + json + " → " + e.getMessage());
        }
    }

    // ---------------------- UTILITIES & NAVIGATION ---------------------- //

    private void setupBottomNavigation() {
        binding.bottomNavigationView.setBackground(null);
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.home) {
                replaceFragment(homeFragment, "HOME_FRAGMENT");
            } else if (id == R.id.bluetooth) {
                replaceFragment(wifiFragment, "WIFI_FRAGMENT");
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
        fab.setOnClickListener(v -> fetchLatestData());
    }

    private void replaceFragment(Fragment fragment, String tag) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.frame_layout, fragment, tag);
        ft.commitAllowingStateLoss();
    }

    private void showToast(String msg) {
        runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
    }

    public void fetchLatestCount() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                Integer count = db.detectionDao().getTodayTotalCount(connectedIP);
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