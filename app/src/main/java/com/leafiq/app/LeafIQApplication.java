package com.leafiq.app;

import android.app.Application;

import com.leafiq.app.data.db.AppDatabase;
import com.leafiq.app.data.repository.PlantRepository;
import com.leafiq.app.util.AppExecutors;

/**
 * Application class for LeafIQ.
 * Initializes shared dependencies at app startup.
 * <p>
 * Provides:
 * - AppExecutors (thread pools for background work)
 * - PlantRepository (data access layer)
 * <p>
 * Activities and ViewModels can access these via:
 * <pre>
 * LeafIQApplication app = (LeafIQApplication) getApplication();
 * PlantRepository repo = app.getPlantRepository();
 * </pre>
 */
public class LeafIQApplication extends Application {

    private AppExecutors appExecutors;
    private PlantRepository plantRepository;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize thread pools
        appExecutors = new AppExecutors();

        // Initialize repository with database DAOs
        AppDatabase db = AppDatabase.getInstance(this);
        plantRepository = new PlantRepository(
                db.plantDao(),
                db.analysisDao(),
                db.careItemDao(),
                appExecutors.io()
        );
    }

    /**
     * Gets the application-wide AppExecutors instance.
     * Provides IO, network, and main thread executors.
     */
    public AppExecutors getAppExecutors() {
        return appExecutors;
    }

    /**
     * Gets the application-wide PlantRepository instance.
     * Used by ViewModels for data access.
     */
    public PlantRepository getPlantRepository() {
        return plantRepository;
    }
}
