package com.example.capstone2.daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.example.capstone2.entities.Detection;
import java.util.List;

@Dao
public interface DetectionDao {

    @Insert
    void insert(Detection detection);

    @Query("SELECT * FROM detections ORDER BY timestamp DESC")
    List<Detection> getAllDetections();

    // ⭐ UPDATED: Get detections by device IP
    @Query("SELECT * FROM detections WHERE deviceIp = :ip ORDER BY timestamp DESC")
    List<Detection> getDetectionsByDevice(String ip);

    // ⭐ UPDATED: Get today's total count by device IP
    @Query("SELECT SUM(insectCount) FROM detections " +
            "WHERE deviceIp = :ip AND date(timestamp) = date('now', 'localtime')")
    Integer getTodayTotalCount(String ip);

    // --- New, Efficient Queries for HistoryFragment ---

    /**
     * Efficiently gets the total insect count for a specific day AND device IP.
     */
    @Query("SELECT SUM(insectCount) FROM detections WHERE deviceIp = :ip AND timestamp LIKE :date || '%'")
    int getDailyTotalForDevice(String date, String ip);

    /**
     * Efficiently gets all detections for a specific day AND device IP.
     */
    @Query("SELECT * FROM detections WHERE deviceIp = :ip AND timestamp LIKE :date || '%' ORDER BY timestamp ASC")
    List<Detection> getDetectionsForDayByDevice(String date, String ip);

    /**
     * Efficiently gets the total insect count since a given timestamp for a specific device IP.
     */
    @Query("SELECT SUM(insectCount) FROM detections WHERE deviceIp = :ip AND timestamp >= :sinceTimestamp")
    int getSumSinceForDevice(String sinceTimestamp, String ip);
}