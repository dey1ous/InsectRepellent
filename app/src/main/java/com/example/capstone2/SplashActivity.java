package com.example.capstone2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.capstone2.database.AppDatabase;
import com.example.capstone2.entities.Device;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final boolean TEST_MODE = false; // true to skip QR scan
    private ActivityResultLauncher<Intent> qrScannerLauncher;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        // QR scanner result handler
        qrScannerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String qrData = result.getData().getStringExtra("QR_RESULT");
                        if (qrData != null) {
                            Toast.makeText(this, "Device QR Scanned: " + qrData, Toast.LENGTH_SHORT).show();

                            // Save device in background thread
                            new Thread(() -> {
                                AppDatabase db = AppDatabase.getInstance(this);
                                db.deviceDao().deleteAll(); // ensure only one device
                                db.deviceDao().insert(new Device("Registered Device", qrData));

                                // Start MainActivity after DB write
                                runOnUiThread(this::startMainActivity);
                            }).start();
                        }
                    } else {
                        Toast.makeText(this, "QR scan canceled", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
        );

        // Splash delay
        progressBar.postDelayed(this::decideNextStep, 2000); // 2 seconds
    }

    private void decideNextStep() {
        if (TEST_MODE) {
            // Test mode skips QR scanning
            startMainActivity();
            return;
        }

        // Access DB in background thread
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            Device registeredDevice = db.deviceDao().getRegisteredDevice();

            runOnUiThread(() -> {
                if (registeredDevice != null) {
                    // Device already registered
                    startMainActivity();
                } else {
                    // Launch mandatory QR scanner
                    qrScannerLauncher.launch(new Intent(SplashActivity.this, QrScannerActivity.class));
                }
            });
        }).start();
    }

    private void startMainActivity() {
        progressBar.setVisibility(View.GONE);
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        finish();
    }
}
