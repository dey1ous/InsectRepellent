package com.example.capstone2.entities;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "devices")
public class Device {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String deviceName;
    private String macAddress; // ✅ use MAC address instead of QR

    // Constructor used by Room
    public Device(String macAddress) {
        this.deviceName = "HC-05"; // default name for your module
        this.macAddress = macAddress;
    }

    // Optional constructor (ignored by Room)
    @Ignore
    public Device(String deviceName, String macAddress) {
        this.deviceName = deviceName;
        this.macAddress = macAddress;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }
}
