package com.leafiq.app.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.leafiq.app.data.entity.CareItem;

import java.util.List;

@Dao
public interface CareItemDao {
    @Query("SELECT * FROM care_items WHERE plant_id = :plantId")
    LiveData<List<CareItem>> getCareItemsForPlant(String plantId);

    @Query("SELECT * FROM care_items WHERE next_due < :timestamp ORDER BY next_due ASC")
    LiveData<List<CareItem>> getOverdueItems(long timestamp);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCareItem(CareItem item);
}
