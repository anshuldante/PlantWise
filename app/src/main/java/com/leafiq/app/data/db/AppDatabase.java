package com.leafiq.app.data.db;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.leafiq.app.data.entity.Analysis;
import com.leafiq.app.data.entity.CareCompletion;
import com.leafiq.app.data.entity.CareItem;
import com.leafiq.app.data.entity.CareSchedule;
import com.leafiq.app.data.entity.Plant;

@Database(entities = {Plant.class, Analysis.class, CareItem.class, CareSchedule.class, CareCompletion.class}, version = 3, exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // No-op identity migration — establishes migration infrastructure
            Log.i("AppDatabase", "Migration 1->2: no-op migration completed successfully");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE plants ADD COLUMN medium_thumbnail_path TEXT");
            database.execSQL("ALTER TABLE plants ADD COLUMN high_res_thumbnail_path TEXT");
            Log.i("AppDatabase", "Migration 2->3: added medium_thumbnail_path and high_res_thumbnail_path columns");
        }
    };

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(new Callback() {
                        @Override
                        public void onOpen(@NonNull SupportSQLiteDatabase db) {
                            super.onOpen(db);
                            Log.i("AppDatabase", "Database opened at version " + db.getVersion());
                        }
                    })
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
