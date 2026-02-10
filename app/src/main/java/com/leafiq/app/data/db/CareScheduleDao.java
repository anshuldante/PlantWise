package com.leafiq.app.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.leafiq.app.data.entity.CareSchedule;

import java.util.List;

@Dao
public interface CareScheduleDao {
    @Query("SELECT * FROM care_schedules WHERE plant_id = :plantId")
    LiveData<List<CareSchedule>> getSchedulesForPlant(String plantId);

    @Query("SELECT * FROM care_schedules WHERE plant_id = :plantId AND is_enabled = 1")
    LiveData<List<CareSchedule>> getEnabledSchedulesForPlant(String plantId);

    @Query("SELECT * FROM care_schedules WHERE is_enabled = 1")
    List<CareSchedule> getAllEnabledSchedules();

    @Query("SELECT * FROM care_schedules WHERE plant_id = :plantId")
    List<CareSchedule> getSchedulesByPlantIdSync(String plantId);

    @Query("SELECT * FROM care_schedules WHERE id = :id")
    CareSchedule getScheduleById(String id);

    @Query("SELECT * FROM care_schedules WHERE is_enabled = 1 AND next_due <= :beforeTimestamp")
    List<CareSchedule> getDueSchedules(long beforeTimestamp);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSchedule(CareSchedule schedule);

    @Update
    void updateSchedule(CareSchedule schedule);

    @Delete
    void deleteSchedule(CareSchedule schedule);

    @Query("DELETE FROM care_schedules WHERE plant_id = :plantId")
    void deleteSchedulesForPlant(String plantId);
}
