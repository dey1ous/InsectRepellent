package com.example.capstone2.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.capstone2.daos.DeviceDao;
import com.example.capstone2.daos.DetectionDao;
import com.example.capstone2.entities.Device;
import com.example.capstone2.entities.Detection;

@Database(entities = {Device.class, Detection.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract DeviceDao deviceDao();
    public abstract DetectionDao detectionDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "capstone_db"
            ).fallbackToDestructiveMigration().build();
        }
        return instance;
    }
}
