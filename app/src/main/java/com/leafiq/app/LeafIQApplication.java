package com.leafiq.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.util.Log;

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
    private boolean migrationFailed = false;
    private String migrationError;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize thread pools
        appExecutors = new AppExecutors();

        // Initialize repository with database DAOs
        AppDatabase db;
        try {
            db = AppDatabase.getInstance(this);
        } catch (IllegalStateException e) {
            Log.e("LeafIQApplication", "Database migration failed", e);
            // Store the error — activities will check and show dialog
            migrationFailed = true;
            migrationError = e.getMessage();
            return;  // Don't initialize repository since DB is unusable
        }

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

    /**
     * Checks if database migration failed during application startup.
     * Activities should check this in onCreate() and show error dialog if true.
     */
    public boolean isMigrationFailed() {
        return migrationFailed;
    }

    /**
     * Gets the migration error message if migration failed.
     */
    public String getMigrationError() {
        return migrationError;
    }

    /**
     * Shows migration error dialog and exits the app when closed.
     * Must be called from an Activity context.
     */
    public static void showMigrationErrorAndExit(Activity activity) {
        new AlertDialog.Builder(activity)
            .setTitle(R.string.migration_error_title)
            .setMessage(R.string.migration_error_message)
            .setPositiveButton(R.string.migration_error_close, (dialog, which) -> {
                activity.finishAffinity();
            })
            .setCancelable(false)
            .show();
    }
}
