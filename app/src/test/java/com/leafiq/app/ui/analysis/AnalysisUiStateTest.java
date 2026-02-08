package com.leafiq.app.ui.analysis;

import static com.google.common.truth.Truth.assertThat;

import com.leafiq.app.data.model.PlantAnalysisResult;

import org.junit.Test;

public class AnalysisUiStateTest {

    // ==================== Factory Method Tests ====================

    @Test
    public void idle_hasIdleState() {
        AnalysisUiState state = AnalysisUiState.idle();
        assertThat(state.getState()).isEqualTo(AnalysisUiState.State.IDLE);
    }

    @Test
    public void idle_hasNullResult() {
        AnalysisUiState state = AnalysisUiState.idle();
        assertThat(state.getResult()).isNull();
    }

    @Test
    public void idle_hasNullErrorMessage() {
        AnalysisUiState state = AnalysisUiState.idle();
        assertThat(state.getErrorMessage()).isNull();
    }

    @Test
    public void loading_hasLoadingState() {
        AnalysisUiState state = AnalysisUiState.loading();
        assertThat(state.getState()).isEqualTo(AnalysisUiState.State.LOADING);
    }

    @Test
    public void loading_isLoadingReturnsTrue() {
        AnalysisUiState state = AnalysisUiState.loading();
        assertThat(state.isLoading()).isTrue();
    }

    @Test
    public void success_hasSuccessState() {
        PlantAnalysisResult result = new PlantAnalysisResult();
        AnalysisUiState state = AnalysisUiState.success(result);
        assertThat(state.getState()).isEqualTo(AnalysisUiState.State.SUCCESS);
    }

    @Test
    public void success_holdsResult() {
        PlantAnalysisResult result = new PlantAnalysisResult();
        result.funFact = "Test fact";
        AnalysisUiState state = AnalysisUiState.success(result);
        assertThat(state.getResult()).isSameInstanceAs(result);
        assertThat(state.getResult().funFact).isEqualTo("Test fact");
    }

    @Test
    public void success_isSuccessReturnsTrue() {
        AnalysisUiState state = AnalysisUiState.success(new PlantAnalysisResult());
        assertThat(state.isSuccess()).isTrue();
    }

    @Test
    public void success_hasNoError() {
        AnalysisUiState state = AnalysisUiState.success(new PlantAnalysisResult());
        assertThat(state.hasError()).isFalse();
        assertThat(state.getErrorMessage()).isNull();
    }

    @Test
    public void error_hasErrorState() {
        AnalysisUiState state = AnalysisUiState.error("Something went wrong");
        assertThat(state.getState()).isEqualTo(AnalysisUiState.State.ERROR);
    }

    @Test
    public void error_holdsErrorMessage() {
        AnalysisUiState state = AnalysisUiState.error("Network timeout");
        assertThat(state.getErrorMessage()).isEqualTo("Network timeout");
    }

    @Test
    public void error_hasErrorReturnsTrue() {
        AnalysisUiState state = AnalysisUiState.error("fail");
        assertThat(state.hasError()).isTrue();
    }

    @Test
    public void error_hasNullResult() {
        AnalysisUiState state = AnalysisUiState.error("fail");
        assertThat(state.getResult()).isNull();
    }

    @Test
    public void error_isNotVisionUnsupported() {
        AnalysisUiState state = AnalysisUiState.error("Generic error");
        assertThat(state.isVisionUnsupported()).isFalse();
        assertThat(state.getVisionUnsupportedProvider()).isNull();
    }

    // ==================== Vision Not Supported Tests ====================

    @Test
    public void visionNotSupported_hasErrorState() {
        AnalysisUiState state = AnalysisUiState.visionNotSupported("Perplexity");
        assertThat(state.getState()).isEqualTo(AnalysisUiState.State.ERROR);
    }

    @Test
    public void visionNotSupported_holdsProviderName() {
        AnalysisUiState state = AnalysisUiState.visionNotSupported("Perplexity");
        assertThat(state.getVisionUnsupportedProvider()).isEqualTo("Perplexity");
    }

    @Test
    public void visionNotSupported_isVisionUnsupportedReturnsTrue() {
        AnalysisUiState state = AnalysisUiState.visionNotSupported("Perplexity");
        assertThat(state.isVisionUnsupported()).isTrue();
    }

    @Test
    public void visionNotSupported_errorMessageContainsProviderName() {
        AnalysisUiState state = AnalysisUiState.visionNotSupported("Perplexity");
        assertThat(state.getErrorMessage()).contains("Perplexity");
        assertThat(state.getErrorMessage()).contains("text-only");
    }

    // ==================== Convenience Method Cross-State Tests ====================

    @Test
    public void idle_convenienceMethodsReturnFalse() {
        AnalysisUiState state = AnalysisUiState.idle();
        assertThat(state.isLoading()).isFalse();
        assertThat(state.isSuccess()).isFalse();
        assertThat(state.hasError()).isFalse();
        assertThat(state.isVisionUnsupported()).isFalse();
    }

    @Test
    public void loading_otherConvenienceMethodsReturnFalse() {
        AnalysisUiState state = AnalysisUiState.loading();
        assertThat(state.isSuccess()).isFalse();
        assertThat(state.hasError()).isFalse();
    }

    @Test
    public void success_otherConvenienceMethodsReturnFalse() {
        AnalysisUiState state = AnalysisUiState.success(new PlantAnalysisResult());
        assertThat(state.isLoading()).isFalse();
        assertThat(state.hasError()).isFalse();
    }
}
