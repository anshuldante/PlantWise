package com.leafiq.app.data.db;

import static com.google.common.truth.Truth.assertThat;

import android.database.Cursor;

import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented tests for Room database migrations.
 *
 * Tests verify that migrations preserve data across all database tables
 * and that fresh installs work correctly at the latest version.
 */
@RunWith(AndroidJUnit4.class)
public class MigrationTest {

    private static final String TEST_DB = "migration-test.db";

    @Rule
    public MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase.class.getName(),
            new FrameworkSQLiteOpenHelperFactory());

    @Test
    public void migrate1To2_preservesPlantData() throws Exception {
        // Create database at version 1
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 1);

        // Insert a plant with all columns
        db.execSQL("INSERT INTO plants (id, common_name, scientific_name, nickname, location, " +
                "thumbnail_path, latest_health_score, created_at, updated_at) VALUES " +
                "('plant-1', 'Monstera', 'Monstera deliciosa', 'My Monstera', 'Living Room', " +
                "'/storage/thumb1.jpg', 8, 1234567890000, 1234567890000)");

        db.close();

        // Run migration to version 2
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, AppDatabase.MIGRATION_1_2);

        // Query plant data and verify all columns preserved
        Cursor cursor = db.query("SELECT * FROM plants WHERE id = 'plant-1'");
        assertThat(cursor.moveToFirst()).isTrue();

        assertThat(cursor.getString(cursor.getColumnIndexOrThrow("id"))).isEqualTo("plant-1");
        assertThat(cursor.getString(cursor.getColumnIndexOrThrow("common_name"))).isEqualTo("Monstera");
        assertThat(cursor.getString(cursor.getColumnIndexOrThrow("scientific_name"))).isEqualTo("Monstera deliciosa");
        assertThat(cursor.getString(cursor.getColumnIndexOrThrow("nickname"))).isEqualTo("My Monstera");
        assertThat(cursor.getString(cursor.getColumnIndexOrThrow("location"))).isEqualTo("Living Room");
        assertThat(cursor.getString(cursor.getColumnIndexOrThrow("thumbnail_path"))).isEqualTo("/storage/thumb1.jpg");
        assertThat(cursor.getInt(cursor.getColumnIndexOrThrow("latest_health_score"))).isEqualTo(8);
        assertThat(cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))).isEqualTo(1234567890000L);
        assertThat(cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"))).isEqualTo(1234567890000L);

        cursor.close();
        db.close();
    }

    @Test
    public void migrate1To2_preservesAnalysisData() throws Exception {
        // Create database at version 1
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 1);

        // Insert a plant first (for foreign key if applicable)
        db.execSQL("INSERT INTO plants (id, common_name, scientific_name, nickname, location, " +
                "thumbnail_path, latest_health_score, created_at, updated_at) VALUES " +
                "('plant-1', 'Test Plant', 'Scientific', NULL, NULL, NULL, 7, 1000000000000, 1000000000000)");

        // Insert an analysis
        db.execSQL("INSERT INTO analyses (id, plant_id, photo_path, raw_response, health_score, " +
                "summary, created_at) VALUES " +
                "('analysis-1', 'plant-1', '/storage/photo1.jpg', '{\"data\":\"test\"}', 9, " +
                "'Healthy plant', 1234567890000)");

        db.close();

        // Run migration to version 2
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, AppDatabase.MIGRATION_1_2);

        // Query analysis data and verify all columns preserved
        Cursor cursor = db.query("SELECT * FROM analyses WHERE id = 'analysis-1'");
        assertThat(cursor.moveToFirst()).isTrue();

        assertThat(cursor.getString(cursor.getColumnIndexOrThrow("id"))).isEqualTo("analysis-1");
        assertThat(cursor.getString(cursor.getColumnIndexOrThrow("plant_id"))).isEqualTo("plant-1");
        assertThat(cursor.getString(cursor.getColumnIndexOrThrow("photo_path"))).isEqualTo("/storage/photo1.jpg");
        assertThat(cursor.getString(cursor.getColumnIndexOrThrow("raw_response"))).isEqualTo("{\"data\":\"test\"}");
        assertThat(cursor.getInt(cursor.getColumnIndexOrThrow("health_score"))).isEqualTo(9);
        assertThat(cursor.getString(cursor.getColumnIndexOrThrow("summary"))).isEqualTo("Healthy plant");
        assertThat(cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))).isEqualTo(1234567890000L);

        cursor.close();
        db.close();
    }

    @Test
    public void migrate1To2_preservesAllFiveTables() throws Exception {
        // Create database at version 1
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 1);

        // Insert one row in each of the 5 tables

        // 1. Plant
        db.execSQL("INSERT INTO plants (id, common_name, scientific_name, nickname, location, " +
                "thumbnail_path, latest_health_score, created_at, updated_at) VALUES " +
                "('p1', 'Plant', 'Scientific', NULL, NULL, NULL, 7, 1000000000000, 1000000000000)");

        // 2. Analysis
        db.execSQL("INSERT INTO analyses (id, plant_id, photo_path, raw_response, health_score, " +
                "summary, created_at) VALUES " +
                "('a1', 'p1', '/photo.jpg', '{}', 8, 'Summary', 1000000000000)");

        // 3. CareItem
        db.execSQL("INSERT INTO care_items (id, plant_id, type, frequency_days, last_done, " +
                "next_due, notes) VALUES " +
                "('ci1', 'p1', 'water', 7, 1000000000000, 1000604800000, 'Water weekly')");

        // 4. CareSchedule
        db.execSQL("INSERT INTO care_schedules (id, plant_id, care_type, frequency_days, " +
                "next_due, is_custom, is_enabled, snooze_count, notes) VALUES " +
                "('cs1', 'p1', 'water', 7, 1000604800000, 0, 1, 0, '200ml')");

        // 5. CareCompletion
        db.execSQL("INSERT INTO care_completions (id, schedule_id, completed_at, source) VALUES " +
                "('cc1', 'cs1', 1000000000000, 'in_app')");

        db.close();

        // Run migration to version 2
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, AppDatabase.MIGRATION_1_2);

        // Verify each table has exactly 1 row
        assertThat(queryCount(db, "plants")).isEqualTo(1);
        assertThat(queryCount(db, "analyses")).isEqualTo(1);
        assertThat(queryCount(db, "care_items")).isEqualTo(1);
        assertThat(queryCount(db, "care_schedules")).isEqualTo(1);
        assertThat(queryCount(db, "care_completions")).isEqualTo(1);

        db.close();
    }

    @Test
    public void migrate1To2_emptyDatabase() throws Exception {
        // Create empty database at version 1
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 1);
        db.close();

        // Run migration (should succeed with no data)
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, AppDatabase.MIGRATION_1_2);

        // Verify all tables exist and are empty
        assertThat(queryCount(db, "plants")).isEqualTo(0);
        assertThat(queryCount(db, "analyses")).isEqualTo(0);
        assertThat(queryCount(db, "care_items")).isEqualTo(0);
        assertThat(queryCount(db, "care_schedules")).isEqualTo(0);
        assertThat(queryCount(db, "care_completions")).isEqualTo(0);

        db.close();
    }

    @Test
    public void freshInstallAtVersion2_works() throws Exception {
        // Create database directly at version 2
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 2);

        // Insert a plant row
        db.execSQL("INSERT INTO plants (id, common_name, scientific_name, nickname, location, " +
                "thumbnail_path, latest_health_score, created_at, updated_at) VALUES " +
                "('fresh-plant', 'Pothos', 'Epipremnum aureum', 'Golden Pothos', 'Kitchen', " +
                "'/storage/pothos.jpg', 9, 1234567890000, 1234567890000)");

        // Query it back
        Cursor cursor = db.query("SELECT * FROM plants WHERE id = 'fresh-plant'");
        assertThat(cursor.moveToFirst()).isTrue();

        assertThat(cursor.getString(cursor.getColumnIndexOrThrow("common_name"))).isEqualTo("Pothos");
        assertThat(cursor.getString(cursor.getColumnIndexOrThrow("scientific_name"))).isEqualTo("Epipremnum aureum");
        assertThat(cursor.getString(cursor.getColumnIndexOrThrow("nickname"))).isEqualTo("Golden Pothos");
        assertThat(cursor.getInt(cursor.getColumnIndexOrThrow("latest_health_score"))).isEqualTo(9);

        cursor.close();
        db.close();
    }

    /**
     * Helper method to count rows in a table.
     */
    private int queryCount(SupportSQLiteDatabase db, String tableName) {
        Cursor cursor = db.query("SELECT COUNT(*) FROM " + tableName);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count;
    }
}
