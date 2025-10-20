package com.example.capstone2.daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.example.capstone2.entities.Detection;
import java.util.List;

@Dao
public interface DetectionDao {

    // --- Existing Methods (Kept for other parts of the app) ---
    @Insert
    void insert(Detection detection);

    @Query("SELECT * FROM detections ORDER BY timestamp DESC")
    List<Detection> getAllDetections();

    @Query("SELECT * FROM detections WHERE deviceMac = :mac ORDER BY timestamp DESC")
    List<Detection> getDetectionsByDevice(String mac);

    @Query("SELECT SUM(insectCount) FROM detections " +
            "WHERE deviceMac = :mac AND date(timestamp) = date('now', 'localtime')")
    Integer getTodayTotalCount(String mac);

    // --- ⭐ New, Efficient Queries for HistoryFragment ---

    /**
     * Efficiently gets the total insect count for a specific day AND device.
     */
    @Query("SELECT SUM(insectCount) FROM detections WHERE deviceMac = :mac AND timestamp LIKE :date || '%'")
    int getDailyTotalForDevice(String date, String mac);

    /**
     * Efficiently gets all detections for a specific day AND device.
     */
    @Query("SELECT * FROM detections WHERE deviceMac = :mac AND timestamp LIKE :date || '%' ORDER BY timestamp ASC")
    List<Detection> getDetectionsForDayByDevice(String date, String mac);

    /**
     * Efficiently gets the total insect count since a given timestamp for a specific device.
     * Used to calculate the "last 7 days" total.
     */
    @Query("SELECT SUM(insectCount) FROM detections WHERE deviceMac = :mac AND timestamp >= :sinceTimestamp")
    int getSumSinceForDevice(String sinceTimestamp, String mac);
}