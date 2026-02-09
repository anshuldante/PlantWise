package com.leafiq.app.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.leafiq.app.data.entity.Analysis;
import com.leafiq.app.data.entity.CareCompletion;
import com.leafiq.app.data.entity.CareItem;
import com.leafiq.app.data.entity.CareSchedule;
import com.leafiq.app.data.entity.Plant;

@Database(entities = {Plant.class, Analysis.class, CareItem.class, CareSchedule.class, CareCompletion.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract PlantDao plantDao();
    public abstract AnalysisDao analysisDao();
    public abstract CareItemDao careItemDao();
    public abstract CareScheduleDao careScheduleDao();
    public abstract CareCompletionDao careCompletionDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "leafiq_database"
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
