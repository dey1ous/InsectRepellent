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

    // Filter by MAC instead of QR
    @Query("SELECT * FROM detections WHERE deviceMac = :mac ORDER BY timestamp DESC")
    List<Detection> getDetectionsByDevice(String mac);

    @Query("SELECT SUM(insectCount) FROM detections " +
            "WHERE deviceMac = :mac AND date(timestamp) = date('now', 'localtime')")
    Integer getTodayTotalCount(String mac);

    @Query("SELECT * FROM detections WHERE date(timestamp) = date('now', 'localtime') ORDER BY timestamp ASC")
    List<Detection> getTodayDetections();
}
