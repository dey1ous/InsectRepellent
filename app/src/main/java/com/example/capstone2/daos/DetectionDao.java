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

    @Query("SELECT * FROM detections WHERE deviceQr = :qr ORDER BY timestamp DESC")
    List<Detection> getDetectionsByDevice(String qr);
}
