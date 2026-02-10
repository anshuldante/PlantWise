package com.leafiq.app.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "care_completions",
        foreignKeys = @ForeignKey(
            entity = CareSchedule.class,
            parentColumns = "id",
            childColumns = "schedule_id",
            onDelete = ForeignKey.CASCADE),
        indices = {@Index("schedule_id")})
public class CareCompletion {
    @PrimaryKey
    @NonNull
    public String id;

    @ColumnInfo(name = "schedule_id")
    public String scheduleId;

    @ColumnInfo(name = "completed_at")
    public long completedAt;    // Unix timestamp ms

    public String source;       // "notification_action", "in_app", "snooze"

    public CareCompletion() {
        this.id = "";
    }
}
