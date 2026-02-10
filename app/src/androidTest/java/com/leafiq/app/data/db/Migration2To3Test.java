package com.leafiq.app.data.db;

import static com.google.common.truth.Truth.assertThat;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented tests for database migration 2->3.
 *
 * Migration 2->3 adds two nullable thumbnail columns to the plants table:
 * - medium_thumbnail_path (300px for grid display)
 * - high_res_thumbnail_path (800px for detail page)
 *
 * Tests verify that the migration preserves existing data and allows writes
 * to the new columns.
 */
@RunWith(AndroidJUnit4.class)
public class Migration2To3Test {

    private static final String TEST_DB = "migration-2-3-test.db";

    @Rule
    public MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase.class.getName(),
            new FrameworkSQLiteOpenHelperFactory());

    @Test
    public void migrate2To3_addsNewColumns() throws Exception {
        // Create version 2 database
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 2);

        // Insert a plant with version 2 schema
        ContentValues values = new ContentValues();
        values.put("id", "test-plant-1");
        values.put("common_name", "Monstera");
        values.put("scientific_name", "Monstera deliciosa");
        values.put("thumbnail_path", "/path/to/thumb.jpg");
        values.put("latest_health_score", 8);
        values.put("created_at", System.currentTimeMillis());
        values.put("updated_at", System.currentTimeMillis());
        db.insert("plants", SQLiteDatabase.CONFLICT_REPLACE, values);
        db.close();

        // Run migration
        db = helper.runMigrationsAndValidate(TEST_DB, 3, true, AppDatabase.MIGRATION_2_3);

        // Verify data preserved
        Cursor cursor = db.query("SELECT * FROM plants WHERE id = 'test-plant-1'");
        assertThat(cursor.moveToFirst()).isTrue();

        int commonNameIdx = cursor.getColumnIndex("common_name");
        assertThat(cursor.getString(commonNameIdx)).isEqualTo("Monstera");

        int thumbIdx = cursor.getColumnIndex("thumbnail_path");
        assertThat(cursor.getString(thumbIdx)).isEqualTo("/path/to/thumb.jpg");

        // Verify new columns exist and are null
        int mediumIdx = cursor.getColumnIndex("medium_thumbnail_path");
        assertThat(mediumIdx).isNotEqualTo(-1);
        assertThat(cursor.isNull(mediumIdx)).isTrue();

        int highResIdx = cursor.getColumnIndex("high_res_thumbnail_path");
        assertThat(highResIdx).isNotEqualTo(-1);
        assertThat(cursor.isNull(highResIdx)).isTrue();

        cursor.close();
        db.close();
    }

    @Test
    public void migrate2To3_newColumnsAcceptValues() throws Exception {
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 2);

        ContentValues values = new ContentValues();
        values.put("id", "test-plant-2");
        values.put("common_name", "Fern");
        values.put("latest_health_score", 6);
        values.put("created_at", System.currentTimeMillis());
        values.put("updated_at", System.currentTimeMillis());
        db.insert("plants", SQLiteDatabase.CONFLICT_REPLACE, values);
        db.close();

        db = helper.runMigrationsAndValidate(TEST_DB, 3, true, AppDatabase.MIGRATION_2_3);

        // Update with new column values
        ContentValues update = new ContentValues();
        update.put("medium_thumbnail_path", "/path/to/medium.jpg");
        update.put("high_res_thumbnail_path", "/path/to/high.jpg");
        db.update("plants", SQLiteDatabase.CONFLICT_REPLACE, update, "id = ?", new String[]{"test-plant-2"});

        Cursor cursor = db.query("SELECT medium_thumbnail_path, high_res_thumbnail_path FROM plants WHERE id = 'test-plant-2'");
        assertThat(cursor.moveToFirst()).isTrue();
        assertThat(cursor.getString(0)).isEqualTo("/path/to/medium.jpg");
        assertThat(cursor.getString(1)).isEqualTo("/path/to/high.jpg");

        cursor.close();
        db.close();
    }
}
