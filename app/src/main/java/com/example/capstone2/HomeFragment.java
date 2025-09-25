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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

        db = AppDatabase.getInstance(requireContext());

        loadTodayInsectCount();
        updateSystemStatus();
        updateSolutionStatus();

        return view;
    }

    private void loadTodayInsectCount() {
        new Thread(() -> {
            List<Detection> allDetections = db.detectionDao().getAllDetections();
            int todayCount = 0;

            SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String todayStr = sdfDate.format(new Date());

            SimpleDateFormat sdfTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

            for (Detection d : allDetections) {
                try {
                    Date detectionDate = sdfTimestamp.parse(d.getTimestamp());
                    if (detectionDate != null && sdfDate.format(detectionDate).equals(todayStr)) {
                        todayCount += d.getInsectCount();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            final int finalCount = todayCount;
            requireActivity().runOnUiThread(() -> mosquitoCount.setText(String.valueOf(finalCount)));
        }).start();
    }

    private void updateSystemStatus() {
        // Replace this with your actual logic for system status
        boolean systemActive = true; // Example: fetch from DB or sensor
        systemStatus.setText("System Status: " + (systemActive ? "Active" : "Inactive"));
        systemStatus.setTextColor(systemActive ?
                getResources().getColor(android.R.color.holo_green_dark) :
                getResources().getColor(android.R.color.holo_red_dark));
    }

    private void updateSolutionStatus() {
        // Replace this with your actual solution status logic
        int solutionLevel = 3; // Example: 3 = High, 2 = Medium, 1 = Low

        String statusText;
        int color;

        switch (solutionLevel) {
            case 3:
                statusText = "High";
                color = getResources().getColor(android.R.color.holo_green_dark);
                break;
            case 2:
                statusText = "Medium";
                color = getResources().getColor(android.R.color.holo_orange_dark);
                break;
            default:
                statusText = "Low";
                color = getResources().getColor(android.R.color.holo_red_dark);
        }

        solutionStatus.setText("Solution Status: " + statusText);
        solutionStatus.setTextColor(color);
    }
}
