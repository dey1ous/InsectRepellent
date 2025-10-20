package com.example.capstone2.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "devices")
public class Device {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String deviceName;
    private String macAddress;

    /**
     * ⭐ A single, clean constructor for both Room and your code to use.
     * When you create a new device in your BluetoothFragment, you already provide both the name and the MAC address, so this works perfectly.
     */
    public Device(String deviceName, String macAddress) {
        this.deviceName = deviceName;
        this.macAddress = macAddress;
    }

    // Getters and setters (unchanged)
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }
}