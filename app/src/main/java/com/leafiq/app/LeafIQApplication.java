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
import com.leafiq.app.util.FileCleanupUtils;
import com.leafiq.app.util.KeystoreHelper;
import com.leafiq.app.util.ParseScanHelper;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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
    private OkHttpClient httpClient;
    private PlantRepository plantRepository;
    private CareScheduleManager careScheduleManager;
    private boolean migrationFailed = false;
    private String migrationError;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize thread pools
        appExecutors = new AppExecutors();

        // Background cleanup sweep for orphaned temp files (runs regardless of DB state)
        appExecutors.io().execute(() -> {
            File thumbnailDir = new File(getFilesDir(), "thumbnails");
            FileCleanupUtils.cleanupOldFiles(thumbnailDir, FileCleanupUtils.ONE_HOUR_MS);
            File photoDir = new File(getFilesDir(), "plant_photos");
            FileCleanupUtils.cleanupOldFiles(photoDir, FileCleanupUtils.ONE_HOUR_MS);
        });

        // Initialize shared HTTP client for AI providers
        httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)  // Hard timeout for total request duration - OkHttp cancels the request properly
            .addInterceptor(createLoggingInterceptor())
            .build();

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

        // Background parse scan: incrementally classify existing analyses
        appExecutors.io().execute(() -> {
            try {
                ParseScanHelper.scanOnLaunch(db.analysisDao());
            } catch (Exception e) {
                Log.w("AnalysisParser", "Background parse scan failed: " + e.getMessage());
            }
        });

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
     * Gets the application-wide OkHttpClient instance.
     * Shared across all AI providers for connection reuse.
     * Configured with 90-second call timeout and request logging.
     */
    public OkHttpClient getHttpClient() {
        return httpClient;
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

    /**
     * Creates an HTTP logging interceptor that masks API keys for security.
     * Logs request provider, method, URL, and response code/duration.
     */
    private Interceptor createLoggingInterceptor() {
        return new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                long startTime = System.currentTimeMillis();

                // Extract provider from URL host
                String host = request.url().host();
                String provider = "Unknown";
                if (host.contains("generativelanguage.googleapis.com")) provider = "Gemini";
                else if (host.contains("anthropic.com")) provider = "Claude";
                else if (host.contains("openai.com")) provider = "OpenAI";

                // Mask API keys in URL (query parameter)
                String url = request.url().toString();
                String maskedUrl = url.replaceAll("(\\?|&)key=([^&]+)", "$1key=***REDACTED***");

                // Mask API keys in headers
                String authHeader = request.header("Authorization");
                String maskedAuth = authHeader != null ? "Bearer ***REDACTED***" : null;
                String apiKeyHeader = request.header("x-api-key");
                String maskedApiKey = apiKeyHeader != null ? "***REDACTED***" : null;

                // Log request
                Log.i("HttpClient", String.format("[%s] %s %s | Auth: %s | x-api-key: %s",
                        provider, request.method(), maskedUrl,
                        maskedAuth != null ? maskedAuth : "none",
                        maskedApiKey != null ? maskedApiKey : "none"));

                // Execute request
                Response response;
                try {
                    response = chain.proceed(request);
                    long duration = System.currentTimeMillis() - startTime;

                    // Log response
                    long contentLength = response.body() != null ? response.body().contentLength() : 0;
                    Log.i("HttpClient", String.format("[%s] Response: %d | Duration: %dms | Size: %d bytes",
                            provider, response.code(), duration, contentLength));

                    return response;
                } catch (IOException e) {
                    long duration = System.currentTimeMillis() - startTime;
                    Log.e("HttpClient", String.format("[%s] Request failed after %dms: %s",
                            provider, duration, e.getMessage()));
                    throw e;
                }
            }
        };
    }
}
