package com.example.capstone2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.capstone2.database.AppDatabase;
import com.example.capstone2.entities.Detection;

// MQTT Imports
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class InsectMonitorService extends Service implements MqttCallback {

    private static final String TAG = "InsectService";
    private static final String CHANNEL_ID = "ForegroundServiceChannel";

    // Broadcast Action String (Used to talk to Activity)
    public static final String ACTION_MQTT_UPDATE = "com.example.capstone2.MQTT_UPDATE";

    // MQTT Configuration
    private static final String MQTT_SERVER = "tcp://io.adafruit.com:1883";
    private static final String IO_USERNAME = "Deodeus";
    private static final String IO_KEY = BuildConfig.AIO_KEY;
    private static final String TOPIC_STATUS = IO_USERNAME + "/feeds/ioi.status";

    private MqttClient mqttClient;
    private MqttConnectOptions mqttOptions;
    private AppDatabase db;

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Database
        db = AppDatabase.getInstance(getApplicationContext());

        // Initialize MQTT
        initializeMqttClient();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 1. Make this a Foreground Service immediately
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Insect Monitor Active")
                .setContentText("Listening for detections...")
                .setSmallIcon(R.drawable.ic_notifications)
                .setOngoing(true)
                .build();
        startForeground(1, notification);

        // 2. Connect to MQTT if not already connected
        connectToMqttBroker();

        return START_STICKY;
    }

    // --- MQTT LOGIC ---

    private void initializeMqttClient() {
        try {
            String clientId = MqttClient.generateClientId();
            mqttClient = new MqttClient(MQTT_SERVER, clientId, new MemoryPersistence());
            mqttClient.setCallback(this);

            mqttOptions = new MqttConnectOptions();
            mqttOptions.setCleanSession(true);
            mqttOptions.setUserName(IO_USERNAME);
            mqttOptions.setPassword(IO_KEY.toCharArray());
            // Add KeepAlive to ensure connection stays open
            mqttOptions.setKeepAliveInterval(60);

        } catch (MqttException e) {
            Log.e(TAG, "MQTT Init Failed", e);
        }
    }

    private void connectToMqttBroker() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                if (mqttClient != null && !mqttClient.isConnected()) {
                    mqttClient.connect(mqttOptions);
                    mqttClient.subscribe(TOPIC_STATUS, 0);
                    Log.d(TAG, "MQTT Connected in Service");
                }
            } catch (MqttException e) {
                Log.e(TAG, "MQTT Connect Failed", e);
            }
        });
    }

    // --- MQTT CALLBACKS ---

    @Override
    public void connectionLost(Throwable cause) {
        Log.w(TAG, "Connection lost. Reconnecting...");
        connectToMqttBroker();
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String payload = new String(message.getPayload());
        Log.d(TAG, "Service received: " + payload);

        handleIncomingJSON(payload);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) { }

    // --- DATA HANDLING ---

    private void handleIncomingJSON(String json) {
        try {
            // 1. Parse Incoming Data
            JSONObject jsonObject = new JSONObject(json);

            boolean isDetectionOccurring = jsonObject.optBoolean("detected", false);
            boolean isLiquidLow = jsonObject.optBoolean("liquid_low", false);
            String statusText = jsonObject.optString("status", "OFF");

            // 2. DATABASE & NOTIFICATION (Only if insect detected)
            if (isDetectionOccurring) {
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                // Insert into Database
                Detection detection = new Detection(IO_USERNAME, timestamp, 1);
                db.detectionDao().insert(detection);

                // Trigger Notification
                checkAndSendNotification();
            }

            // 3. ⭐ SAVE STATE (Critical Fix)
            // We save the status immediately so MainActivity can read it later (even after app restart)
            getSharedPreferences("AppPrefs", MODE_PRIVATE)
                    .edit()
                    .putString("LAST_STATUS", statusText)
                    .putBoolean("LAST_LIQUID", isLiquidLow)
                    .apply();

            // 4. BROADCAST TO UI (Update screen if app is currently open)
            sendDataToActivity(isDetectionOccurring, statusText, isLiquidLow);

        } catch (Exception e) {
            Log.e(TAG, "Parse Error: " + e.getMessage());
        }
    }

    private void checkAndSendNotification() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isNotifyEnabled = prefs.getBoolean("NOTIFY_ON", true); // Default true

        if (isNotifyEnabled) {
            NotificationHelper.showNotification(
                    this,
                    "Insect Detected!",
                    "A detection event has just been recorded."
            );
        }
    }

    private void sendDataToActivity(boolean detected, String status, boolean liquidLow) {
        Intent intent = new Intent(ACTION_MQTT_UPDATE);
        intent.putExtra("detected", detected);
        intent.putExtra("status", status);
        intent.putExtra("liquid_low", liquidLow);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // --- BOILERPLATE ---

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (mqttClient != null) mqttClient.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Insect Monitor Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}