package com.example.capstone2.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.capstone2.daos.DeviceDao;
import com.example.capstone2.daos.DetectionDao;
import com.example.capstone2.entities.Device;
import com.example.capstone2.entities.Detection;

@Database(entities = {Device.class, Detection.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    // Singleton instance (only one database instance throughout the app)
    private static volatile AppDatabase INSTANCE;

    // Abstract DAOs
    public abstract DeviceDao deviceDao();
    public abstract DetectionDao detectionDao();

    // Get database instance
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "capstone_db"
                            )
                            // Use this only for development (it deletes data on schema change)
                            .fallbackToDestructiveMigration()
                            // Optional: allow database access on main thread (for quick tests only)
                            // .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
