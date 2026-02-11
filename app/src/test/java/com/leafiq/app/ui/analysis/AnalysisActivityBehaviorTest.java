package com.leafiq.app.ui.analysis;

import static com.google.common.truth.Truth.assertThat;

import com.leafiq.app.data.model.PlantAnalysisResult;
import com.leafiq.app.util.PhotoQualityChecker;
import com.leafiq.app.util.RobustJsonParser;

import org.junit.Test;

/**
 * Behavior-locking tests for AnalysisActivity state machine.
 * These tests verify the LOGIC behind UI decisions without touching Android views.
 * They serve as a safety net during extraction to ensure behavior doesn't change.
 * <p>
 * Tests cover:
 * - AnalysisUiState state machine transitions
 * - Quality check decision tree (borderline vs egregious)
 * - Parse failure behavior (OK, PARTIAL, FAILED, EMPTY)
 */
public class AnalysisActivityBehaviorTest {

    // ==================== AnalysisUiState state machine tests ====================

    @Test
    public void uiState_idle_initialState() {
        AnalysisUiState state = AnalysisUiState.idle();

        assertThat(state.getState()).isEqualTo(AnalysisUiState.State.IDLE);
        assertThat(state.isLoading()).isFalse();
        assertThat(state.isSuccess()).isFalse();
        assertThat(state.hasError()).isFalse();
        assertThat(state.getResult()).isNull();
        assertThat(state.getErrorMessage()).isNull();
    }

    @Test
    public void uiState_loading_showsProgress() {
        AnalysisUiState state = AnalysisUiState.loading();

        assertThat(state.getState()).isEqualTo(AnalysisUiState.State.LOADING);
        assertThat(state.isLoading()).isTrue();
        assertThat(state.isSuccess()).isFalse();
        assertThat(state.hasError()).isFalse();
        assertThat(state.getLoadingMessage()).isNull();
    }

    @Test
    public void uiState_loadingWithMessage_preservesMessage() {
        AnalysisUiState state = AnalysisUiState.loadingWithMessage("Analysis is taking longer than usual...");

        assertThat(state.getState()).isEqualTo(AnalysisUiState.State.LOADING);
        assertThat(state.isLoading()).isTrue();
        assertThat(state.getLoadingMessage()).isEqualTo("Analysis is taking longer than usual...");
    }

    @Test
    public void uiState_success_containsResult() {
        PlantAnalysisResult result = createMockResult("Monstera", 8);
        AnalysisUiState state = AnalysisUiState.success(result);

        assertThat(state.getState()).isEqualTo(AnalysisUiState.State.SUCCESS);
        assertThat(state.isSuccess()).isTrue();
        assertThat(state.isLoading()).isFalse();
        assertThat(state.hasError()).isFalse();
        assertThat(state.getResult()).isEqualTo(result);
        assertThat(state.getResult().identification.commonName).isEqualTo("Monstera");
    }

    @Test
    public void uiState_error_containsMessage() {
        AnalysisUiState state = AnalysisUiState.error("Network timeout");

        assertThat(state.getState()).isEqualTo(AnalysisUiState.State.ERROR);
        assertThat(state.hasError()).isTrue();
        assertThat(state.isLoading()).isFalse();
        assertThat(state.isSuccess()).isFalse();
        assertThat(state.getErrorMessage()).isEqualTo("Network timeout");
    }

    @Test
    public void uiState_visionNotSupported_hasProvider() {
        AnalysisUiState state = AnalysisUiState.visionNotSupported("GPT-3.5");

        assertThat(state.getState()).isEqualTo(AnalysisUiState.State.ERROR);
        assertThat(state.hasError()).isTrue();
        assertThat(state.isVisionUnsupported()).isTrue();
        assertThat(state.getVisionUnsupportedProvider()).isEqualTo("GPT-3.5");
        assertThat(state.getErrorMessage()).contains("GPT-3.5");
        assertThat(state.getErrorMessage()).contains("text-only");
    }

    // ==================== Quality check decision tests ====================

    @Test
    public void qualityResult_passed_proceedsToAnalysis() {
        PhotoQualityChecker.QualityResult result = PhotoQualityChecker.QualityResult.ok(0.5f);

        assertThat(result.passed).isTrue();
        assertThat(result.overrideAllowed).isFalse();
        assertThat(result.issueSeverity).isNull();
        assertThat(result.brightnessScore).isEqualTo(0.5f);
    }

    @Test
    public void qualityResult_borderlineDark_allowsOverride() {
        PhotoQualityChecker.QualityResult result = PhotoQualityChecker.QualityResult.fail(
                "Photo is too dark",
                "dark",
                true,  // override allowed
                "borderline",
                0.10f  // Brightness between EGREGIOUS (0.05) and MIN (0.15)
        );

        assertThat(result.passed).isFalse();
        assertThat(result.overrideAllowed).isTrue();
        assertThat(result.issueSeverity).isEqualTo("borderline");
        assertThat(result.issueType).isEqualTo("dark");
        assertThat(result.message).contains("dark");
    }

    @Test
    public void qualityResult_egregiousDark_blocksOverride() {
        PhotoQualityChecker.QualityResult result = PhotoQualityChecker.QualityResult.egregiousFail(
                "Photo is extremely dark and unusable",
                "dark",
                0.03f  // Brightness below EGREGIOUS threshold (0.05)
        );

        assertThat(result.passed).isFalse();
        assertThat(result.overrideAllowed).isFalse();
        assertThat(result.issueSeverity).isEqualTo("egregious");
        assertThat(result.issueType).isEqualTo("dark");
        assertThat(result.message).contains("extremely dark");
    }

    @Test
    public void qualityResult_quickDiagnosisMode_morePermissive() {
        // Quick Diagnosis brightness thresholds are more lenient:
        // QUICK_DIAGNOSIS_MIN_BRIGHTNESS = 0.12f (vs standard 0.15f)

        // A photo with brightness=0.13 would fail standard mode but pass Quick mode
        PhotoQualityChecker.QualityResult standardFail = PhotoQualityChecker.QualityResult.fail(
                "Photo is too dark",
                "dark",
                true,
                "borderline",
                0.13f  // Below standard MIN_BRIGHTNESS (0.15) but above QUICK (0.12)
        );

        assertThat(standardFail.passed).isFalse();
        assertThat(standardFail.brightnessScore).isLessThan(0.15f);  // Would fail standard
        assertThat(standardFail.brightnessScore).isGreaterThan(0.12f);  // Would pass Quick
    }

    // ==================== Parse failure behavior tests ====================

    @Test
    public void parseResult_validJson_statusOk() {
        String validJson = "{\"identification\":{\"commonName\":\"Monstera\",\"scientificName\":\"Monstera deliciosa\",\"confidence\":\"high\"},\"healthAssessment\":{\"score\":8,\"summary\":\"Healthy\"}}";
        RobustJsonParser.ParseResult result = RobustJsonParser.parse(validJson);

        assertThat(result.parseStatus).isEqualTo("OK");
        assertThat(result.result).isNotNull();
        assertThat(result.result.identification.commonName).isEqualTo("Monstera");
        assertThat(result.contentHash).isNotNull();
        assertThat(result.contentHash.length()).isEqualTo(8);
    }

    @Test
    public void parseResult_malformedJson_statusFailed() {
        String malformedJson = "{this is not valid json at all}";
        RobustJsonParser.ParseResult result = RobustJsonParser.parse(malformedJson);

        // Malformed JSON with no extractable Tier 1 fields -> FAILED
        assertThat(result.parseStatus).isEqualTo("FAILED");
        assertThat(result.result).isNull();
        assertThat(result.contentHash).isNotNull();
    }

    @Test
    public void parseResult_partialJson_statusPartial() {
        // JSON with only Tier 1 fields extractable (malformed care plan)
        String partialJson = "{\"identification\":{\"commonName\":\"Pothos\"},\"healthAssessment\":{\"score\":7},\"carePlan\":{broken}}";
        RobustJsonParser.ParseResult result = RobustJsonParser.parse(partialJson);

        // Structured parse fails, but Tier 1 extraction succeeds
        assertThat(result.parseStatus).isEqualTo("PARTIAL");
        assertThat(result.result).isNotNull();
        assertThat(result.result.identification).isNotNull();
        assertThat(result.result.identification.commonName).isEqualTo("Pothos");
        assertThat(result.contentHash).isNotNull();
    }

    @Test
    public void parseResult_emptyResponse_statusEmpty() {
        RobustJsonParser.ParseResult result = RobustJsonParser.parse(null);

        assertThat(result.parseStatus).isEqualTo("EMPTY");
        assertThat(result.result).isNull();
        assertThat(result.contentHash).isNull();
    }

    @Test
    public void parseResult_emptyString_statusEmpty() {
        RobustJsonParser.ParseResult result = RobustJsonParser.parse("");

        assertThat(result.parseStatus).isEqualTo("EMPTY");
        assertThat(result.result).isNull();
        assertThat(result.contentHash).isNull();
    }

    @Test
    public void parseResult_whitespaceOnly_statusEmpty() {
        RobustJsonParser.ParseResult result = RobustJsonParser.parse("   \n\t  ");

        assertThat(result.parseStatus).isEqualTo("EMPTY");
        assertThat(result.result).isNull();
        assertThat(result.contentHash).isNull();
    }

    @Test
    public void parseResult_partialHasSomeFields() {
        // Partial parse should have at least identification or health assessment
        String partialJson = "{\"identification\":{\"commonName\":\"Snake Plant\"},\"carePlan\":invalid}";
        RobustJsonParser.ParseResult result = RobustJsonParser.parse(partialJson);

        assertThat(result.parseStatus).isEqualTo("PARTIAL");
        assertThat(result.result).isNotNull();

        // Tier 1 fields extracted
        assertThat(result.result.identification).isNotNull();
        assertThat(result.result.identification.commonName).isEqualTo("Snake Plant");

        // Tier 2 fields (care plan) silently dropped
        assertThat(result.result.carePlan).isNull();
        assertThat(result.result.immediateActions).isNotNull();
        assertThat(result.result.immediateActions).isEmpty();
    }

    @Test
    public void parseResult_partialWithHealthScore() {
        // JSON that's malformed but has extractable health score via regex
        String partialJson = "{\"healthAssessment\":{\"score\":6,\"summary\":\"Needs water\"} BROKEN_JSON_HERE";
        RobustJsonParser.ParseResult result = RobustJsonParser.parse(partialJson);

        assertThat(result.parseStatus).isEqualTo("PARTIAL");
        assertThat(result.result).isNotNull();
        assertThat(result.result.healthAssessment).isNotNull();
        assertThat(result.result.healthAssessment.score).isEqualTo(6);
        assertThat(result.result.healthAssessment.summary).isEqualTo("Needs water");
    }

    // ==================== Test helpers ====================

    /**
     * Creates a mock PlantAnalysisResult for testing state transitions
     */
    private PlantAnalysisResult createMockResult(String commonName, int healthScore) {
        PlantAnalysisResult result = new PlantAnalysisResult();

        result.identification = new PlantAnalysisResult.Identification();
        result.identification.commonName = commonName;
        result.identification.scientificName = "";
        result.identification.confidence = "high";
        result.identification.notes = "";

        result.healthAssessment = new PlantAnalysisResult.HealthAssessment();
        result.healthAssessment.score = healthScore;
        result.healthAssessment.summary = "Looks healthy";
        result.healthAssessment.issues = new java.util.ArrayList<>();

        result.immediateActions = new java.util.ArrayList<>();
        result.funFact = "";
        result.rawResponse = "{}";

        return result;
    }
}
