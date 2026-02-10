package com.leafiq.app;

import android.app.Application;

import com.leafiq.app.care.CareScheduleManager;
import com.leafiq.app.care.NotificationHelper;
import com.leafiq.app.data.db.AppDatabase;
import com.leafiq.app.data.repository.PlantRepository;
import com.leafiq.app.util.AppExecutors;
import com.leafiq.app.util.KeystoreHelper;

/**
 * Application class for LeafIQ.
 * Initializes shared dependencies at app startup.
 * <p>
 * Provides:
 * - AppExecutors (thread pools for background work)
 * - PlantRepository (data access layer)
 * - CareScheduleManager (care reminder scheduling)
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
    private CareScheduleManager careScheduleManager;

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
                db.careScheduleDao(),
                db.careCompletionDao(),
                appExecutors.io()
        );

        // Create notification channel for care reminders
        NotificationHelper.createNotificationChannel(this);
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

    /**
     * Gets the application-wide CareScheduleManager instance.
     * Lazy initialization on first access.
     * Used for care reminder scheduling operations.
     */
    public CareScheduleManager getCareScheduleManager() {
        if (careScheduleManager == null) {
            careScheduleManager = new CareScheduleManager(
                    this,
                    plantRepository,
                    new KeystoreHelper(this)
            );
        }
        return careScheduleManager;
    }
}
