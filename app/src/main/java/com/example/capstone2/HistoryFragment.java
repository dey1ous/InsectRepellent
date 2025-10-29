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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;

public class HistoryFragment extends Fragment {

    private LineChart historyGraph;
    private TextView weekCount, dayCount, deviceNameTextView;
    private androidx.appcompat.widget.AppCompatAutoCompleteTextView dropdownMenu;
    private AppDatabase db;
    // ⭐ RENAMED: Use IP to match the new database key (Device.ipAddress)
    private String currentDeviceIP;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        historyGraph = view.findViewById(R.id.historyGraph);
        weekCount = view.findViewById(R.id.weekCount);
        dayCount = view.findViewById(R.id.dayCount);
        deviceNameTextView = view.findViewById(R.id.deviceName);
        dropdownMenu = view.findViewById(R.id.dropdownMenu);

        db = AppDatabase.getInstance(requireContext());

        // ⭐ FIX HERE: Use the new getConnectedIP() method
        if (getActivity() instanceof MainActivity) {
            currentDeviceIP = ((MainActivity) getActivity()).getConnectedIP();
        }

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        dropdownMenu.setText(today, false);
        loadGraphData(today);

        dropdownMenu.setOnClickListener(v -> showDatePicker());
        // setupChartInteraction(); // Assuming this method is defined elsewhere

        return view;
    }

    private void showDatePicker() {
        // Implementation remains here...
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, y, m, d) -> {
                    String selectedDate = String.format(Locale.getDefault(), "%d-%02d-%02d", y, m + 1, d);
                    dropdownMenu.setText(selectedDate, false);
                    loadGraphData(selectedDate);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void setupChartInteraction() {
        // Implementation remains here...
    }

    // ⭐ Added onResume to refresh data when returning to the fragment
    @Override
    public void onResume() {
        super.onResume();
        // Reload data using the currently selected date in the dropdown
        loadGraphData(dropdownMenu.getText().toString());
    }

    /**
     * Loads, aggregates, and displays detection data specific to the connected device IP.
     */
    private void loadGraphData(String selectedDate) {
        // ⭐ CRITICAL CHECK: Check IP instead of MAC
        if (currentDeviceIP == null || currentDeviceIP.isEmpty() || currentDeviceIP.equals("192.168.4.1")) {
            Toast.makeText(getContext(), "No specific device connected. History unavailable.", Toast.LENGTH_LONG).show();
            clearChartAndCounts();
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -7);
            String sevenDaysAgoTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(cal.getTime());

            // ⭐ Queries now correctly pass the currentDeviceIP to the DAO
            int dailySum = db.detectionDao().getDailyTotalForDevice(selectedDate, currentDeviceIP);
            int weeklySum = db.detectionDao().getSumSinceForDevice(sevenDaysAgoTimestamp, currentDeviceIP);
            List<Detection> dailyDetections = db.detectionDao().getDetectionsForDayByDevice(selectedDate, currentDeviceIP);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    dayCount.setText(String.valueOf(dailySum));
                    weekCount.setText(String.valueOf(weeklySum));

                    // Update device name display (optional: fetch name from DeviceDao)
                    deviceNameTextView.setText("Device: " + currentDeviceIP);

                    List<Entry> entries = new ArrayList<>();
                    SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

                    // Convert Detections to Chart Entries
                    for (Detection d : dailyDetections) {
                        try {
                            Date detectionDate = timestampFormat.parse(d.getTimestamp());
                            if (detectionDate != null) {
                                // Use the timestamp as the X-axis value
                                entries.add(new Entry(detectionDate.getTime(), d.getInsectCount()));
                            }
                        } catch (Exception e) {
                            // Log.e(TAG, "Error parsing timestamp for chart: " + e.getMessage());
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

        // Configuration and styling of the chart data
        LineDataSet dataSet = new LineDataSet(entries, "Detections on " + selectedDate);
        // Assuming R.color.teal_700 and R.color.black exist in your resources
        dataSet.setColor(context.getColor(R.color.teal_700));
        dataSet.setValueTextColor(context.getColor(R.color.black));
        dataSet.setCircleColor(context.getColor(R.color.teal_700));
        dataSet.setLineWidth(2f);
        dataSet.setDrawValues(false); // Often cleaner for time-series data

        LineData lineData = new LineData(dataSet);
        historyGraph.setData(lineData);

        // Configure X-axis to display time
        XAxis xAxis = historyGraph.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat mFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            @Override
            public String getFormattedValue(float value) {
                return mFormat.format(new Date((long) value));
            }
        });
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelRotationAngle(-45);

        historyGraph.getDescription().setEnabled(false);
        historyGraph.animateX(500);
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