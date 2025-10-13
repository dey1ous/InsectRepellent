package com.example.capstone2.entities;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "devices")
public class Device {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String deviceName;
    private String qrCode;

    // Room will use this constructor
    public Device(String qrCode) {
        this.deviceName = "Unknown"; // default name
        this.qrCode = qrCode;
    }

    // Ignore this constructor for Room
    @Ignore
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
