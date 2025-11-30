package com.example.capstone2;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences; // Import SharedPreferences
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsFragment extends Fragment {

    // MUST match the names used in NotificationHelper and InsectMonitorService
    private static final String PREFS_NAME = "AppPrefs";
    private static final String PREF_NOTIFY_KEY = "NOTIFY_ON";

    private androidx.activity.result.ActivityResultLauncher<String> requestNotificationPermissionLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialSwitch switchNotification = view.findViewById(R.id.switchNotification);
        LinearLayout rowLeaveApp = view.findViewById(R.id.rowLeaveApp);

        // 1. SETUP SHARED PREFERENCES
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // 2. READ SAVED STATE (Set the switch to the correct position when opening the screen)
        boolean isEnabled = prefs.getBoolean(PREF_NOTIFY_KEY, true); // Default is true
        switchNotification.setChecked(isEnabled);

        // Permission request launcher
        requestNotificationPermissionLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Toast.makeText(requireContext(), "Notification permission granted", Toast.LENGTH_SHORT).show();
                    } else {
                        // If denied, turn the switch back off visually and in storage
                        switchNotification.setChecked(false);
                        prefs.edit().putBoolean(PREF_NOTIFY_KEY, false).apply();
                        Toast.makeText(requireContext(), "Permission denied. Notifications disabled.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // 🔔 Notification switch logic
        switchNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {

            // 3. SAVE THE SETTING IMMEDIATELY
            prefs.edit().putBoolean(PREF_NOTIFY_KEY, isChecked).apply();

            if (isChecked) {
                // Check Android 13+ Permissions
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                        requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                        return;
                    }
                }

                // Show a test notification to confirm it works
                // (NotificationHelper will check the preference we just saved and see it's true)
                NotificationHelper.showNotification(requireContext(),
                        "Notifications Enabled",
                        "You will now receive alerts.");

                Toast.makeText(requireContext(), "Notifications Enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Notifications Disabled", Toast.LENGTH_SHORT).show();
            }
        });

        // 🚪 Leave App confirmation
        rowLeaveApp.setOnClickListener(v -> {
            AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Exit App")
                    .setMessage("Are you sure you want to leave the app?")
                    .setPositiveButton("Yes", (d, which) -> requireActivity().finishAffinity())
                    .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                    .create();

            dialog.setOnShowListener(dlg -> {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                        .setTextColor(requireContext().getColor(R.color.black));
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
                        .setTextColor(requireContext().getColor(R.color.black));
            });

            dialog.show();
        });
    }
}