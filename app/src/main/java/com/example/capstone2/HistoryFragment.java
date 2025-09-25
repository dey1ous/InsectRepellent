package com.example.capstone2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
import com.google.android.material.textfield.TextInputLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

        // Initialize views
        historyGraph = view.findViewById(R.id.historyGraph);
        weekCount = view.findViewById(R.id.weekCount);
        dayCount = view.findViewById(R.id.dayCount);
        dropdownMenu = view.findViewById(R.id.dropdownMenu);

        db = AppDatabase.getInstance(requireContext());

        loadDatesDropdown();

        return view;
    }

    private void loadDatesDropdown() {
        new Thread(() -> {
            List<Detection> allDetections = db.detectionDao().getAllDetections();
            List<String> dates = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            for (Detection d : allDetections) {
                try {
                    Date detectionDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .parse(d.getTimestamp());
                    String dateStr = sdf.format(detectionDate);
                    if (!dates.contains(dateStr)) dates.add(dateStr);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            requireActivity().runOnUiThread(() -> {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, dates);
                dropdownMenu.setAdapter(adapter);

                dropdownMenu.setOnItemClickListener((parent, view, position, id) -> {
                    String selectedDate = adapter.getItem(position);
                    if (selectedDate != null) loadGraphData(selectedDate);
                });
            });
        }).start();
    }

    private void loadGraphData(String selectedDate) {
        new Thread(() -> {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            long startOfDay = 0;
            long endOfDay = 0;

            try {
                Date date = dateFormat.parse(selectedDate);
                if (date != null) {
                    startOfDay = date.getTime();
                    endOfDay = startOfDay + 24L * 60 * 60 * 1000 - 1;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }

            List<Detection> allDetections = db.detectionDao().getAllDetections();
            List<Detection> dailyDetections = new ArrayList<>();
            long oneWeekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
            int weeklyCounter = 0;

            for (Detection d : allDetections) {
                try {
                    Date detectionDate = timestampFormat.parse(d.getTimestamp());
                    if (detectionDate == null) continue;

                    long timeMillis = detectionDate.getTime();

                    if (timeMillis >= startOfDay && timeMillis <= endOfDay) dailyDetections.add(d);
                    if (timeMillis >= oneWeekAgo) weeklyCounter++;

                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            final int finalWeeklyCount = weeklyCounter;
            final int dailyCountValue = dailyDetections.size();

            // Prepare chart entries
            List<Entry> entries = new ArrayList<>();
            for (int i = 0; i < dailyDetections.size(); i++) {
                entries.add(new Entry(i, dailyDetections.get(i).getInsectCount()));
            }

            LineDataSet dataSet = new LineDataSet(entries, "Insect Detections");
            dataSet.setColor(getResources().getColor(R.color.teal_700, null));
            dataSet.setValueTextColor(getResources().getColor(R.color.black, null));
            LineData lineData = new LineData(dataSet);

            requireActivity().runOnUiThread(() -> {
                dayCount.setText(String.valueOf(dailyCountValue));
                weekCount.setText(String.valueOf(finalWeeklyCount));

                historyGraph.setData(lineData);
                historyGraph.getDescription().setEnabled(false);
                historyGraph.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                historyGraph.invalidate();
            });
        }).start();
    }
}
