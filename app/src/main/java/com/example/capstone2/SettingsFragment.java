package com.example.capstone2;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsFragment extends Fragment {

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

        switchNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(requireContext(), "Notifications Enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Notifications Disabled", Toast.LENGTH_SHORT).show();
            }
        });

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
