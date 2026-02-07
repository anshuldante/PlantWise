package com.leafiq.app.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "plants")
public class Plant {
    @PrimaryKey
    @NonNull
    public String id;

    @ColumnInfo(name = "common_name")
    public String commonName;

    @ColumnInfo(name = "scientific_name")
    public String scientificName;

    public String nickname;
    public String location;

    @ColumnInfo(name = "thumbnail_path")
    public String thumbnailPath;

    @ColumnInfo(name = "latest_health_score")
    public int latestHealthScore;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    public Plant() {
        this.id = "";
    }
}
