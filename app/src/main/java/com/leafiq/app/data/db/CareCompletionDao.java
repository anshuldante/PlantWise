package com.leafiq.app.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.leafiq.app.data.entity.CareCompletion;
import com.leafiq.app.data.model.CareCompletionWithPlantInfo;

import java.util.List;

@Dao
public interface CareCompletionDao {
    @Query("SELECT * FROM care_completions WHERE schedule_id = :scheduleId ORDER BY completed_at DESC")
    LiveData<List<CareCompletion>> getCompletionsForSchedule(String scheduleId);

    @Query("SELECT cc.* FROM care_completions cc " +
           "INNER JOIN care_schedules cs ON cc.schedule_id = cs.id " +
           "WHERE cs.plant_id = :plantId " +
           "ORDER BY cc.completed_at DESC LIMIT :limit")
    LiveData<List<CareCompletion>> getRecentCompletionsForPlant(String plantId, int limit);

    @Query("SELECT * FROM care_completions WHERE schedule_id = :scheduleId ORDER BY completed_at DESC LIMIT 1")
    CareCompletion getLastCompletionForSchedule(String scheduleId);

    @Insert
    void insertCompletion(CareCompletion completion);

    @Query("DELETE FROM care_completions WHERE schedule_id = :scheduleId")
    void deleteCompletionsForSchedule(String scheduleId);

    @Query("SELECT cc.id, cc.schedule_id, cc.completed_at, cc.source, " +
           "cs.care_type, " +
           "p.id AS plant_id, p.nickname, p.common_name " +
           "FROM care_completions cc " +
           "INNER JOIN care_schedules cs ON cc.schedule_id = cs.id " +
           "INNER JOIN plants p ON cs.plant_id = p.id " +
           "WHERE cc.completed_at >= :afterTimestamp " +
           "AND cc.source != 'snooze' " +
           "ORDER BY cc.completed_at DESC " +
           "LIMIT :maxEntries")
    List<CareCompletionWithPlantInfo> getRecentCompletions(long afterTimestamp, int maxEntries);
}
