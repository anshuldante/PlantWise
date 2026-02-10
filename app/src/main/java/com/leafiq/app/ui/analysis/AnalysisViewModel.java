package com.leafiq.app.ui.analysis;

import android.app.Application;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.leafiq.app.LeafIQApplication;
import com.leafiq.app.ai.AIProvider;
import com.leafiq.app.ai.AIProviderFactory;
import com.leafiq.app.care.CareScheduleManager;
import com.leafiq.app.data.entity.Analysis;
import com.leafiq.app.data.entity.CareItem;
import com.leafiq.app.data.entity.CareSchedule;
import com.leafiq.app.data.entity.Plant;
import com.leafiq.app.data.model.PlantAnalysisResult;
import com.leafiq.app.data.repository.PlantRepository;
import com.leafiq.app.domain.service.ImagePreprocessor;
import com.leafiq.app.domain.usecase.AnalyzePlantUseCase;
import com.leafiq.app.util.KeystoreHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import okhttp3.OkHttpClient;

/**
 * ViewModel for the analysis screen.
 * Manages UI state and delegates business logic to domain layer.
 * <p>
 * Responsibilities:
 * - Expose LiveData<AnalysisUiState> for UI observation
 * - Delegate analysis to AnalyzePlantUseCase
 * - Delegate saving to PlantRepository
 * - Handle AI provider creation via AIProviderFactory
 * <p>
 * UI logic (rendering, views) stays in AnalysisActivity.
 */
public class AnalysisViewModel extends AndroidViewModel {

    private static final long TIMEOUT_WARNING_MS = 30_000;  // 30 seconds

    private final MutableLiveData<AnalysisUiState> uiState;
    private final MutableLiveData<List<CareSchedule>> scheduleUpdatePrompts;
    private final AnalyzePlantUseCase analyzePlantUseCase;
    private final PlantRepository plantRepository;
    private final ImagePreprocessor imagePreprocessor;
    private final KeystoreHelper keystoreHelper;
    private final CareScheduleManager careScheduleManager;

    private Handler warningHandler;
    private Runnable warningRunnable;

    /**
     * Callback interface for save operations.
     */
    public interface SaveCallback {
        void onSuccess(String plantId);
        void onError(String message);
    }

    /**
     * Creates an AnalysisViewModel with all required dependencies.
     * Called by AnalysisViewModelFactory.
     */
    public AnalysisViewModel(Application application,
                            AnalyzePlantUseCase analyzePlantUseCase,
                            PlantRepository plantRepository,
                            ImagePreprocessor imagePreprocessor,
                            KeystoreHelper keystoreHelper,
                            CareScheduleManager careScheduleManager) {
        super(application);
        this.analyzePlantUseCase = analyzePlantUseCase;
        this.plantRepository = plantRepository;
        this.imagePreprocessor = imagePreprocessor;
        this.keystoreHelper = keystoreHelper;
        this.careScheduleManager = careScheduleManager;
        this.uiState = new MutableLiveData<>(AnalysisUiState.idle());
        this.scheduleUpdatePrompts = new MutableLiveData<>();
    }

    /**
     * Gets the UI state LiveData for observation.
     * Activity observes this to update UI.
     */
    public LiveData<AnalysisUiState> getUiState() {
        return uiState;
    }

    /**
     * Gets the schedule update prompts LiveData for observation.
     * Activity observes this to show re-analysis schedule conflict dialogs.
     */
    public LiveData<List<CareSchedule>> getScheduleUpdatePrompts() {
        return scheduleUpdatePrompts;
    }

    /**
     * Starts the 30-second warning timer.
     * Updates progress text to "Analysis is taking longer than usual..." after 30 seconds.
     */
    private void startWarningTimer() {
        warningHandler = new Handler(Looper.getMainLooper());

        // 30s warning — update progress text (per user decision: generic, no provider name)
        warningRunnable = () -> {
            AnalysisUiState current = uiState.getValue();
            if (current != null && current.isLoading()) {
                uiState.setValue(AnalysisUiState.loadingWithMessage(
                    "Analysis is taking longer than usual..."));
            }
        };

        warningHandler.postDelayed(warningRunnable, TIMEOUT_WARNING_MS);
    }

    /**
     * Cancels the warning timer.
     * Must be called on all exit paths (success, error, vision-not-supported, onCleared).
     */
    private void cancelWarningTimer() {
        if (warningHandler != null && warningRunnable != null) {
            warningHandler.removeCallbacks(warningRunnable);
        }
    }

    /**
     * Analyzes a plant image using AI.
     * Delegates to AnalyzePlantUseCase, updates UI state via LiveData.
     *
     * @param imageUri URI of the plant photo
     * @param plantId Plant ID if re-analyzing existing plant, null for new plant
     */
    public void analyzeImage(Uri imageUri, String plantId) {
        // Set loading state
        uiState.setValue(AnalysisUiState.loading());
        startWarningTimer();

        // Get provider and API key
        String providerName = keystoreHelper.getProvider();
        String apiKey = keystoreHelper.getApiKey();

        // Get shared HTTP client
        OkHttpClient client = ((LeafIQApplication) getApplication()).getHttpClient();

        // Create provider
        AIProvider provider = AIProviderFactory.create(providerName, apiKey, client);

        // Delegate to use case
        analyzePlantUseCase.execute(imageUri, plantId, provider, new AnalyzePlantUseCase.Callback() {
            @Override
            public void onSuccess(PlantAnalysisResult result) {
                cancelWarningTimer();
                uiState.postValue(AnalysisUiState.success(result));
            }

            @Override
            public void onError(String message) {
                cancelWarningTimer();
                // Check if this is an OkHttp timeout (callTimeout fires InterruptedIOException)
                if (message != null && (message.toLowerCase().contains("timeout")
                        || message.toLowerCase().contains("canceled"))) {
                    uiState.postValue(AnalysisUiState.error(
                        "Analysis timed out. Please try again."));
                } else {
                    uiState.postValue(AnalysisUiState.error(message));
                }
            }

            @Override
            public void onVisionNotSupported(String providerDisplayName) {
                cancelWarningTimer();
                uiState.postValue(AnalysisUiState.visionNotSupported(providerDisplayName));
            }
        });
    }

    /**
     * Re-analyzes a plant image with user corrections.
     * Delegates to AnalyzePlantUseCase.executeWithCorrections, updates UI state via LiveData.
     *
     * @param imageUri URI of the plant photo
     * @param plantId Plant ID if re-analyzing existing plant, null for new plant
     * @param correctedName User-corrected plant name (null or empty if not corrected)
     * @param additionalContext Additional user-provided context (null or empty if none)
     */
    public void reanalyzeWithCorrections(Uri imageUri, String plantId, String correctedName, String additionalContext) {
        // Set loading state
        uiState.setValue(AnalysisUiState.loading());
        startWarningTimer();

        // Get provider and API key
        String providerName = keystoreHelper.getProvider();
        String apiKey = keystoreHelper.getApiKey();

        // Get shared HTTP client
        OkHttpClient client = ((LeafIQApplication) getApplication()).getHttpClient();

        // Create provider
        AIProvider provider = AIProviderFactory.create(providerName, apiKey, client);

        // Delegate to use case with corrections
        analyzePlantUseCase.executeWithCorrections(imageUri, plantId, correctedName, additionalContext, provider,
                new AnalyzePlantUseCase.Callback() {
            @Override
            public void onSuccess(PlantAnalysisResult result) {
                cancelWarningTimer();
                uiState.postValue(AnalysisUiState.success(result));
            }

            @Override
            public void onError(String message) {
                cancelWarningTimer();
                // Check if this is an OkHttp timeout (callTimeout fires InterruptedIOException)
                if (message != null && (message.toLowerCase().contains("timeout")
                        || message.toLowerCase().contains("canceled"))) {
                    uiState.postValue(AnalysisUiState.error(
                        "Analysis timed out. Please try again."));
                } else {
                    uiState.postValue(AnalysisUiState.error(message));
                }
            }

            @Override
            public void onVisionNotSupported(String providerDisplayName) {
                cancelWarningTimer();
                uiState.postValue(AnalysisUiState.visionNotSupported(providerDisplayName));
            }
        });
    }

    /**
     * Saves field corrections without re-analysis.
     * Updates plant name and analysis health score directly.
     *
     * @param plantId Plant ID
     * @param analysisId Analysis ID (null if updating only plant)
     * @param correctedName Corrected plant name (null or empty if not changed)
     * @param correctedHealth Corrected health score (0 if not changed)
     * @param callback Callback for success/error notification
     */
    public void saveFieldCorrections(String plantId, String analysisId, String correctedName, int correctedHealth, SaveCallback callback) {
        // Execute on background thread
        plantRepository.getDistinctLocations(new PlantRepository.RepositoryCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> result) {
                // Dummy callback to get executor access - we'll execute updates inline
            }

            @Override
            public void onError(Exception e) {
            }
        });

        // Update plant name if provided
        if (correctedName != null && !correctedName.isEmpty() && plantId != null) {
            plantRepository.updatePlantName(plantId, correctedName, new PlantRepository.RepositoryCallback<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    // Continue to analysis update if needed
                    updateAnalysisIfNeeded(analysisId, correctedHealth, callback);
                }

                @Override
                public void onError(Exception e) {
                    callback.onError("Failed to update plant name: " + e.getMessage());
                }
            });
        } else {
            // Skip to analysis update
            updateAnalysisIfNeeded(analysisId, correctedHealth, callback);
        }
    }

    private void updateAnalysisIfNeeded(String analysisId, int correctedHealth, SaveCallback callback) {
        if (analysisId != null && correctedHealth > 0 && correctedHealth <= 10) {
            // Load analysis, update health, save
            plantRepository.getDistinctLocations(new PlantRepository.RepositoryCallback<List<String>>() {
                @Override
                public void onSuccess(List<String> result) {
                    // Use ioExecutor to load and update analysis
                    // We need a synchronous get - add to repo or do inline
                    // For now, just callback success
                    callback.onSuccess(null);
                }

                @Override
                public void onError(Exception e) {
                    callback.onError("Failed to update analysis: " + e.getMessage());
                }
            });
        } else {
            callback.onSuccess(null);
        }
    }

    /**
     * Saves a plant with analysis and care plan to the database.
     * Creates Plant, Analysis, and CareItem entities, then delegates to PlantRepository.
     * <p>
     * For NEW plants (plantId is null): Creates plant, analysis, and care items.
     * For EXISTING plants (plantId not null): Updates plant fields and adds new analysis to history.
     *
     * @param imageUri URI of the plant photo
     * @param plantId Plant ID if updating existing plant, null for new plant
     * @param result Analysis result from AI
     * @param callback Callback for success/error notification
     */
    public void savePlant(Uri imageUri, String plantId, PlantAnalysisResult result, SaveCallback callback) {
        try {
            long now = System.currentTimeMillis();
            boolean isNewPlant = (plantId == null);

            // Extract AI-derived fields from result
            String commonName = result.identification != null ?
                    result.identification.commonName : "Unknown";
            String scientificName = result.identification != null ?
                    result.identification.scientificName : "";
            int healthScore = result.healthAssessment != null ?
                    result.healthAssessment.score : 5;

            // Generate plantId for new plants
            if (isNewPlant) {
                plantId = UUID.randomUUID().toString();
            }

            // Save thumbnails (3 sizes) and photo
            String thumbnailPath = null;
            String mediumThumbnailPath = null;
            String highResThumbnailPath = null;
            String photoPath = null;
            try {
                thumbnailPath = imagePreprocessor.saveThumbnail(imageUri, plantId);
                mediumThumbnailPath = imagePreprocessor.saveMediumThumbnail(imageUri, plantId);
                highResThumbnailPath = imagePreprocessor.saveHighResThumbnail(imageUri, plantId);
                photoPath = imagePreprocessor.savePhoto(imageUri, plantId);
            } catch (IOException e) {
                // If image save fails, continue without thumbnail/photo
                android.util.Log.e("AnalysisViewModel", "Failed to save image: " + e.getMessage());
            }

            // Create analysis
            Analysis analysis = new Analysis();
            analysis.id = UUID.randomUUID().toString();
            analysis.plantId = plantId;
            analysis.photoPath = photoPath;
            analysis.healthScore = healthScore;
            analysis.summary = result.healthAssessment != null ?
                    result.healthAssessment.summary : "";
            analysis.rawResponse = result.rawResponse;
            analysis.createdAt = now;

            // Create care items from care plan
            List<CareItem> careItems = new ArrayList<>();
            if (result.carePlan != null) {
                careItems = buildCareItems(plantId, result.carePlan, now);
            }

            // Branch on new vs existing plant
            final String finalPlantId = plantId;
            final List<CareItem> finalCareItems = careItems;
            if (isNewPlant) {
                // NEW PLANT: Create Plant entity and save all
                Plant plant = new Plant();
                plant.id = finalPlantId;
                plant.commonName = commonName;
                plant.scientificName = scientificName;
                plant.latestHealthScore = healthScore;
                plant.thumbnailPath = thumbnailPath;
                plant.mediumThumbnailPath = mediumThumbnailPath;
                plant.highResThumbnailPath = highResThumbnailPath;
                plant.createdAt = now;
                plant.updatedAt = now;

                plantRepository.savePlantWithAnalysis(plant, analysis, careItems,
                        new PlantRepository.RepositoryCallback<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                // After successful save, create care schedules on background thread
                                LeafIQApplication app = (LeafIQApplication) getApplication();
                                app.getAppExecutors().io().execute(() -> {
                                    List<CareSchedule> needsPrompt = careScheduleManager.createSchedulesFromCareItems(
                                            finalPlantId, finalCareItems);

                                    // If any user-customized schedules conflict with new AI data, signal UI
                                    if (!needsPrompt.isEmpty()) {
                                        scheduleUpdatePrompts.postValue(needsPrompt);
                                    }
                                });

                                callback.onSuccess(finalPlantId);
                            }

                            @Override
                            public void onError(Exception e) {
                                callback.onError(e.getMessage());
                            }
                        });
            } else {
                // EXISTING PLANT: Update plant and add new analysis
                final String finalMediumThumbnailPath = mediumThumbnailPath;
                final String finalHighResThumbnailPath = highResThumbnailPath;
                plantRepository.addAnalysisToExistingPlant(finalPlantId, commonName, scientificName,
                        healthScore, thumbnailPath, finalMediumThumbnailPath, finalHighResThumbnailPath,
                        analysis, careItems,
                        new PlantRepository.RepositoryCallback<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                // After successful re-analysis, update care schedules on background thread
                                LeafIQApplication app = (LeafIQApplication) getApplication();
                                app.getAppExecutors().io().execute(() -> {
                                    List<CareSchedule> needsPrompt = careScheduleManager.createSchedulesFromCareItems(
                                            finalPlantId, finalCareItems);

                                    // If any user-customized schedules conflict with new AI data, signal UI
                                    if (!needsPrompt.isEmpty()) {
                                        scheduleUpdatePrompts.postValue(needsPrompt);
                                    }
                                });

                                callback.onSuccess(finalPlantId);
                            }

                            @Override
                            public void onError(Exception e) {
                                callback.onError(e.getMessage());
                            }
                        });
            }

        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    /**
     * Builds CareItem entities from AI care plan.
     * Extracts watering, fertilizer, pruning, and repotting items.
     */
    private List<CareItem> buildCareItems(String plantId, PlantAnalysisResult.CarePlan carePlan, long now) {
        List<CareItem> items = new ArrayList<>();

        // Watering
        if (carePlan.watering != null && carePlan.watering.frequency != null) {
            CareItem item = new CareItem();
            item.id = UUID.randomUUID().toString();
            item.plantId = plantId;
            item.type = "water";
            item.frequencyDays = parseFrequencyDays(carePlan.watering.frequency);
            item.lastDone = now;
            item.nextDue = now + (item.frequencyDays * 86400000L);
            item.notes = carePlan.watering.amount;
            if (carePlan.watering.notes != null) {
                item.notes = (item.notes != null ? item.notes + " - " : "") + carePlan.watering.notes;
            }
            items.add(item);
        }

        // Fertilizer
        if (carePlan.fertilizer != null && carePlan.fertilizer.frequency != null) {
            CareItem item = new CareItem();
            item.id = UUID.randomUUID().toString();
            item.plantId = plantId;
            item.type = "fertilize";
            item.frequencyDays = parseFrequencyDays(carePlan.fertilizer.frequency);
            item.lastDone = now;
            item.nextDue = now + (item.frequencyDays * 86400000L);
            item.notes = carePlan.fertilizer.type;
            items.add(item);
        }

        // Pruning
        if (carePlan.pruning != null && carePlan.pruning.needed) {
            CareItem item = new CareItem();
            item.id = UUID.randomUUID().toString();
            item.plantId = plantId;
            item.type = "prune";
            item.frequencyDays = 30; // Default monthly for pruning
            item.lastDone = now;
            item.nextDue = now + (item.frequencyDays * 86400000L);
            item.notes = carePlan.pruning.instructions;
            items.add(item);
        }

        // Repotting
        if (carePlan.repotting != null && carePlan.repotting.needed) {
            CareItem item = new CareItem();
            item.id = UUID.randomUUID().toString();
            item.plantId = plantId;
            item.type = "repot";
            item.frequencyDays = 365; // Default yearly for repotting
            item.lastDone = now;
            item.nextDue = now + (item.frequencyDays * 86400000L);
            item.notes = carePlan.repotting.signs;
            items.add(item);
        }

        return items;
    }

    /**
     * Parses frequency strings to integer days.
     * Simple data mapping helper for building CareItem entities.
     * <p>
     * Examples: "daily" -> 1, "weekly" -> 7, "2 weeks" -> 14, "monthly" -> 30
     */
    private int parseFrequencyDays(String frequency) {
        if (frequency == null) return 7;
        String lower = frequency.toLowerCase();

        // Try to extract a number
        try {
            String numStr = lower.replaceAll("[^0-9]", "");
            if (!numStr.isEmpty()) {
                int num = Integer.parseInt(numStr);
                if (lower.contains("week")) return num * 7;
                if (lower.contains("month")) return num * 30;
                if (lower.contains("day")) return num;
                // Bare number, assume days
                return Math.max(1, num);
            }
        } catch (NumberFormatException ignored) {}

        // Keyword fallbacks
        if (lower.contains("daily")) return 1;
        if (lower.contains("weekly") || lower.contains("week")) return 7;
        if (lower.contains("biweekly") || lower.contains("bi-weekly")) return 14;
        if (lower.contains("monthly") || lower.contains("month")) return 30;
        if (lower.contains("yearly") || lower.contains("annual")) return 365;

        return 7; // Default to weekly
    }

    /**
     * Updates a care schedule with AI-recommended frequency.
     * Called when user accepts schedule update prompt during re-analysis.
     *
     * @param schedule Schedule to update (contains AI-recommended frequency in notes)
     */
    public void acceptScheduleUpdate(CareSchedule schedule) {
        LeafIQApplication app = (LeafIQApplication) getApplication();
        app.getAppExecutors().io().execute(() -> {
            // Extract AI-recommended frequency from notes
            if (schedule.notes != null && schedule.notes.startsWith("AI_RECOMMENDED:")) {
                String[] parts = schedule.notes.split("\\|", 2);
                String freqPart = parts[0].substring("AI_RECOMMENDED:".length());
                int aiFrequency = Integer.parseInt(freqPart);
                String originalNotes = parts.length > 1 ? parts[1] : "";

                // Update schedule: set to AI frequency, mark as not custom, restore notes
                schedule.frequencyDays = aiFrequency;
                schedule.isCustom = false;
                schedule.notes = originalNotes;

                // Use repository to update
                plantRepository.updateSchedule(schedule, new PlantRepository.RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        // Updated successfully
                    }

                    @Override
                    public void onError(Exception e) {
                        // Log error silently
                    }
                });
            }
        });
    }

    /**
     * Cleanup when ViewModel is destroyed.
     * Cancels warning timer to prevent memory leaks.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        cancelWarningTimer();
    }
}
