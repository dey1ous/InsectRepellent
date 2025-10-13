package com.example.capstone2;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.capstone2.database.AppDatabase;
import com.example.capstone2.entities.Detection;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryFragment extends Fragment {

    private LineChart historyGraph;
    private TextView weekCount, dayCount;
    private androidx.appcompat.widget.AppCompatAutoCompleteTextView dropdownMenu;
    private AppDatabase db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        historyGraph = view.findViewById(R.id.historyGraph);
        weekCount = view.findViewById(R.id.weekCount);
        dayCount = view.findViewById(R.id.dayCount);
        dropdownMenu = view.findViewById(R.id.dropdownMenu);

        db = AppDatabase.getInstance(requireContext());

        // Default load for today
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        dropdownMenu.setText(today, false);
        loadGraphData(today);

        // When dropdown is clicked, open the date picker
        dropdownMenu.setOnClickListener(v -> showDatePicker());

        // Enable zoom/pan for chart
        setupChartInteraction();

        return view;
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();

        // Force the dialog to use a style that includes OK/Cancel buttons
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                android.R.style.Theme_Holo_Light_Dialog_MinWidth, // ✅ ensures OK/Cancel visible
                (view, year, month, dayOfMonth) -> {
                    String selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    dropdownMenu.setText(selectedDate, false);
                    loadGraphData(selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Optional: only allow up to today
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

        // Make background transparent (optional aesthetic)
        if (datePickerDialog.getWindow() != null) {
            datePickerDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        datePickerDialog.show();
    }

    private void setupChartInteraction() {
        historyGraph.setDragEnabled(true);
        historyGraph.setScaleEnabled(true);
        historyGraph.setPinchZoom(true);
    }

    private void loadGraphData(String selectedDate) {
        new Thread(() -> {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

            long oneWeekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
            int weeklySum = 0;
            int dailySum = 0;

            List<Detection> allDetections = db.detectionDao().getAllDetections();
            List<Detection> dailyDetections = new ArrayList<>();

            for (Detection d : allDetections) {
                try {
                    Date detectionDate = timestampFormat.parse(d.getTimestamp());
                    if (detectionDate == null) continue;

                    long timeMillis = detectionDate.getTime();

                    if (selectedDate.equals(dateFormat.format(detectionDate))) {
                        dailySum += d.getInsectCount();
                        dailyDetections.add(d);
                    }

                    if (timeMillis >= oneWeekAgo) weeklySum += d.getInsectCount();

                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            int finalDailySum = dailySum;
            int finalWeeklySum = weeklySum;
            requireActivity().runOnUiThread(() -> {
                dayCount.setText(String.valueOf(finalDailySum));
                weekCount.setText(String.valueOf(finalWeeklySum));

                List<Entry> entries = new ArrayList<>();
                for (int i = 0; i < dailyDetections.size(); i++) {
                    entries.add(new Entry(i, dailyDetections.get(i).getInsectCount()));
                }

                LineDataSet dataSet = new LineDataSet(entries, "Insect Detections");
                dataSet.setColor(getResources().getColor(R.color.teal_700, null));
                dataSet.setValueTextColor(getResources().getColor(R.color.black, null));

                LineData lineData = new LineData(dataSet);
                historyGraph.setData(lineData);
                historyGraph.getDescription().setEnabled(false);
                historyGraph.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                historyGraph.invalidate();
            });
        }).start();
    }
}
