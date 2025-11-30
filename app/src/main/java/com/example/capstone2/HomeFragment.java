package com.example.capstone2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.capstone2.database.AppDatabase;

public class HomeFragment extends Fragment {

    private TextView mosquitoCount, systemStatus, solutionStatus;
    private AppDatabase db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        mosquitoCount = view.findViewById(R.id.mosquitoCount);
        systemStatus = view.findViewById(R.id.systemStatus);
        solutionStatus = view.findViewById(R.id.solutionStatus);

        systemStatus.setText("System Status: OFF");
        solutionStatus.setText("Solution Status: ---");

        db = AppDatabase.getInstance(requireContext());
        return view;
    }

    public void updateInsectCount(int count) {
        if (getActivity() != null && mosquitoCount != null) {
            getActivity().runOnUiThread(() -> mosquitoCount.setText(String.valueOf(count)));
        }
    }

    /**
     * UPDATED: Accepts String because ESP32 sends "status": "OFF"/"Idle"/"Repel Active"
     */
    public void updateSystemStatus(String statusText) {
        if (getActivity() != null && systemStatus != null) {
            getActivity().runOnUiThread(() -> {
                systemStatus.setText("System Status: " + statusText);

                if ("OFF".equalsIgnoreCase(statusText)) {
                    systemStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                } else {
                    // "Idle", "Repel Active", "Startup" -> Green
                    systemStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                }
            });
        }
    }

    /**
     * UPDATED: Aligned with ESP32 JSON "liquid_low": boolean
     *
     * @param isLiquidLow true = LOW (Red), false = FULL (Green)
     */
    public void updateLiquidStatus(boolean isLiquidLow) {
        if (getActivity() != null && solutionStatus != null) {
            getActivity().runOnUiThread(() -> {
                // If liquid_low is TRUE, it means the tank is empty/low -> RED Warning
                if (isLiquidLow) {
                    solutionStatus.setText("Solution Status: LOW");
                    solutionStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                }
                // If liquid_low is FALSE, it means the tank is OK -> GREEN
                else {
                    solutionStatus.setText("Solution Status: FULL");
                    solutionStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();

            activity.fetchLatestCount();

            // FIX: This now receives a String (e.g., "Idle")
            updateSystemStatus(activity.getLatestSystemStatus());

            // FIX: This now receives a boolean (true/false)
            updateLiquidStatus(activity.getLatestLiquidStatus());
        }
    }
}