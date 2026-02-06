package com.anshul.plantwise.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "care_items",
        foreignKeys = @ForeignKey(
            entity = Plant.class,
            parentColumns = "id",
            childColumns = "plant_id",
            onDelete = ForeignKey.CASCADE))
public class CareItem {
    @PrimaryKey
    @NonNull
    public String id;

    @ColumnInfo(name = "plant_id", index = true)
    public String plantId;

    public String type;     // "water", "fertilize", "prune", "repot"

    @ColumnInfo(name = "frequency_days")
    public int frequencyDays;

    @ColumnInfo(name = "last_done")
    public long lastDone;

    @ColumnInfo(name = "next_due")
    public long nextDue;

    public String notes;

    public CareItem() {
        this.id = "";
    }
}
