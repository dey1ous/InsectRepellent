package com.example.capstone2.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "devices")
public class Device {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String deviceName;
    private String ipAddress; // ⭐ CHANGED: Now stores the ESP32's IP address

    /**
     * Constructor for a new device, identified by its IP.
     */
    public Device(String deviceName, String ipAddress) {
        this.deviceName = deviceName;
        this.ipAddress = ipAddress;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}