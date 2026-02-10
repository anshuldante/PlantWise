package com.leafiq.app.domain.usecase;

import android.content.Context;
import android.net.Uri;

import com.leafiq.app.ai.AIProvider;
import com.leafiq.app.ai.AIProviderException;
import com.leafiq.app.ai.NetworkUtils;
import com.leafiq.app.data.entity.Analysis;
import com.leafiq.app.data.entity.Plant;
import com.leafiq.app.data.model.PlantAnalysisResult;
import com.leafiq.app.data.repository.PlantRepository;
import com.leafiq.app.domain.service.AIAnalysisService;
import com.leafiq.app.domain.service.ImagePreprocessor;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Use case that orchestrates the full plant analysis pipeline.
 * <p>
 * Flow:
 * 1. Check if provider supports vision (fail early if text-only)
 * 2. Preprocess image (resize, compress, base64 encode)
 * 3. Load existing plant context (if re-analyzing)
 * 4. Call AI analysis service with context
 * 5. Return result via callback
 * <p>
 * All operations run on background thread (networkExecutor).
 * Results delivered via Callback interface.
 */
public class AnalyzePlantUseCase {

    private final Context context;
    private final ImagePreprocessor imagePreprocessor;
    private final AIAnalysisService aiAnalysisService;
    private final PlantRepository plantRepository;
    private final Executor networkExecutor;

    /**
     * Callback interface for async result delivery.
     */
    public interface Callback {
        /**
         * Called when analysis completes successfully.
         *
         * @param result Parsed analysis result from AI
         */
        void onSuccess(PlantAnalysisResult result);

        /**
         * Called when an error occurs during analysis.
         *
         * @param message Human-readable error message
         */
        void onError(String message);

        /**
         * Called when selected provider doesn't support vision.
         *
         * @param providerDisplayName Name of the provider (for error message)
         */
        void onVisionNotSupported(String providerDisplayName);
    }

    /**
     * Creates an AnalyzePlantUseCase with all required dependencies.
     *
     * @param context Application context for network connectivity checks
     * @param imagePreprocessor Service for image preparation
     * @param aiAnalysisService Service for AI API calls
     * @param plantRepository Repository for plant data access
     * @param networkExecutor Executor for background network operations
     */
    public AnalyzePlantUseCase(Context context,
                              ImagePreprocessor imagePreprocessor,
                              AIAnalysisService aiAnalysisService,
                              PlantRepository plantRepository,
                              Executor networkExecutor) {
        this.context = context;
        this.imagePreprocessor = imagePreprocessor;
        this.aiAnalysisService = aiAnalysisService;
        this.plantRepository = plantRepository;
        this.networkExecutor = networkExecutor;
    }

    /**
     * Executes the plant analysis pipeline.
     * Runs entirely on background thread (networkExecutor).
     *
     * @param imageUri URI of the plant photo
     * @param plantId Plant ID if re-analyzing existing plant, null for new plant
     * @param provider AI provider to use (created by caller via AIProviderFactory)
     * @param callback Callback for result delivery
     */
    public void execute(Uri imageUri, String plantId, AIProvider provider, Callback callback) {
        networkExecutor.execute(() -> {
            // Pre-check network connectivity before starting analysis
            if (!NetworkUtils.isNetworkAvailable(context)) {
                callback.onError("No internet connection. Please check your network.");
                return;
            }

            try {
                // 1. Check vision support (fail early)
                if (!aiAnalysisService.supportsVision(provider)) {
                    callback.onVisionNotSupported(provider.getDisplayName());
                    return;
                }

                // 2. Preprocess image to base64
                String base64Image = imagePreprocessor.prepareForApi(imageUri);

                // 3. Load existing plant context (if re-analyzing)
                String knownPlantName = null;
                String location = null;
                List<Analysis> previousAnalyses = null;

                if (plantId != null) {
                    // Synchronous calls are safe - we're already on background thread
                    Plant existingPlant = plantRepository.getPlantByIdSync(plantId);
                    if (existingPlant != null) {
                        knownPlantName = existingPlant.commonName;
                        location = existingPlant.location;
                    }

                    previousAnalyses = plantRepository.getRecentAnalysesSync(plantId);
                }

                // 4. Call AI analysis service
                PlantAnalysisResult result = aiAnalysisService.analyze(
                        provider,
                        base64Image,
                        knownPlantName,
                        previousAnalyses,
                        location
                );

                // 5. Success - deliver result
                callback.onSuccess(result);

            } catch (AIProviderException e) {
                callback.onError(NetworkUtils.classifyException(e, e.getHttpStatusCode()));
            } catch (IOException e) {
                callback.onError(NetworkUtils.classifyException(e, 0));
            }
        });
    }

    /**
     * Executes the plant analysis pipeline with user corrections.
     * Runs entirely on background thread (networkExecutor).
     *
     * @param imageUri URI of the plant photo
     * @param plantId Plant ID if re-analyzing existing plant, null for new plant
     * @param correctedName User-corrected plant name (null if not corrected)
     * @param additionalContext Additional user-provided context (null if none)
     * @param provider AI provider to use (created by caller via AIProviderFactory)
     * @param callback Callback for result delivery
     */
    public void executeWithCorrections(
            Uri imageUri,
            String plantId,
            String correctedName,
            String additionalContext,
            AIProvider provider,
            Callback callback) {
        networkExecutor.execute(() -> {
            // Pre-check network connectivity before starting analysis
            if (!NetworkUtils.isNetworkAvailable(context)) {
                callback.onError("No internet connection. Please check your network.");
                return;
            }

            try {
                // 1. Check vision support (fail early)
                if (!aiAnalysisService.supportsVision(provider)) {
                    callback.onVisionNotSupported(provider.getDisplayName());
                    return;
                }

                // 2. Preprocess image to base64
                String base64Image = imagePreprocessor.prepareForApi(imageUri);

                // 3. Load existing plant context (if re-analyzing)
                List<Analysis> previousAnalyses = null;
                String location = null;
                if (plantId != null) {
                    Plant existingPlant = plantRepository.getPlantByIdSync(plantId);
                    if (existingPlant != null) {
                        location = existingPlant.location;
                    }
                    previousAnalyses = plantRepository.getRecentAnalysesSync(plantId);
                }

                // 4. Call AI analysis service with corrections
                PlantAnalysisResult result = aiAnalysisService.analyzeWithCorrections(
                        provider, base64Image, correctedName,
                        additionalContext, previousAnalyses, location);

                // 5. Success - deliver result
                callback.onSuccess(result);

            } catch (AIProviderException e) {
                callback.onError(NetworkUtils.classifyException(e, e.getHttpStatusCode()));
            } catch (IOException e) {
                callback.onError(NetworkUtils.classifyException(e, 0));
            }
        });
    }
}
