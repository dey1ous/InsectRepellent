package com.example.capstone2;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.capstone2.database.AppDatabase;
import com.example.capstone2.entities.Detection;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryFragment extends Fragment {

    private LineChart historyGraph;
    private TextView weekCount, dayCount, deviceNameTextView;
    private androidx.appcompat.widget.AppCompatAutoCompleteTextView dropdownMenu;
    private AppDatabase db;
    private String currentDeviceMac;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        historyGraph = view.findViewById(R.id.historyGraph);
        weekCount = view.findViewById(R.id.weekCount);
        dayCount = view.findViewById(R.id.dayCount);
        deviceNameTextView = view.findViewById(R.id.deviceName); // Assuming you have a TextView with this ID
        dropdownMenu = view.findViewById(R.id.dropdownMenu);

        db = AppDatabase.getInstance(requireContext());

        // ⭐ Get the currently connected device's MAC from MainActivity
        if (getActivity() instanceof MainActivity) {
            currentDeviceMac = ((MainActivity) getActivity()).getConnectedMac();
        }

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        dropdownMenu.setText(today, false);
        loadGraphData(today);

        dropdownMenu.setOnClickListener(v -> showDatePicker());
        setupChartInteraction();

        return view;
    }

    private void showDatePicker() {
        // ... (this method can remain the same as your previous version)
    }

    private void setupChartInteraction() {
        // ... (this method can remain the same as your previous version)
    }

    /**
     * ⭐ REWRITTEN to be device-specific and highly efficient.
     */
    private void loadGraphData(String selectedDate) {
        if (currentDeviceMac == null || currentDeviceMac.isEmpty()) {
            Toast.makeText(getContext(), "No device connected. History is unavailable.", Toast.LENGTH_LONG).show();
            clearChartAndCounts();
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -7);
            String sevenDaysAgoTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(cal.getTime());

            // ⭐ Execute the new, device-specific queries from the DAO
            int dailySum = db.detectionDao().getDailyTotalForDevice(selectedDate, currentDeviceMac);
            int weeklySum = db.detectionDao().getSumSinceForDevice(sevenDaysAgoTimestamp, currentDeviceMac);
            List<Detection> dailyDetections = db.detectionDao().getDetectionsForDayByDevice(selectedDate, currentDeviceMac);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    dayCount.setText(String.valueOf(dailySum));
                    weekCount.setText(String.valueOf(weeklySum));

                    List<Entry> entries = new ArrayList<>();
                    SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    for (Detection d : dailyDetections) {
                        try {
                            Date detectionDate = timestampFormat.parse(d.getTimestamp());
                            if (detectionDate != null) {
                                entries.add(new Entry(detectionDate.getTime(), d.getInsectCount()));
                            }
                        } catch (Exception e) {
                            // Handle parsing error
                        }
                    }

                    updateChart(entries, selectedDate);
                });
            }
        });
    }

    private void updateChart(List<Entry> entries, String selectedDate) {
        Context context = getContext();
        if (context == null) return;

        if (entries.isEmpty()) {
            historyGraph.clear();
            historyGraph.setNoDataText("No detections on " + selectedDate);
            historyGraph.invalidate();
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Detections on " + selectedDate);
        dataSet.setColor(context.getColor(R.color.teal_700));
        dataSet.setValueTextColor(context.getColor(R.color.black));
        dataSet.setCircleColor(context.getColor(R.color.teal_700));
        dataSet.setLineWidth(2f);

        LineData lineData = new LineData(dataSet);
        historyGraph.setData(lineData);
        historyGraph.invalidate();
    }

    private void clearChartAndCounts() {
        dayCount.setText("0");
        weekCount.setText("0");
        historyGraph.clear();
        historyGraph.setNoDataText("Please connect to a device to see history.");
        historyGraph.invalidate();
    }
}