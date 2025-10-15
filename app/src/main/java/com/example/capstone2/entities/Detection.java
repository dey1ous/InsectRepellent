package com.example.capstone2.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "detections")
public class Detection {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String deviceMac; // ✅ replaced deviceQr → deviceMac
    private String timestamp;
    private int insectCount;

    public Detection(String deviceMac, String timestamp, int insectCount) {
        this.deviceMac = deviceMac;
        this.timestamp = timestamp;
        this.insectCount = insectCount;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDeviceMac() { return deviceMac; }
    public void setDeviceMac(String deviceMac) { this.deviceMac = deviceMac; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public int getInsectCount() { return insectCount; }
    public void setInsectCount(int insectCount) { this.insectCount = insectCount; }
}
