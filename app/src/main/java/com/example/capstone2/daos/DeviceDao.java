package com.example.capstone2.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.capstone2.entities.Device;

import java.util.List;

@Dao
public interface DeviceDao {

    // Insert a new device
    @Insert
    void insert(Device device);

    // Get the first (and only) registered device
    @Query("SELECT * FROM devices LIMIT 1")
    Device getRegisteredDevice();

    // Find a device by its QR code
    @Query("SELECT * FROM devices WHERE qrCode = :qr LIMIT 1")
    Device findByQr(String qr);

    // Delete all devices
    @Query("DELETE FROM devices")
    void deleteAll();

    // Optional: Delete a specific device
    @Delete
    void delete(Device device);
}
