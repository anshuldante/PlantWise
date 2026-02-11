package com.leafiq.app.ui.analysis;

import com.leafiq.app.data.entity.Analysis;
import com.leafiq.app.data.entity.Plant;
import com.leafiq.app.data.model.PlantAnalysisResult;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for AnalysisStateMapper.
 * Tests pure mapping functions with no Android dependencies.
 */
public class AnalysisStateMapperTest {

    // ==================== mapForDisplay tests ====================

    @Test
    public void mapForDisplay_statusOk_returnsSuccess() {
        // Arrange
        Analysis analysis = createAnalysis("OK");
        Plant plant = createPlant("Rose", "Rosa");
        PlantAnalysisResult parsedResult = createFullResult();

        // Act
        AnalysisUiState state = AnalysisStateMapper.mapForDisplay(
            analysis, plant, parsedResult, "OK", false, false
        );

        // Assert
        assertTrue(state.isSuccess());
        assertNotNull(state.getResult());
        assertNull(state.getFallbackMessage());
        assertFalse(state.shouldShowReanalyzeButton());
    }

    @Test
    public void mapForDisplay_statusPartial_returnsFallbackMessage() {
        // Arrange
        Analysis analysis = createAnalysis("PARTIAL");
        Plant plant = createPlant("Rose", "Rosa");
        PlantAnalysisResult parsedResult = createFullResult();

        // Act
        AnalysisUiState state = AnalysisStateMapper.mapForDisplay(
            analysis, plant, parsedResult, "PARTIAL", false, false
        );

        // Assert
        assertTrue(state.isSuccess());
        assertEquals("Some details couldn't be loaded", state.getFallbackMessage());
        assertTrue(state.shouldShowReanalyzeButton());
    }

    @Test
    public void mapForDisplay_statusFailed_returnsFullDetailsUnavailable() {
        // Arrange
        Analysis analysis = createAnalysis("FAILED");
        Plant plant = createPlant("Rose", "Rosa");

        // Act
        AnalysisUiState state = AnalysisStateMapper.mapForDisplay(
            analysis, plant, null, "FAILED", false, false
        );

        // Assert
        assertTrue(state.isSuccess());
        assertEquals("Full details unavailable", state.getFallbackMessage());
        assertTrue(state.shouldShowReanalyzeButton());
        assertNotNull(state.getResult()); // Should have minimal result
    }

    @Test
    public void mapForDisplay_statusEmpty_returnsFullDetailsUnavailable() {
        // Arrange
        Analysis analysis = createAnalysis("EMPTY");
        Plant plant = createPlant("Rose", "Rosa");

        // Act
        AnalysisUiState state = AnalysisStateMapper.mapForDisplay(
            analysis, plant, null, "EMPTY", false, false
        );

        // Assert
        assertTrue(state.isSuccess());
        assertEquals("Full details unavailable", state.getFallbackMessage());
        assertTrue(state.shouldShowReanalyzeButton());
    }

    @Test
    public void mapForDisplay_quickDiagnosis_includesDisclaimer() {
        // Arrange
        Analysis analysis = createAnalysis("OK");
        Plant plant = createPlant("Rose", "Rosa");
        PlantAnalysisResult parsedResult = createFullResult();

        // Act
        AnalysisUiState state = AnalysisStateMapper.mapForDisplay(
            analysis, plant, parsedResult, "OK", true, false
        );

        // Assert
        assertTrue(state.isSuccess());
        assertNotNull(state.getQuickDiagnosisDisclaimer());
        assertTrue(state.getQuickDiagnosisDisclaimer().contains("Quick assessment"));
    }

    @Test
    public void mapForDisplay_qualityOverridden_flagSet() {
        // Arrange
        Analysis analysis = createAnalysis("OK");
        Plant plant = createPlant("Rose", "Rosa");
        PlantAnalysisResult parsedResult = createFullResult();

        // Act
        AnalysisUiState state = AnalysisStateMapper.mapForDisplay(
            analysis, plant, parsedResult, "OK", false, true
        );

        // Assert
        assertTrue(state.isSuccess());
        assertTrue(state.isQualityOverridden());
    }

    @Test
    public void mapForDisplay_unrecoverableAnalysis_returnsError() {
        // Arrange: no photo, no name, no metadata
        Analysis analysis = new Analysis();
        analysis.id = "test-1";
        analysis.photoPath = null;
        analysis.healthScore = 0;
        analysis.summary = null;

        Plant plant = new Plant();
        plant.commonName = null;

        // Act
        AnalysisUiState state = AnalysisStateMapper.mapForDisplay(
            analysis, plant, null, "FAILED", false, false
        );

        // Assert
        assertTrue(state.hasError());
        assertEquals("Unable to load analysis data", state.getErrorMessage());
    }

    @Test
    public void mapForDisplay_reanalyzedDate_formatted() {
        // Arrange
        Analysis analysis = createAnalysis("OK");
        analysis.reAnalyzedAt = 1738368000000L; // Feb 1, 2025
        Plant plant = createPlant("Rose", "Rosa");
        PlantAnalysisResult parsedResult = createFullResult();

        // Act
        AnalysisUiState state = AnalysisStateMapper.mapForDisplay(
            analysis, plant, parsedResult, "OK", false, false
        );

        // Assert
        assertTrue(state.isSuccess());
        assertNotNull(state.getReanalyzedDate());
        assertTrue(state.getReanalyzedDate().contains("2025"));
    }

    // ==================== shouldShowReanalyze tests ====================

    @Test
    public void shouldShowReanalyze_statusOk_returnsFalse() {
        assertFalse(AnalysisStateMapper.shouldShowReanalyze("OK", "/path/to/photo.jpg"));
    }

    @Test
    public void shouldShowReanalyze_statusFailed_withPhoto_returnsTrue() {
        assertTrue(AnalysisStateMapper.shouldShowReanalyze("FAILED", "/path/to/photo.jpg"));
    }

    @Test
    public void shouldShowReanalyze_statusFailed_noPhoto_returnsFalse() {
        assertFalse(AnalysisStateMapper.shouldShowReanalyze("FAILED", null));
        assertFalse(AnalysisStateMapper.shouldShowReanalyze("FAILED", ""));
    }

    @Test
    public void shouldShowReanalyze_statusPartial_withPhoto_returnsTrue() {
        assertTrue(AnalysisStateMapper.shouldShowReanalyze("PARTIAL", "/path/to/photo.jpg"));
    }

    @Test
    public void shouldShowReanalyze_statusEmpty_withPhoto_returnsTrue() {
        assertTrue(AnalysisStateMapper.shouldShowReanalyze("EMPTY", "/path/to/photo.jpg"));
    }

    // ==================== getFallbackMessage tests ====================

    @Test
    public void getFallbackMessage_ok_returnsNull() {
        assertNull(AnalysisStateMapper.getFallbackMessage("OK"));
    }

    @Test
    public void getFallbackMessage_partial_returnsSomeDetails() {
        assertEquals("Some details couldn't be loaded",
            AnalysisStateMapper.getFallbackMessage("PARTIAL"));
    }

    @Test
    public void getFallbackMessage_failed_returnsFullDetails() {
        assertEquals("Full details unavailable",
            AnalysisStateMapper.getFallbackMessage("FAILED"));
    }

    @Test
    public void getFallbackMessage_empty_returnsFullDetails() {
        assertEquals("Full details unavailable",
            AnalysisStateMapper.getFallbackMessage("EMPTY"));
    }

    @Test
    public void getFallbackMessage_null_returnsNull() {
        assertNull(AnalysisStateMapper.getFallbackMessage(null));
    }

    @Test
    public void getFallbackMessage_unknown_returnsNull() {
        assertNull(AnalysisStateMapper.getFallbackMessage("UNKNOWN_STATUS"));
    }

    // ==================== buildMinimalResult tests ====================

    @Test
    public void buildMinimalResult_usesPlantName() {
        // Arrange
        Analysis analysis = createAnalysis("FAILED");
        Plant plant = createPlant("Rose", "Rosa rubiginosa");

        // Act
        PlantAnalysisResult result = AnalysisStateMapper.buildMinimalResult(analysis, plant);

        // Assert
        assertNotNull(result.identification);
        assertEquals("Rose", result.identification.commonName);
        assertEquals("Rosa rubiginosa", result.identification.scientificName);
    }

    @Test
    public void buildMinimalResult_nullPlant_usesUnknown() {
        // Arrange
        Analysis analysis = createAnalysis("FAILED");

        // Act
        PlantAnalysisResult result = AnalysisStateMapper.buildMinimalResult(analysis, null);

        // Assert
        assertNotNull(result.identification);
        assertEquals("Unknown Plant", result.identification.commonName);
        assertEquals("", result.identification.scientificName);
    }

    @Test
    public void buildMinimalResult_nullCommonName_usesUnknown() {
        // Arrange
        Analysis analysis = createAnalysis("FAILED");
        Plant plant = new Plant();
        plant.commonName = null;

        // Act
        PlantAnalysisResult result = AnalysisStateMapper.buildMinimalResult(analysis, plant);

        // Assert
        assertEquals("Unknown Plant", result.identification.commonName);
    }

    @Test
    public void buildMinimalResult_usesAnalysisHealthScore() {
        // Arrange
        Analysis analysis = createAnalysis("FAILED");
        analysis.healthScore = 7;
        Plant plant = createPlant("Rose", "Rosa");

        // Act
        PlantAnalysisResult result = AnalysisStateMapper.buildMinimalResult(analysis, plant);

        // Assert
        assertNotNull(result.healthAssessment);
        assertEquals(7, result.healthAssessment.score);
    }

    @Test
    public void buildMinimalResult_usesAnalysisSummary() {
        // Arrange
        Analysis analysis = createAnalysis("FAILED");
        analysis.summary = "Plant appears healthy overall";
        Plant plant = createPlant("Rose", "Rosa");

        // Act
        PlantAnalysisResult result = AnalysisStateMapper.buildMinimalResult(analysis, plant);

        // Assert
        assertNotNull(result.healthAssessment);
        assertEquals("Plant appears healthy overall", result.healthAssessment.summary);
    }

    @Test
    public void buildMinimalResult_nullSummary_usesEmptyString() {
        // Arrange
        Analysis analysis = createAnalysis("FAILED");
        analysis.summary = null;
        Plant plant = createPlant("Rose", "Rosa");

        // Act
        PlantAnalysisResult result = AnalysisStateMapper.buildMinimalResult(analysis, plant);

        // Assert
        assertEquals("", result.healthAssessment.summary);
    }

    @Test
    public void buildMinimalResult_confidence_setsUnknown() {
        // Arrange
        Analysis analysis = createAnalysis("FAILED");
        Plant plant = createPlant("Rose", "Rosa");

        // Act
        PlantAnalysisResult result = AnalysisStateMapper.buildMinimalResult(analysis, plant);

        // Assert
        assertEquals("unknown", result.identification.confidence);
    }

    // ==================== getQuickDiagnosisDisclaimer tests ====================

    @Test
    public void disclaimer_quickDiagnosis_returnsText() {
        String disclaimer = AnalysisStateMapper.getQuickDiagnosisDisclaimer(true);

        assertNotNull(disclaimer);
        assertTrue(disclaimer.contains("Quick assessment"));
        assertTrue(disclaimer.contains("results may be less precise"));
    }

    @Test
    public void disclaimer_fullAnalysis_returnsNull() {
        String disclaimer = AnalysisStateMapper.getQuickDiagnosisDisclaimer(false);

        assertNull(disclaimer);
    }

    // ==================== isUnrecoverable tests ====================

    @Test
    public void isUnrecoverable_noPhotoNoMeta_true() {
        // Arrange
        Analysis analysis = new Analysis();
        analysis.photoPath = null;
        analysis.healthScore = 0;
        analysis.summary = null;

        Plant plant = new Plant();
        plant.commonName = null;

        // Act
        boolean result = AnalysisStateMapper.isUnrecoverable(analysis, plant);

        // Assert
        assertTrue(result);
    }

    @Test
    public void isUnrecoverable_hasPhoto_false() {
        // Arrange
        Analysis analysis = new Analysis();
        analysis.photoPath = "/path/to/photo.jpg";
        analysis.healthScore = 0;
        analysis.summary = null;

        Plant plant = new Plant();
        plant.commonName = null;

        // Act
        boolean result = AnalysisStateMapper.isUnrecoverable(analysis, plant);

        // Assert
        assertFalse(result);
    }

    @Test
    public void isUnrecoverable_hasName_false() {
        // Arrange
        Analysis analysis = new Analysis();
        analysis.photoPath = null;
        analysis.healthScore = 0;
        analysis.summary = null;

        Plant plant = new Plant();
        plant.commonName = "Rose";

        // Act
        boolean result = AnalysisStateMapper.isUnrecoverable(analysis, plant);

        // Assert
        assertFalse(result);
    }

    @Test
    public void isUnrecoverable_hasHealthScore_false() {
        // Arrange
        Analysis analysis = new Analysis();
        analysis.photoPath = null;
        analysis.healthScore = 8;
        analysis.summary = null;

        Plant plant = new Plant();
        plant.commonName = null;

        // Act
        boolean result = AnalysisStateMapper.isUnrecoverable(analysis, plant);

        // Assert
        assertFalse(result);
    }

    @Test
    public void isUnrecoverable_hasSummary_false() {
        // Arrange
        Analysis analysis = new Analysis();
        analysis.photoPath = null;
        analysis.healthScore = 0;
        analysis.summary = "Healthy plant";

        Plant plant = new Plant();
        plant.commonName = null;

        // Act
        boolean result = AnalysisStateMapper.isUnrecoverable(analysis, plant);

        // Assert
        assertFalse(result);
    }

    @Test
    public void isUnrecoverable_nullPlant_noPhoto_true() {
        // Arrange
        Analysis analysis = new Analysis();
        analysis.photoPath = null;
        analysis.healthScore = 0;
        analysis.summary = null;

        // Act
        boolean result = AnalysisStateMapper.isUnrecoverable(analysis, null);

        // Assert
        assertTrue(result);
    }

    // ==================== Helper methods ====================

    private Analysis createAnalysis(String parseStatus) {
        Analysis analysis = new Analysis();
        analysis.id = "test-123";
        analysis.plantId = "plant-456";
        analysis.photoPath = "/path/to/photo.jpg";
        analysis.healthScore = 8;
        analysis.summary = "Healthy plant";
        analysis.createdAt = System.currentTimeMillis();
        analysis.parseStatus = parseStatus;
        return analysis;
    }

    private Plant createPlant(String commonName, String scientificName) {
        Plant plant = new Plant();
        plant.id = "plant-456";
        plant.commonName = commonName;
        plant.scientificName = scientificName;
        plant.createdAt = System.currentTimeMillis();
        plant.updatedAt = System.currentTimeMillis();
        return plant;
    }

    private PlantAnalysisResult createFullResult() {
        PlantAnalysisResult result = new PlantAnalysisResult();

        result.identification = new PlantAnalysisResult.Identification();
        result.identification.commonName = "Rose";
        result.identification.scientificName = "Rosa rubiginosa";
        result.identification.confidence = "high";

        result.healthAssessment = new PlantAnalysisResult.HealthAssessment();
        result.healthAssessment.score = 8;
        result.healthAssessment.summary = "Plant is healthy";

        return result;
    }
}
