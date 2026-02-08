package com.leafiq.app.ui.analysis;

import com.leafiq.app.data.model.PlantAnalysisResult;

/**
 * Immutable UI state class for analysis screen.
 * Represents all possible states of the analysis flow.
 * <p>
 * States:
 * - IDLE: Initial state, no analysis in progress
 * - LOADING: Analysis in progress, show loading UI
 * - SUCCESS: Analysis complete, show results
 * - ERROR: Analysis failed, show error message
 * <p>
 * Use factory methods to create state instances (idle(), loading(), success(), error()).
 */
public class AnalysisUiState {

    /**
     * State enumeration for analysis flow.
     */
    public enum State {
        IDLE,       // No analysis in progress
        LOADING,    // Analysis running
        SUCCESS,    // Analysis complete with result
        ERROR       // Analysis failed with error
    }

    private final State state;
    private final PlantAnalysisResult result;
    private final String errorMessage;
    private final String visionUnsupportedProvider;

    /**
     * Private constructor - use factory methods instead.
     */
    private AnalysisUiState(State state,
                           PlantAnalysisResult result,
                           String errorMessage,
                           String visionUnsupportedProvider) {
        this.state = state;
        this.result = result;
        this.errorMessage = errorMessage;
        this.visionUnsupportedProvider = visionUnsupportedProvider;
    }

    // ==================== Factory Methods ====================

    /**
     * Creates an IDLE state.
     * Used as initial state before any analysis.
     */
    public static AnalysisUiState idle() {
        return new AnalysisUiState(State.IDLE, null, null, null);
    }

    /**
     * Creates a LOADING state.
     * Used while analysis is in progress.
     */
    public static AnalysisUiState loading() {
        return new AnalysisUiState(State.LOADING, null, null, null);
    }

    /**
     * Creates a SUCCESS state with analysis result.
     *
     * @param result The analysis result to display
     */
    public static AnalysisUiState success(PlantAnalysisResult result) {
        return new AnalysisUiState(State.SUCCESS, result, null, null);
    }

    /**
     * Creates an ERROR state with error message.
     *
     * @param message Human-readable error message
     */
    public static AnalysisUiState error(String message) {
        return new AnalysisUiState(State.ERROR, null, message, null);
    }

    /**
     * Creates an ERROR state for vision-not-supported scenario.
     * Provides specific error message guiding user to switch providers.
     *
     * @param providerName Name of the text-only provider
     */
    public static AnalysisUiState visionNotSupported(String providerName) {
        String message = providerName + " is text-only. Switch to Claude, ChatGPT, or Gemini in Settings for image analysis.";
        return new AnalysisUiState(State.ERROR, null, message, providerName);
    }

    // ==================== Getters ====================

    /**
     * Gets the current state.
     */
    public State getState() {
        return state;
    }

    /**
     * Gets the analysis result (only non-null when state is SUCCESS).
     */
    public PlantAnalysisResult getResult() {
        return result;
    }

    /**
     * Gets the error message (only non-null when state is ERROR).
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Gets the vision-unsupported provider name (only non-null for vision errors).
     */
    public String getVisionUnsupportedProvider() {
        return visionUnsupportedProvider;
    }

    // ==================== Convenience Methods ====================

    /**
     * Checks if analysis is currently loading.
     */
    public boolean isLoading() {
        return state == State.LOADING;
    }

    /**
     * Checks if analysis completed successfully.
     */
    public boolean isSuccess() {
        return state == State.SUCCESS;
    }

    /**
     * Checks if analysis failed with error.
     */
    public boolean hasError() {
        return state == State.ERROR;
    }

    /**
     * Checks if error is specifically due to vision not being supported.
     */
    public boolean isVisionUnsupported() {
        return visionUnsupportedProvider != null;
    }
}
