package com.leafiq.app.data.repository;

import androidx.lifecycle.LiveData;

import com.leafiq.app.data.db.AnalysisDao;
import com.leafiq.app.data.db.CareItemDao;
import com.leafiq.app.data.db.PlantDao;
import com.leafiq.app.data.entity.Analysis;
import com.leafiq.app.data.entity.CareItem;
import com.leafiq.app.data.entity.Plant;

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
}
