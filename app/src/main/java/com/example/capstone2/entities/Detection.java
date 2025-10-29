package com.example.capstone2.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "detections")
public class Detection {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String deviceIp; // ⭐ CHANGED: Foreign key is now the device's IP
    private String timestamp;
    private int insectCount;

    public Detection(String deviceIp, String timestamp, int insectCount) { // ⭐ CONSTRUCTOR UPDATED
        this.deviceIp = deviceIp;
        this.timestamp = timestamp;
        this.insectCount = insectCount;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDeviceIp() { return deviceIp; } // ⭐ CHANGED
    public void setDeviceIp(String deviceIp) { this.deviceIp = deviceIp; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public int getInsectCount() { return insectCount; }
    public void setInsectCount(int insectCount) { this.insectCount = insectCount; }
}