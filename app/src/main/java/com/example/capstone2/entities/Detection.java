package com.example.capstone2.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "detections")
public class Detection {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String deviceQr; // which device detected
    private String timestamp; // when detection occurred
    private int insectCount; // how many insects detected

    public Detection(String deviceQr, String timestamp, int insectCount) {
        this.deviceQr = deviceQr;
        this.timestamp = timestamp;
        this.insectCount = insectCount;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDeviceQr() { return deviceQr; }
    public void setDeviceQr(String deviceQr) { this.deviceQr = deviceQr; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public int getInsectCount() { return insectCount; }
    public void setInsectCount(int insectCount) { this.insectCount = insectCount; }
}
