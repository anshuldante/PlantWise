package com.leafiq.app.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.leafiq.app.data.entity.Analysis;
import com.leafiq.app.data.model.AnalysisWithPlant;

import java.util.List;

@Dao
public interface AnalysisDao {
    @Query("SELECT * FROM analyses WHERE plant_id = :plantId ORDER BY created_at DESC")
    LiveData<List<Analysis>> getAnalysesForPlant(String plantId);

    @Query("SELECT * FROM analyses WHERE plant_id = :plantId ORDER BY created_at DESC LIMIT 5")
    List<Analysis> getRecentAnalysesSync(String plantId);

    @Query("SELECT * FROM analyses WHERE plant_id = :plantId ORDER BY created_at DESC LIMIT 1")
    Analysis getLatestForPlantSync(String plantId);

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

    @Query("SELECT analyses.*, " +
            "plants.common_name AS plant_common_name, " +
            "plants.thumbnail_path AS plant_thumbnail_path, " +
            "plants.nickname AS plant_nickname, " +
            "plants.scientific_name AS plant_scientific_name, " +
            "plants.latest_health_score AS plant_latest_health_score " +
            "FROM analyses " +
            "INNER JOIN plants ON analyses.plant_id = plants.id " +
            "ORDER BY analyses.created_at DESC")
    LiveData<List<AnalysisWithPlant>> getAllAnalysesWithPlant();

    @Query("SELECT analyses.*, " +
            "plants.common_name AS plant_common_name, " +
            "plants.thumbnail_path AS plant_thumbnail_path, " +
            "plants.nickname AS plant_nickname, " +
            "plants.scientific_name AS plant_scientific_name, " +
            "plants.latest_health_score AS plant_latest_health_score " +
            "FROM analyses " +
            "INNER JOIN plants ON analyses.plant_id = plants.id " +
            "WHERE analyses.plant_id = :plantId " +
            "ORDER BY analyses.created_at DESC")
    LiveData<List<AnalysisWithPlant>> getAnalysesWithPlantForPlant(String plantId);

    @Query("SELECT * FROM analyses WHERE parse_status = 'OK' ORDER BY created_at DESC LIMIT :limit")
    List<Analysis> getAnalysesNeedingScan(int limit);

    @Query("UPDATE analyses SET parse_status = :status WHERE id = :id")
    void updateParseStatus(String id, String status);

    @Query("UPDATE analyses SET re_analyzed_at = :timestamp, parse_status = 'OK' WHERE id = :id")
    void markReAnalyzed(String id, long timestamp);

    @Query("SELECT COUNT(*) FROM analyses WHERE plant_id = :plantId")
    LiveData<Integer> getAnalysisCountForPlant(String plantId);
}
