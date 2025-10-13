package com.example.capstone2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Size;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.capstone2.database.AppDatabase;
import com.example.capstone2.entities.Device;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class QrScannerActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 100;

    private PreviewView previewView;
    private BarcodeScanner scanner;
    private boolean isProcessing = false; // Prevent duplicate scans

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrscanner);

        previewView = findViewById(R.id.previewView);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        // Configure QR scanner for QR codes only
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        scanner = BarcodeScanning.getClient(options);

        // Request camera permission if needed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE
            );
        } else {
            startCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this,
                        "Camera permission is required to scan QR codes",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                analysis.setAnalyzer(ContextCompat.getMainExecutor(this), this::processImage);

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                );

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void processImage(ImageProxy imageProxy) {
        if (isProcessing) {
            imageProxy.close();
            return;
        }

        @SuppressWarnings("UnsafeOptInUsageError")
        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        isProcessing = true;

        scanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    if (!barcodes.isEmpty()) {
                        handleResult(barcodes);
                    } else {
                        imageProxy.close();
                        isProcessing = false;
                    }
                })
                .addOnFailureListener(e -> {
                    imageProxy.close();
                    isProcessing = false;
                });
    }

    private void handleResult(List<Barcode> barcodes) {
        if (barcodes.isEmpty()) {
            isProcessing = false;
            return;
        }

        String qrData = barcodes.get(0).getRawValue();
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());

        new Thread(() -> {
            Device existingDevice = db.deviceDao().getRegisteredDevice();

            runOnUiThread(() -> {
                if (isFinishing()) return;

                if (existingDevice != null) {
                    new androidx.appcompat.app.AlertDialog.Builder(QrScannerActivity.this)
                            .setCancelable(false)
                            .setTitle("Replace QR Code?")
                            .setMessage("A QR code is already saved:\n\n" + existingDevice.getQrCode() +
                                    "\n\nDo you want to replace it with:\n" + qrData + "?")
                            .setPositiveButton("Replace", (dialog, which) -> {
                                new Thread(() -> {
                                    db.deviceDao().deleteAll();
                                    db.deviceDao().insert(new Device("Scanned Device", qrData));
                                }).start();

                                Intent resultIntent = new Intent();
                                resultIntent.putExtra("QR_RESULT", qrData);
                                setResult(RESULT_OK, resultIntent);
                                finish();
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> {
                                dialog.dismiss();
                                isProcessing = false; // Allow rescanning again
                            })
                            .show();
                } else {
                    new Thread(() -> {
                        db.deviceDao().insert(new Device("Scanned Device", qrData));
                    }).start();

                    Toast.makeText(this, "QR Code saved: " + qrData, Toast.LENGTH_SHORT).show();

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("QR_RESULT", qrData);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }
            });
        }).start();
    }
}
