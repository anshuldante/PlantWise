package com.leafiq.app.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

import com.leafiq.app.data.entity.Analysis;

/**
 * POJO for JOIN query result combining Analysis with Plant metadata.
 * Used by timeline, history, and growth tracking screens to display analysis data
 * with plant context (name, thumbnail, nickname).
 * <p>
 * This is NOT an @Entity - it's a query result class for JOIN operations.
 */
public class AnalysisWithPlant {

    /**
     * Analysis entity fields (no prefix needed - column names don't collide with plant fields).
     */
    @Embedded
    public Analysis analysis;

    /**
     * Plant common name from JOIN.
     */
    @ColumnInfo(name = "plant_common_name")
    public String plantCommonName;

    /**
     * Plant thumbnail path from JOIN.
     */
    @ColumnInfo(name = "plant_thumbnail_path")
    public String plantThumbnailPath;

    /**
     * Plant nickname from JOIN (user-assigned).
     */
    @ColumnInfo(name = "plant_nickname")
    public String plantNickname;

    /**
     * Plant scientific name from JOIN.
     */
    @ColumnInfo(name = "plant_scientific_name")
    public String plantScientificName;

    /**
     * Plant latest health score from JOIN.
     */
    @ColumnInfo(name = "plant_latest_health_score")
    public int plantLatestHealthScore;

    public AnalysisWithPlant() {
        this.analysis = new Analysis();
    }
}
