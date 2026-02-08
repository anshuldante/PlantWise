package com.leafiq.app.domain.service;

import com.leafiq.app.ai.AIProvider;
import com.leafiq.app.ai.AIProviderException;
import com.leafiq.app.ai.PromptBuilder;
import com.leafiq.app.data.entity.Analysis;
import com.leafiq.app.data.model.PlantAnalysisResult;

import java.io.IOException;
import java.util.List;

/**
 * Service layer that encapsulates AI provider interaction.
 * Handles prompt building and API calls for plant analysis.
 * <p>
 * Responsibilities:
 * - Build analysis prompts with context (known plant name, previous analyses)
 * - Delegate to AIProvider for actual API call
 * - Check provider capabilities (vision support)
 * <p>
 * The AIProvider instance is passed in by the caller (not created here).
 * This keeps the service stateless and testable.
 */
public class AIAnalysisService {

    /**
     * Creates an AIAnalysisService.
     * No persistent dependencies - provider is passed per-call.
     */
    public AIAnalysisService() {
        // No dependencies needed - provider passed in analyze()
    }

    /**
     * Analyzes a plant photo using the specified AI provider.
     * Builds prompt with context and calls provider API.
     *
     * @param provider The AI provider to use (created by caller)
     * @param base64Image Base64-encoded image string
     * @param knownPlantName Previously identified plant name (null if first analysis)
     * @param previousAnalyses List of previous analyses for this plant (null if first analysis)
     * @param location Plant location (null if not set) - used for location-aware care advice
     * @return PlantAnalysisResult containing identification, health assessment, and care plan
     * @throws AIProviderException if API call fails
     * @throws IOException if network/IO error occurs
     */
    public PlantAnalysisResult analyze(AIProvider provider,
                                      String base64Image,
                                      String knownPlantName,
                                      List<Analysis> previousAnalyses,
                                      String location)
            throws AIProviderException, IOException {
        // Build prompt with plant context
        String prompt = PromptBuilder.buildAnalysisPrompt(
                knownPlantName,
                previousAnalyses,
                location
        );

        // Call AI provider
        return provider.analyzePhoto(base64Image, prompt);
    }

    /**
     * Checks if the provider supports vision (image analysis).
     * Text-only providers (like Perplexity's sonar model) will return false.
     *
     * @param provider The AI provider to check
     * @return true if provider supports image analysis, false otherwise
     */
    public boolean supportsVision(AIProvider provider) {
        return provider.supportsVision();
    }
}
