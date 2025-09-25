package com.example.capstone2.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "devices")
public class Device {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String deviceName;
    private String qrCode;

    public Device(String deviceName, String qrCode) {
        this.deviceName = deviceName;
        this.qrCode = qrCode;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
}
