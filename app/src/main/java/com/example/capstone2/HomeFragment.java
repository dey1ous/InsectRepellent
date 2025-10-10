package com.example.capstone2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.capstone2.database.AppDatabase;
import com.example.capstone2.entities.Detection;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView mosquitoCount, systemStatus, solutionStatus;
    private AppDatabase db;
    private String deviceQr;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        mosquitoCount = view.findViewById(R.id.mosquitoCount);
        systemStatus = view.findViewById(R.id.systemStatus);
        solutionStatus = view.findViewById(R.id.solutionStatus);

        systemStatus.setText("System Status: Active");
        solutionStatus.setText("Solution Status: Low");

        db = AppDatabase.getInstance(requireContext());

        // Optional: set deviceQr from arguments if passed
        if (getArguments() != null) {
            deviceQr = getArguments().getString("DEVICE_QR");
        }

        // Load today’s insect count
        loadTodayInsectCount();

        return view;
    }

    public void loadTodayInsectCount() {
        new Thread(() -> {
            try {
                int totalToday = db.detectionDao().getTodayTotalCount(deviceQr != null ? deviceQr : "UNKNOWN");

                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> updateInsectCount(totalToday));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void updateInsectCount(int count) {
        if (getActivity() != null && mosquitoCount != null && solutionStatus != null) {
            getActivity().runOnUiThread(() -> {
                mosquitoCount.setText(String.valueOf(count));

                if (count > 800) {
                    solutionStatus.setText("Solution Status: High");
                    solutionStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                } else if (count > 500) {
                    solutionStatus.setText("Solution Status: Medium");
                    solutionStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                } else {
                    solutionStatus.setText("Solution Status: Low");
                    solutionStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                }
            });
        }
    }
}
