package com.example.capstone2.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.capstone2.daos.DeviceDao;
import com.example.capstone2.daos.DetectionDao;
import com.example.capstone2.entities.Device;
import com.example.capstone2.entities.Detection;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Device.class, Detection.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    // ⭐ RECOMMENDATION: Create a fixed-size thread pool for all database operations.
    // This is more performant than creating a new thread for every query.
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public abstract DeviceDao deviceDao();
    public abstract DetectionDao detectionDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "capstone_db"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}