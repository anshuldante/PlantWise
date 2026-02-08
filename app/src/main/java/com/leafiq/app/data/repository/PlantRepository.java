package com.leafiq.app.data.repository;

import androidx.lifecycle.LiveData;

import com.leafiq.app.data.db.AnalysisDao;
import com.leafiq.app.data.db.CareItemDao;
import com.leafiq.app.data.db.PlantDao;
import com.leafiq.app.data.entity.Analysis;
import com.leafiq.app.data.entity.CareItem;
import com.leafiq.app.data.entity.Plant;
import com.leafiq.app.data.model.AnalysisWithPlant;

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
     * @param ioExecutor Executor for background database operations
     */
    public PlantRepository(PlantDao plantDao, AnalysisDao analysisDao,
                          CareItemDao careItemDao, Executor ioExecutor) {
        this.plantDao = plantDao;
        this.analysisDao = analysisDao;
        this.careItemDao = careItemDao;
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
     * Synchronously gets an analysis by ID.
     * MUST be called from background thread.
     */
    public Analysis getAnalysisByIdSync(String analysisId) {
        return analysisDao.getAnalysisById(analysisId);
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
}
