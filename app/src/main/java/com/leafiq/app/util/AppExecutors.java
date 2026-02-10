package com.leafiq.app.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Thread pool holder providing executors for different types of background work.
 * <p>
 * Usage:
 * - IO executor: Database operations (Room DAOs)
 * - Network executor: AI API calls, HTTP requests
 * - Main thread executor: Posting results back to UI
 */
public class AppExecutors {

    private final Executor ioExecutor;
    private final Executor networkExecutor;
    private final Executor mainThreadExecutor;

    /**
     * Creates executor pools for background work.
     * Thread pool sizes are optimized for the device CPU count.
     */
    public AppExecutors() {
        // Fixed thread pool for database operations
        // Size: 2-4 threads based on CPU count (conservative for sequential DB writes)
        int cpuCount = Runtime.getRuntime().availableProcessors();
        int ioPoolSize = Math.max(2, Math.min(cpuCount - 1, 4));
        this.ioExecutor = Executors.newFixedThreadPool(ioPoolSize);

        // Cached thread pool for network requests
        // Creates threads as needed, reuses idle threads
        this.networkExecutor = Executors.newCachedThreadPool();

        // Main thread executor for posting results to UI
        this.mainThreadExecutor = new MainThreadExecutor();
    }

    /**
     * Executor for database operations (Room DAOs).
     * Uses a fixed thread pool (2-4 threads).
     */
    public Executor io() {
        return ioExecutor;
    }

    /**
     * Executor for network operations (AI API calls).
     * Uses a cached thread pool (creates threads as needed).
     */
    public Executor network() {
        return networkExecutor;
    }

    /**
     * Executor that posts tasks to the main UI thread.
     */
    public Executor mainThread() {
        return mainThreadExecutor;
    }

    /**
     * Executor implementation that runs tasks on the main thread via Handler.
     */
    private static class MainThreadExecutor implements Executor {
        private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable command) {
            mainThreadHandler.post(command);
        }
    }
}
