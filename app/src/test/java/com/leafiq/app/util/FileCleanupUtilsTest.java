package com.leafiq.app.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Unit tests for FileCleanupUtils.
 * Tests edge cases: null directory, non-existent directory, empty directory,
 * age-based filtering, byte tracking, subdirectory handling, and quiet deletion.
 */
public class FileCleanupUtilsTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private static final long ONE_HOUR_MS = FileCleanupUtils.ONE_HOUR_MS;

    // ==================== cleanupOldFiles tests ====================

    @Test
    public void cleanupOldFiles_nullDirectory_returnsZero() {
        FileCleanupUtils.CleanupResult result = FileCleanupUtils.cleanupOldFiles(null, ONE_HOUR_MS);

        assertThat(result.filesDeleted).isEqualTo(0);
        assertThat(result.bytesFreed).isEqualTo(0);
    }

    @Test
    public void cleanupOldFiles_nonExistentDirectory_returnsZero() {
        File nonExistent = new File("/nonexistent/directory/path");

        FileCleanupUtils.CleanupResult result = FileCleanupUtils.cleanupOldFiles(nonExistent, ONE_HOUR_MS);

        assertThat(result.filesDeleted).isEqualTo(0);
        assertThat(result.bytesFreed).isEqualTo(0);
    }

    @Test
    public void cleanupOldFiles_emptyDirectory_returnsZero() throws IOException {
        File emptyDir = tempFolder.newFolder("empty");

        FileCleanupUtils.CleanupResult result = FileCleanupUtils.cleanupOldFiles(emptyDir, ONE_HOUR_MS);

        assertThat(result.filesDeleted).isEqualTo(0);
        assertThat(result.bytesFreed).isEqualTo(0);
    }

    @Test
    public void cleanupOldFiles_allFilesNewer_deletesNone() throws IOException {
        File dir = tempFolder.newFolder("recent");

        // Create 3 files with current timestamp (recently created)
        File file1 = new File(dir, "file1.tmp");
        File file2 = new File(dir, "file2.tmp");
        File file3 = new File(dir, "file3.tmp");
        file1.createNewFile();
        file2.createNewFile();
        file3.createNewFile();

        FileCleanupUtils.CleanupResult result = FileCleanupUtils.cleanupOldFiles(dir, ONE_HOUR_MS);

        assertThat(result.filesDeleted).isEqualTo(0);
        assertThat(result.bytesFreed).isEqualTo(0);
        // All files still exist
        assertThat(file1.exists()).isTrue();
        assertThat(file2.exists()).isTrue();
        assertThat(file3.exists()).isTrue();
    }

    @Test
    public void cleanupOldFiles_allFilesOlder_deletesAll() throws IOException {
        File dir = tempFolder.newFolder("old");

        // Create 3 files and make them 2 hours old
        File file1 = createOldFile(dir, "file1.tmp", 2 * ONE_HOUR_MS);
        File file2 = createOldFile(dir, "file2.tmp", 2 * ONE_HOUR_MS);
        File file3 = createOldFile(dir, "file3.tmp", 2 * ONE_HOUR_MS);

        FileCleanupUtils.CleanupResult result = FileCleanupUtils.cleanupOldFiles(dir, ONE_HOUR_MS);

        assertThat(result.filesDeleted).isEqualTo(3);
        // No files should remain
        assertThat(file1.exists()).isFalse();
        assertThat(file2.exists()).isFalse();
        assertThat(file3.exists()).isFalse();
    }

    @Test
    public void cleanupOldFiles_mixedAges_deletesOnlyOld() throws IOException {
        File dir = tempFolder.newFolder("mixed");

        // Create 2 old files (2 hours old)
        File oldFile1 = createOldFile(dir, "old1.tmp", 2 * ONE_HOUR_MS);
        File oldFile2 = createOldFile(dir, "old2.tmp", 2 * ONE_HOUR_MS);

        // Create 1 new file (current timestamp)
        File newFile = new File(dir, "new.tmp");
        newFile.createNewFile();

        FileCleanupUtils.CleanupResult result = FileCleanupUtils.cleanupOldFiles(dir, ONE_HOUR_MS);

        assertThat(result.filesDeleted).isEqualTo(2);
        // Only old files deleted
        assertThat(oldFile1.exists()).isFalse();
        assertThat(oldFile2.exists()).isFalse();
        // New file remains
        assertThat(newFile.exists()).isTrue();
    }

    @Test
    public void cleanupOldFiles_tracksBytes_correctly() throws IOException {
        File dir = tempFolder.newFolder("bytes");

        // Create file with known content (11 bytes)
        File file = new File(dir, "content.tmp");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write("hello world".getBytes());
        }
        // Make it 2 hours old
        file.setLastModified(System.currentTimeMillis() - 2 * ONE_HOUR_MS);

        FileCleanupUtils.CleanupResult result = FileCleanupUtils.cleanupOldFiles(dir, ONE_HOUR_MS);

        assertThat(result.filesDeleted).isEqualTo(1);
        assertThat(result.bytesFreed).isEqualTo(11);
    }

    @Test
    public void cleanupOldFiles_skipsSubdirectories() throws IOException {
        File dir = tempFolder.newFolder("parent");

        // Create a subdirectory and make it old
        File subdir = new File(dir, "subdir");
        subdir.mkdir();
        subdir.setLastModified(System.currentTimeMillis() - 2 * ONE_HOUR_MS);

        FileCleanupUtils.CleanupResult result = FileCleanupUtils.cleanupOldFiles(dir, ONE_HOUR_MS);

        assertThat(result.filesDeleted).isEqualTo(0);
        // Subdirectory should still exist (directories are not files)
        assertThat(subdir.exists()).isTrue();
        assertThat(subdir.isDirectory()).isTrue();
    }

    // ==================== deleteFileQuietly tests ====================

    @Test
    public void deleteFileQuietly_existingFile_deletesIt() throws IOException {
        File file = tempFolder.newFile("to-delete.tmp");

        assertThat(file.exists()).isTrue();

        FileCleanupUtils.deleteFileQuietly(file);

        assertThat(file.exists()).isFalse();
    }

    @Test
    public void deleteFileQuietly_nullFile_noException() {
        // Should not throw any exception
        FileCleanupUtils.deleteFileQuietly(null);
    }

    @Test
    public void deleteFileQuietly_nonExistentFile_noException() {
        File nonExistent = new File("/nonexistent/file.tmp");

        // Should not throw any exception
        FileCleanupUtils.deleteFileQuietly(nonExistent);
    }

    // ==================== Helper methods ====================

    /**
     * Creates a file with specified age (by adjusting lastModified timestamp).
     */
    private File createOldFile(File dir, String name, long ageMs) throws IOException {
        File file = new File(dir, name);
        file.createNewFile();
        file.setLastModified(System.currentTimeMillis() - ageMs);
        return file;
    }
}
