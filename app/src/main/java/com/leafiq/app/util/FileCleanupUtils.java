package com.leafiq.app.util;

import android.util.Log;

import java.io.File;
import java.io.FileFilter;

/**
 * Utility class for cleaning up orphaned temporary files.
 * <p>
 * Provides:
 * - Age-based file cleanup with logging
 * - Quiet single-file deletion for error paths
 * <p>
 * Used by:
 * - LeafIQApplication for startup sweep (files >1 hour old)
 * - AnalyzePlantUseCase for immediate cleanup on analysis error
 */
public class FileCleanupUtils {

    private static final String TAG = "FileCleanupUtils";
    public static final long ONE_HOUR_MS = 60 * 60 * 1000L;

    /**
     * Result of a cleanup operation.
     * Contains count and size of deleted files.
     */
    public static class CleanupResult {
        public final int filesDeleted;
        public final long bytesFreed;

        public CleanupResult(int filesDeleted, long bytesFreed) {
            this.filesDeleted = filesDeleted;
            this.bytesFreed = bytesFreed;
        }

        @Override
        public String toString() {
            return filesDeleted + " files (" + String.format("%.2f", bytesFreed / (1024.0 * 1024.0)) + " MB)";
        }
    }

    /**
     * Cleans up files in a directory that are older than a specified age.
     * <p>
     * Logs at INFO level if any files were deleted.
     * Handles null listFiles() result (permission errors).
     * Only counts successful deletions.
     *
     * @param directory Directory to clean
     * @param maxAgeMs Maximum age in milliseconds (files older than this are deleted)
     * @return CleanupResult with count and size of deleted files
     */
    public static CleanupResult cleanupOldFiles(File directory, long maxAgeMs) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return new CleanupResult(0, 0);
        }

        long cutoffTime = System.currentTimeMillis() - maxAgeMs;

        // FileFilter to match files (not directories) older than cutoff
        FileFilter filter = file -> file.isFile() && file.lastModified() < cutoffTime;

        File[] oldFiles = directory.listFiles(filter);

        // Handle null result (permission errors)
        if (oldFiles == null) {
            Log.w(TAG, "listFiles() returned null for " + directory.getPath() + " (permission error?)");
            return new CleanupResult(0, 0);
        }

        int deletedCount = 0;
        long bytesFreed = 0;

        for (File file : oldFiles) {
            long fileSize = file.length();
            if (file.delete()) {
                deletedCount++;
                bytesFreed += fileSize;
            } else {
                Log.w(TAG, "Failed to delete: " + file.getPath());
            }
        }

        if (deletedCount > 0) {
            Log.i(TAG, "Cleaned up " + deletedCount + " orphaned files (" +
                  String.format("%.2f", bytesFreed / (1024.0 * 1024.0)) + " MB freed)");
        }

        return new CleanupResult(deletedCount, bytesFreed);
    }

    /**
     * Deletes a file quietly without throwing exceptions.
     * Used in finally blocks and error paths where cleanup failure shouldn't mask original error.
     * Logs at DEBUG level if deletion fails.
     *
     * @param file File to delete (can be null)
     */
    public static void deleteFileQuietly(File file) {
        if (file == null || !file.exists()) {
            return;
        }

        try {
            if (!file.delete()) {
                Log.d(TAG, "Failed to delete file: " + file.getPath());
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception during quiet delete: " + e.getMessage());
        }
    }

    // Private constructor to prevent instantiation
    private FileCleanupUtils() {
    }
}
