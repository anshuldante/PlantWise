package com.leafiq.app.data.model;

import androidx.room.ColumnInfo;

/**
 * POJO for JOIN query result across care_completions, care_schedules, and plants tables.
 * Used by Care Overview to display recent completion history.
 */
public class CareCompletionWithPlantInfo {
    public String id;

    @ColumnInfo(name = "schedule_id")
    public String scheduleId;

    @ColumnInfo(name = "completed_at")
    public long completedAt;

    public String source;

    @ColumnInfo(name = "care_type")
    public String careType;

    @ColumnInfo(name = "plant_id")
    public String plantId;

    public String nickname;

    @ColumnInfo(name = "common_name")
    public String commonName;
}
