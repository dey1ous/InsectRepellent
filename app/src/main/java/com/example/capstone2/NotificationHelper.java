package com.example.capstone2;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences; // Import added
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationHelper {

    private static final String CHANNEL_ID = "capstone_channel";

    // Make sure this matches the name used in your SettingsFragment
    private static final String PREFS_NAME = "AppPrefs";
    private static final String PREF_NOTIFY_KEY = "NOTIFY_ON";

    public static void showNotification(Context context, String title, String message) {

        // 1. CHECK USER PREFERENCE FIRST
        // We read the settings database. If "NOTIFY_ON" is false, we stop immediately.
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isNotificationEnabled = prefs.getBoolean(PREF_NOTIFY_KEY, true); // Default is true

        if (!isNotificationEnabled) {
            return; // Exit method, do not show notification
        }

        // 2. Create the Channel (Required for Android 8+)
        createNotificationChannel(context);

        // 3. Build the Notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // 4. Show it (with safety checks)
        try {
            NotificationManagerCompat manager = NotificationManagerCompat.from(context);

            // basic check if system notifications are enabled for the app
            if (manager.areNotificationsEnabled()) {
                // Use a unique ID (System.currentTimeMillis) so notifications stack
                // Or use a fixed number (e.g., 999) if you want to replace the old one
                manager.notify((int) System.currentTimeMillis(), builder.build());
            }
        } catch (SecurityException e) {
            // This happens on Android 13+ if the user denied the runtime permission
            e.printStackTrace();
        }
    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Capstone Alerts";
            String description = "Notification channel for insect detections";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}