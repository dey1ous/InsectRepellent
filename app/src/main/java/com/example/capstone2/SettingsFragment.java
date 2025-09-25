package com.example.capstone2;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsFragment extends Fragment {

    private ActivityResultLauncher<Intent> qrScannerLauncher;

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
        LinearLayout rowScanQr = view.findViewById(R.id.rowScanQr);

        qrScannerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        String qrData = result.getData().getStringExtra("QR_RESULT");
                        if (qrData != null) {
                            Toast.makeText(requireContext(), "Scanned QR: " + qrData, Toast.LENGTH_SHORT).show();
                            // TODO: Handle the scanned QR for changing device or settings
                        }
                    }
                }
        );


        switchNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String message = isChecked ? "Notifications Enabled" : "Notifications Disabled";
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        });


        rowLeaveApp.setOnClickListener(v -> {
            AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Exit App")
                    .setMessage("Are you sure you want to leave the app?")
                    .setPositiveButton("Yes", (d, which) -> requireActivity().finishAffinity())
                    .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                    .create(); // create the AlertDialog instance

            dialog.setOnShowListener(dlg -> {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                        .setTextColor(requireContext().getColor(R.color.black));
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
                        .setTextColor(requireContext().getColor(R.color.black));
            });

            dialog.show();
        });


        rowScanQr.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), QrScannerActivity.class);
            qrScannerLauncher.launch(intent);
        });
    }
}
