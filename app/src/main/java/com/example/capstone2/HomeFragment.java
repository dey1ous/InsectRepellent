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

    public void updateSystemStatus(int code) {
        if (getActivity() != null && systemStatus != null) {
            getActivity().runOnUiThread(() -> {
                if (code == 0) {
                    systemStatus.setText("System Status: OFF");
                    systemStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                } else if (code == 1) {
                    systemStatus.setText("System Status: ON");
                    systemStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                }
            });
        }
    }

    public void updateLiquidStatus(int code) {
        if (getActivity() != null && solutionStatus != null) {
            getActivity().runOnUiThread(() -> {
                if (code == 3) {
                    solutionStatus.setText("Solution Status: LOW");
                    solutionStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                } else if (code == 4) {
                    solutionStatus.setText("Solution Status: MEDIUM");
                    solutionStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                } else if (code == 5) {
                    solutionStatus.setText("Solution Status: FULL");
                    solutionStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                } else {
                    solutionStatus.setText("Solution Status: ---");
                    solutionStatus.setTextColor(getResources().getColor(android.R.color.black));
                }
            });
        }
    }
}
