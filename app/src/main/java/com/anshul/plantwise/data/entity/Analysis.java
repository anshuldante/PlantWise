package com.anshul.plantwise.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "analyses",
        foreignKeys = @ForeignKey(
            entity = Plant.class,
            parentColumns = "id",
            childColumns = "plant_id",
            onDelete = ForeignKey.CASCADE))
public class Analysis {
    @PrimaryKey
    @NonNull
    public String id;

    @ColumnInfo(name = "plant_id", index = true)
    public String plantId;

    @ColumnInfo(name = "photo_path")
    public String photoPath;

    @ColumnInfo(name = "raw_response")
    public String rawResponse;

    @ColumnInfo(name = "health_score")
    public int healthScore;

    public String summary;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    public Analysis() {
        this.id = "";
    }
}
