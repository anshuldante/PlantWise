package com.leafiq.app.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.leafiq.app.data.entity.Plant;

import java.util.List;

@Dao
public interface PlantDao {
    @Query("SELECT * FROM plants ORDER BY updated_at DESC")
    LiveData<List<Plant>> getAllPlants();

    @Query("SELECT * FROM plants WHERE id = :id")
    LiveData<Plant> getPlantById(String id);

    @Query("SELECT * FROM plants WHERE id = :id")
    Plant getPlantByIdSync(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPlant(Plant plant);

    @Update
    void updatePlant(Plant plant);

    @Delete
    void deletePlant(Plant plant);
}
