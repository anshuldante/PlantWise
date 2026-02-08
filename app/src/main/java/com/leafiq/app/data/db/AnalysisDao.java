package com.leafiq.app.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.leafiq.app.data.entity.Analysis;

import java.util.List;

@Dao
public interface AnalysisDao {
    @Query("SELECT * FROM analyses WHERE plant_id = :plantId ORDER BY created_at DESC")
    LiveData<List<Analysis>> getAnalysesForPlant(String plantId);

    @Query("SELECT * FROM analyses WHERE plant_id = :plantId ORDER BY created_at DESC LIMIT 5")
    List<Analysis> getRecentAnalysesSync(String plantId);

    @Query("SELECT * FROM analyses WHERE id = :id")
    Analysis getAnalysisById(String id);

    @Insert
    void insertAnalysis(Analysis analysis);

    @Update
    void updateAnalysis(Analysis analysis);

    @Query("DELETE FROM analyses WHERE id = :analysisId")
    void deleteAnalysisById(String analysisId);

    @Query("SELECT photo_path FROM analyses WHERE plant_id = :plantId AND photo_path IS NOT NULL")
    List<String> getPhotoPathsForPlantSync(String plantId);
}
