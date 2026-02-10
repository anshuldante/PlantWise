package com.leafiq.app.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "care_schedules",
        foreignKeys = @ForeignKey(
            entity = Plant.class,
            parentColumns = "id",
            childColumns = "plant_id",
            onDelete = ForeignKey.CASCADE),
        indices = {@Index("plant_id")})
public class CareSchedule {
    @PrimaryKey
    @NonNull
    public String id;

    @ColumnInfo(name = "plant_id")
    public String plantId;

    @ColumnInfo(name = "care_type")
    public String careType;     // "water", "fertilize", "repot" (NOT pruning per user decision)

    @ColumnInfo(name = "frequency_days")
    public int frequencyDays;

    @ColumnInfo(name = "next_due")
    public long nextDue;        // Unix timestamp ms

    @ColumnInfo(name = "is_custom")
    public boolean isCustom;    // false = AI-derived, true = user-customized

    @ColumnInfo(name = "is_enabled")
    public boolean isEnabled;   // per-plant toggle, default true

    @ColumnInfo(name = "snooze_count")
    public int snoozeCount;     // consecutive snoozes, reset on completion

    public String notes;        // AI-derived notes like amount/type

    public CareSchedule() {
        this.id = "";
    }
}
