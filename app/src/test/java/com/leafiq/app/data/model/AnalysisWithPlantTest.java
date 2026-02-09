package com.leafiq.app.data.model;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class AnalysisWithPlantTest {

    @Test
    public void constructor_initializesAnalysis() {
        AnalysisWithPlant awp = new AnalysisWithPlant();
        assertThat(awp.analysis).isNotNull();
    }

    @Test
    public void constructor_analysisHasEmptyId() {
        AnalysisWithPlant awp = new AnalysisWithPlant();
        assertThat(awp.analysis.id).isEqualTo("");
    }

    @Test
    public void constructor_plantFieldsAreNull() {
        AnalysisWithPlant awp = new AnalysisWithPlant();
        assertThat(awp.plantCommonName).isNull();
        assertThat(awp.plantThumbnailPath).isNull();
        assertThat(awp.plantNickname).isNull();
        assertThat(awp.plantScientificName).isNull();
    }

    @Test
    public void constructor_healthScoreDefaultsToZero() {
        AnalysisWithPlant awp = new AnalysisWithPlant();
        assertThat(awp.plantLatestHealthScore).isEqualTo(0);
    }

    @Test
    public void fieldAssignment_worksCorrectly() {
        AnalysisWithPlant awp = new AnalysisWithPlant();
        awp.plantCommonName = "Monstera";
        awp.plantThumbnailPath = "/photos/thumb.jpg";
        awp.plantNickname = "Monty";
        awp.plantScientificName = "Monstera deliciosa";
        awp.plantLatestHealthScore = 8;

        assertThat(awp.plantCommonName).isEqualTo("Monstera");
        assertThat(awp.plantThumbnailPath).isEqualTo("/photos/thumb.jpg");
        assertThat(awp.plantNickname).isEqualTo("Monty");
        assertThat(awp.plantScientificName).isEqualTo("Monstera deliciosa");
        assertThat(awp.plantLatestHealthScore).isEqualTo(8);
    }

    @Test
    public void analysisFields_canBeSet() {
        AnalysisWithPlant awp = new AnalysisWithPlant();
        awp.analysis.id = "analysis-1";
        awp.analysis.plantId = "plant-1";
        awp.analysis.healthScore = 7;
        awp.analysis.summary = "Healthy plant";
        awp.analysis.createdAt = 1000L;

        assertThat(awp.analysis.id).isEqualTo("analysis-1");
        assertThat(awp.analysis.plantId).isEqualTo("plant-1");
        assertThat(awp.analysis.healthScore).isEqualTo(7);
        assertThat(awp.analysis.summary).isEqualTo("Healthy plant");
        assertThat(awp.analysis.createdAt).isEqualTo(1000L);
    }
}
