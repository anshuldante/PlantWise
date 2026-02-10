package com.leafiq.app.data.repository;

import androidx.lifecycle.LiveData;

import com.leafiq.app.data.db.AnalysisDao;
import com.leafiq.app.data.db.CareCompletionDao;
import com.leafiq.app.data.db.CareItemDao;
import com.leafiq.app.data.db.CareScheduleDao;
import com.leafiq.app.data.db.PlantDao;
import com.leafiq.app.data.entity.Analysis;
import com.leafiq.app.data.entity.CareCompletion;
import com.leafiq.app.data.entity.CareItem;
import com.leafiq.app.data.entity.CareSchedule;
import com.leafiq.app.data.entity.Plant;
import com.leafiq.app.data.model.AnalysisWithPlant;
import com.leafiq.app.data.model.CareCompletionWithPlantInfo;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Repository layer that abstracts data access for Plant domain.
 * Wraps PlantDao, AnalysisDao, and CareItemDao.
 * <p>
 * LiveData read methods delegate directly to DAOs (Room handles threading automatically).
 * Write methods execute on injected IO executor with callback pattern for results.
 */
public class PlantRepository {

    private final PlantDao plantDao;
    private final AnalysisDao analysisDao;
    private final CareItemDao careItemDao;
    private final CareScheduleDao careScheduleDao;
    private final CareCompletionDao careCompletionDao;
    private final Executor ioExecutor;

    /**
     * Callback interface for asynchronous repository operations.
     *
     * @param <T> Result type (use Void for operations with no return value)
     */
    public interface RepositoryCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    /**
     * Creates a PlantRepository with all required dependencies.
     *
     * @param plantDao DAO for plant entities
     * @param analysisDao DAO for analysis entities
     * @param careItemDao DAO for care item entities
     * @param careScheduleDao DAO for care schedule entities
     * @param careCompletionDao DAO for care completion entities
     * @param ioExecutor Executor for background database operations
     */
    public PlantRepository(PlantDao plantDao, AnalysisDao analysisDao,
                          CareItemDao careItemDao, CareScheduleDao careScheduleDao,
                          CareCompletionDao careCompletionDao, Executor ioExecutor) {
        this.plantDao = plantDao;
        this.analysisDao = analysisDao;
        this.careItemDao = careItemDao;
        this.careScheduleDao = careScheduleDao;
        this.careCompletionDao = careCompletionDao;
        this.ioExecutor = ioExecutor;
    }

    // ==================== LiveData Read Methods ====================
    // Room automatically executes these queries on background threads

    /**
     * Gets all plants ordered by last update time.
     * LiveData updates automatically when database changes.
     */
    public LiveData<List<Plant>> getAllPlants() {
        return plantDao.getAllPlants();
    }

    /**
     * Gets a specific plant by ID.
     * LiveData updates automatically when plant changes.
     */
    public LiveData<Plant> getPlantById(String id) {
        return plantDao.getPlantById(id);
    }

    /**
     * Gets all analyses for a plant, ordered newest first.
     * LiveData updates automatically when analyses change.
     */
    public LiveData<List<Analysis>> getAnalysesForPlant(String plantId) {
        return analysisDao.getAnalysesForPlant(plantId);
    }

    /**
     * Gets all care items for a plant.
     * LiveData updates automatically when care items change.
     */
    public LiveData<List<CareItem>> getCareItemsForPlant(String plantId) {
        return careItemDao.getCareItemsForPlant(plantId);
    }

    /**
     * Gets all care items due before the specified timestamp.
     * LiveData updates automatically when care items change.
     *
     * @param timestamp Unix timestamp in milliseconds
     */
    public LiveData<List<CareItem>> getOverdueItems(long timestamp) {
        return careItemDao.getOverdueItems(timestamp);
    }

    /**
     * Gets all analyses with plant metadata (name, thumbnail, nickname).
     * Returns JOIN query result ordered by creation time (newest first).
     * LiveData updates automatically when analyses or plants change.
     * <p>
     * Used by timeline screen to show all plant activity across library.
     */
    public LiveData<List<AnalysisWithPlant>> getAllAnalysesWithPlant() {
        return analysisDao.getAllAnalysesWithPlant();
    }

    /**
     * Gets analyses with plant metadata for a specific plant.
     * Returns JOIN query result ordered by creation time (newest first).
     * LiveData updates automatically when analyses or plants change.
     * <p>
     * Used by plant detail screen to show individual plant history.
     *
     * @param plantId Plant ID to get analyses for
     */
    public LiveData<List<AnalysisWithPlant>> getAnalysesWithPlantForPlant(String plantId) {
        return analysisDao.getAnalysesWithPlantForPlant(plantId);
    }

    /**
     * Gets all care schedules for a plant.
     * LiveData updates automatically when schedules change.
     */
    public LiveData<List<CareSchedule>> getSchedulesForPlant(String plantId) {
        return careScheduleDao.getSchedulesForPlant(plantId);
    }

    /**
     * Gets enabled care schedules for a plant.
     * LiveData updates automatically when schedules change.
     */
    public LiveData<List<CareSchedule>> getEnabledSchedulesForPlant(String plantId) {
        return careScheduleDao.getEnabledSchedulesForPlant(plantId);
    }

    /**
     * Gets recent care completions for a plant.
     * LiveData updates automatically when completions change.
     *
     * @param plantId Plant ID to get completions for
     * @param limit Maximum number of completions to return
     */
    public LiveData<List<CareCompletion>> getRecentCompletionsForPlant(String plantId, int limit) {
        return careCompletionDao.getRecentCompletionsForPlant(plantId, limit);
    }

    // ==================== Synchronous Read Methods ====================
    // For use on background threads only (e.g., within UseCase executors)

    /**
     * Synchronously gets a plant by ID.
     * MUST be called from background thread.
     */
    public Plant getPlantByIdSync(String id) {
        return plantDao.getPlantByIdSync(id);
    }

    /**
     * Synchronously gets recent analyses for a plant.
     * MUST be called from background thread.
     */
    public List<Analysis> getRecentAnalysesSync(String plantId) {
        return analysisDao.getRecentAnalysesSync(plantId);
    }

    /**
     * Synchronously gets the latest analysis for a plant.
     * MUST be called from background thread.
     */
    public Analysis getLatestAnalysisSync(String plantId) {
        return analysisDao.getLatestForPlantSync(plantId);
    }

    /**
     * Synchronously gets an analysis by ID.
     * MUST be called from background thread.
     */
    public Analysis getAnalysisByIdSync(String analysisId) {
        return analysisDao.getAnalysisById(analysisId);
    }

    /**
     * Synchronously gets care schedules for a plant.
     * MUST be called from background thread.
     */
    public List<CareSchedule> getSchedulesByPlantIdSync(String plantId) {
        return careScheduleDao.getSchedulesByPlantIdSync(plantId);
    }

    /**
     * Synchronously gets due care schedules.
     * MUST be called from background thread.
     *
     * @param beforeTimestamp Returns schedules where nextDue <= beforeTimestamp
     */
    public List<CareSchedule> getDueSchedules(long beforeTimestamp) {
        return careScheduleDao.getDueSchedules(beforeTimestamp);
    }

    /**
     * Synchronously gets a care schedule by ID.
     * MUST be called from background thread.
     */
    public CareSchedule getScheduleByIdSync(String id) {
        return careScheduleDao.getScheduleById(id);
    }

    /**
     * Synchronously gets all enabled care schedules.
     * MUST be called from background thread.
     */
    public List<CareSchedule> getAllEnabledSchedulesSync() {
        return careScheduleDao.getAllEnabledSchedules();
    }

    /**
     * Synchronously gets the last completion for a schedule.
     * MUST be called from background thread.
     */
    public CareCompletion getLastCompletionForScheduleSync(String scheduleId) {
        return careCompletionDao.getLastCompletionForSchedule(scheduleId);
    }

    /**
     * Synchronously gets recent completions with plant information.
     * MUST be called from background thread.
     *
     * @param afterTimestamp Returns completions where completedAt >= afterTimestamp
     * @param limit Maximum number of completions to return
     */
    public List<CareCompletionWithPlantInfo> getRecentCompletionsSync(long afterTimestamp, int limit) {
        List<CareCompletionWithPlantInfo> completions = careCompletionDao.getRecentCompletions(afterTimestamp, limit);
        android.util.Log.i("CareSystem", "Loaded " + completions.size() + " recent completions");
        return completions;
    }

    // ==================== Write Methods ====================
    // All write operations execute on ioExecutor

    /**
     * Inserts a plant into the database.
     * Executes on background thread, result delivered via callback.
     *
     * @param plant Plant to insert
     * @param callback Callback for success/error
     */
    public void insertPlant(Plant plant, RepositoryCallback<Void> callback) {
        ioExecutor.execute(() -> {
            try {
                plantDao.insertPlant(plant);
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Inserts an analysis into the database.
     * Executes on background thread, result delivered via callback.
     *
     * @param analysis Analysis to insert
     * @param callback Callback for success/error
     */
    public void insertAnalysis(Analysis analysis, RepositoryCallback<Void> callback) {
        ioExecutor.execute(() -> {
            try {
                analysisDao.insertAnalysis(analysis);
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Inserts a care item into the database.
     * Executes on background thread, result delivered via callback.
     *
     * @param item Care item to insert
     * @param callback Callback for success/error
     */
    public void insertCareItem(CareItem item, RepositoryCallback<Void> callback) {
        ioExecutor.execute(() -> {
            try {
                careItemDao.insertCareItem(item);
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Saves a plant with its analysis and care items in a single transaction.
     * All entities are inserted sequentially on background thread.
     * Executes on background thread, result delivered via callback.
     *
     * @param plant Plant to insert
     * @param analysis Analysis to insert
     * @param careItems List of care items to insert
     * @param callback Callback for success/error
     */
    public void savePlantWithAnalysis(Plant plant, Analysis analysis,
                                     List<CareItem> careItems,
                                     RepositoryCallback<Void> callback) {
        ioExecutor.execute(() -> {
            try {
                // Insert plant first
                plantDao.insertPlant(plant);

                // Insert analysis (references plant)
                analysisDao.insertAnalysis(analysis);

                // Insert all care items (reference plant)
                for (CareItem item : careItems) {
                    careItemDao.insertCareItem(item);
                }

                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Adds a new analysis to an existing plant (re-analysis flow).
     * Updates the plant's AI-derived fields while preserving user-set fields.
     * Uses updatePlant (not insertPlant) to avoid CASCADE deletion of existing data.
     * <p>
     * This method is for re-analyzing existing plants. It:
     * - Reads the existing plant to preserve nickname, location, and createdAt
     * - Updates only AI-derived fields (commonName, scientificName, healthScore, thumbnail)
     * - Inserts the new analysis and care items
     * - Does NOT delete existing analyses or care items
     * <p>
     * Executes on background thread, result delivered via callback.
     *
     * @param plantId Plant ID of existing plant to update
     * @param commonName New common name from AI analysis
     * @param scientificName New scientific name from AI analysis
     * @param healthScore New health score from AI analysis
     * @param thumbnailPath New thumbnail path (if null, existing thumbnail is preserved)
     * @param mediumThumbnailPath New medium thumbnail path (if null, existing is preserved)
     * @param highResThumbnailPath New high-res thumbnail path (if null, existing is preserved)
     * @param analysis New Analysis to insert
     * @param careItems New CareItems to insert
     * @param callback Callback for success/error
     */
    public void addAnalysisToExistingPlant(String plantId, String commonName, String scientificName,
                                          int healthScore, String thumbnailPath,
                                          String mediumThumbnailPath, String highResThumbnailPath,
                                          Analysis analysis, List<CareItem> careItems,
                                          RepositoryCallback<Void> callback) {
        ioExecutor.execute(() -> {
            try {
                // Read existing plant
                Plant existingPlant = plantDao.getPlantByIdSync(plantId);
                if (existingPlant == null) {
                    callback.onError(new Exception("Plant not found: " + plantId));
                    return;
                }

                // Update only AI-derived fields
                existingPlant.commonName = commonName;
                existingPlant.scientificName = scientificName;
                existingPlant.latestHealthScore = healthScore;
                existingPlant.updatedAt = System.currentTimeMillis();

                // Update thumbnails only if new ones provided
                if (thumbnailPath != null) {
                    existingPlant.thumbnailPath = thumbnailPath;
                }
                if (mediumThumbnailPath != null) {
                    existingPlant.mediumThumbnailPath = mediumThumbnailPath;
                }
                if (highResThumbnailPath != null) {
                    existingPlant.highResThumbnailPath = highResThumbnailPath;
                }

                // PRESERVE user-set fields:
                // - nickname (already in existingPlant)
                // - location (already in existingPlant)
                // - createdAt (already in existingPlant)

                // Update plant (uses @Update, not @Insert REPLACE)
                plantDao.updatePlant(existingPlant);

                // Insert new analysis (adds to history)
                analysisDao.insertAnalysis(analysis);

                // Insert new care items
                for (CareItem item : careItems) {
                    careItemDao.insertCareItem(item);
                }

                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Updates a plant in the database.
     * Executes on background thread, result delivered via callback.
     *
     * @param plant Plant to update
     * @param callback Callback for success/error
     */
    public void updatePlant(Plant plant, RepositoryCallback<Void> callback) {
        ioExecutor.execute(() -> {
            try {
                plantDao.updatePlant(plant);
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Updates an analysis in the database.
     * Executes on background thread, result delivered via callback.
     *
     * @param analysis Analysis to update
     * @param callback Callback for success/error
     */
    public void updateAnalysis(Analysis analysis, RepositoryCallback<Void> callback) {
        ioExecutor.execute(() -> {
            try {
                analysisDao.updateAnalysis(analysis);
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Deletes an analysis from the database.
     * Executes on background thread, result delivered via callback.
     *
     * @param analysisId Analysis ID to delete
     * @param callback Callback for success/error
     */
    public void deleteAnalysis(String analysisId, RepositoryCallback<Void> callback) {
        ioExecutor.execute(() -> {
            try {
                analysisDao.deleteAnalysisById(analysisId);
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Updates a plant's common name.
     * Executes on background thread, result delivered via callback.
     *
     * @param plantId Plant ID
     * @param newName New common name
     * @param callback Callback for success/error
     */
    public void updatePlantName(String plantId, String newName, RepositoryCallback<Void> callback) {
        ioExecutor.execute(() -> {
            try {
                Plant plant = plantDao.getPlantByIdSync(plantId);
                if (plant != null) {
                    plant.commonName = newName;
                    plant.updatedAt = System.currentTimeMillis();
                    plantDao.updatePlant(plant);
                    callback.onSuccess(null);
                } else {
                    callback.onError(new Exception("Plant not found"));
                }
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Gets distinct plant locations from the database.
     * Executes on background thread, result delivered via callback.
     *
     * @param callback Callback for success/error
     */
    public void getDistinctLocations(RepositoryCallback<List<String>> callback) {
        ioExecutor.execute(() -> {
            try {
                List<String> locations = plantDao.getDistinctLocations();
                callback.onSuccess(locations);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Deletes a plant and all associated data (analyses, care items, photos).
     * Cleans up photo files from disk before deleting database records.
     * Database CASCADE handles deletion of analyses and care items.
     * Executes on background thread, result delivered via callback.
     *
     * @param plant Plant to delete
     * @param callback Callback for success/error
     */
    public void deletePlant(Plant plant, RepositoryCallback<Void> callback) {
        ioExecutor.execute(() -> {
            try {
                // Get all analysis photo paths for cleanup
                List<String> photoPaths = analysisDao.getPhotoPathsForPlantSync(plant.id);

                // Delete analysis photo files
                for (String photoPath : photoPaths) {
                    if (photoPath != null && !photoPath.isEmpty()) {
                        java.io.File photoFile = new java.io.File(photoPath);
                        if (photoFile.exists()) {
                            photoFile.delete();
                        }
                    }
                }

                // Delete plant thumbnail file
                if (plant.thumbnailPath != null && !plant.thumbnailPath.isEmpty()) {
                    java.io.File thumbnailFile = new java.io.File(plant.thumbnailPath);
                    if (thumbnailFile.exists()) {
                        thumbnailFile.delete();
                    }
                }

                // Delete plant from database (CASCADE deletes analyses and care items)
                plantDao.deletePlant(plant);

                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Inserts a care schedule into the database.
     * Executes on background thread, result delivered via callback.
     *
     * @param schedule Care schedule to insert
     * @param callback Callback for success/error
     */
    public void insertSchedule(CareSchedule schedule, RepositoryCallback<Void> callback) {
        ioExecutor.execute(() -> {
            try {
                careScheduleDao.insertSchedule(schedule);
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Updates a care schedule in the database.
     * Executes on background thread, result delivered via callback.
     *
     * @param schedule Care schedule to update
     * @param callback Callback for success/error
     */
    public void updateSchedule(CareSchedule schedule, RepositoryCallback<Void> callback) {
        ioExecutor.execute(() -> {
            try {
                careScheduleDao.updateSchedule(schedule);
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Deletes a care schedule from the database.
     * Executes on background thread, result delivered via callback.
     *
     * @param schedule Care schedule to delete
     * @param callback Callback for success/error
     */
    public void deleteSchedule(CareSchedule schedule, RepositoryCallback<Void> callback) {
        ioExecutor.execute(() -> {
            try {
                careScheduleDao.deleteSchedule(schedule);
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Inserts a care completion into the database.
     * Executes on background thread, result delivered via callback.
     *
     * @param completion Care completion to insert
     * @param callback Callback for success/error
     */
    public void insertCompletion(CareCompletion completion, RepositoryCallback<Void> callback) {
        ioExecutor.execute(() -> {
            try {
                careCompletionDao.insertCompletion(completion);
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Marks a care task as complete.
     * Creates a CareCompletion record, resets snooze count, and updates next due date.
     * Executes on background thread, result delivered via callback.
     *
     * @param scheduleId Care schedule ID
     * @param source Completion source ("notification_action", "in_app", "snooze")
     * @param callback Callback for success/error
     */
    public void markCareComplete(String scheduleId, String source, RepositoryCallback<Void> callback) {
        ioExecutor.execute(() -> {
            try {
                // Get the schedule
                CareSchedule schedule = careScheduleDao.getScheduleById(scheduleId);
                if (schedule == null) {
                    callback.onError(new Exception("Schedule not found: " + scheduleId));
                    return;
                }

                // Create completion record
                CareCompletion completion = new CareCompletion();
                completion.id = java.util.UUID.randomUUID().toString();
                completion.scheduleId = scheduleId;
                completion.completedAt = System.currentTimeMillis();
                completion.source = source;
                careCompletionDao.insertCompletion(completion);

                // Update schedule: reset snooze count and update next due
                schedule.snoozeCount = 0;
                schedule.nextDue = System.currentTimeMillis() + (schedule.frequencyDays * 24L * 60 * 60 * 1000);
                careScheduleDao.updateSchedule(schedule);

                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Deletes all care schedules for a plant.
     * Executes on background thread, result delivered via callback.
     *
     * @param plantId Plant ID
     * @param callback Callback for success/error
     */
    public void deleteSchedulesForPlant(String plantId, RepositoryCallback<Void> callback) {
        ioExecutor.execute(() -> {
            try {
                careScheduleDao.deleteSchedulesForPlant(plantId);
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
}
