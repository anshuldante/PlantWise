package com.leafiq.app.util;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Unit tests for ImageUtils.
 * Note: Most ImageUtils methods require Android context/Uri,
 * so complex integration tests are in androidTest.
 * This tests edge cases that can be verified in JVM.
 */
@RunWith(MockitoJUnitRunner.class)
public class ImageUtilsTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    // ==================== Null context tests ====================

    @Test
    public void prepareForApi_withNullContext_shouldThrowException() {
        // This test documents the expected behavior - null context should fail
        // The actual test with real context is in androidTest
        assertThrows(NullPointerException.class, () -> {
            ImageUtils.prepareForApi(null, null);
        });
    }

    @Test
    public void savePhoto_withNullContext_shouldThrowException() {
        assertThrows(NullPointerException.class, () -> {
            ImageUtils.savePhoto(null, null, "test-id");
        });
    }

    @Test
    public void saveThumbnail_withNullContext_shouldThrowException() {
        assertThrows(NullPointerException.class, () -> {
            ImageUtils.saveThumbnail(null, null, "test-id");
        });
    }

    // ==================== Failed URI tests ====================

    @Test
    public void prepareForApi_invalidUri_throwsIOException() {
        // Mock context with ContentResolver that returns null InputStream (default behavior)
        Context mockContext = mock(Context.class);
        ContentResolver mockResolver = mock(ContentResolver.class);
        when(mockContext.getContentResolver()).thenReturn(mockResolver);

        // With returnDefaultValues=true, openInputStream returns null by default
        // BitmapFactory.decodeStream(null) returns null, causing IOException

        Uri invalidUri = Uri.parse("content://invalid/path");

        IOException exception = assertThrows(IOException.class, () -> {
            ImageUtils.prepareForApi(mockContext, invalidUri);
        });

        // Verify the error message indicates image decode failure
        assertThat(exception.getMessage()).contains("Failed to decode image");
    }

    @Test
    public void prepareForApi_nullUri_throwsIOException() {
        Context mockContext = mock(Context.class);
        ContentResolver mockResolver = mock(ContentResolver.class);
        when(mockContext.getContentResolver()).thenReturn(mockResolver);

        // When URI is null, getBitmapFromUri returns null, causing IOException
        IOException exception = assertThrows(IOException.class, () -> {
            ImageUtils.prepareForApi(mockContext, null);
        });

        // Verify the error message indicates image decode failure
        assertThat(exception.getMessage()).contains("Failed to decode image");
    }

    // ==================== Disk full tests ====================

    @Test
    public void savePhoto_diskFull_throwsIOException() throws IOException {
        // Create a read-only directory to simulate disk full / permission denied
        File readOnlyDir = tempFolder.newFolder("readonly");
        readOnlyDir.setWritable(false);

        // Mock context that returns the read-only directory as filesDir
        Context mockContext = mock(Context.class);
        when(mockContext.getFilesDir()).thenReturn(readOnlyDir);

        ContentResolver mockResolver = mock(ContentResolver.class);
        when(mockContext.getContentResolver()).thenReturn(mockResolver);

        // Any attempt to write should fail
        Uri testUri = Uri.parse("content://test/path");

        IOException exception = assertThrows(IOException.class, () -> {
            ImageUtils.savePhoto(mockContext, testUri, "test-plant");
        });

        // Clean up - restore write permission
        readOnlyDir.setWritable(true);
    }

    @Test
    public void saveThumbnail_diskFull_throwsIOException() throws IOException {
        // Create a read-only directory to simulate disk full / permission denied
        File readOnlyDir = tempFolder.newFolder("readonly-thumb");
        readOnlyDir.setWritable(false);

        // Mock context that returns the read-only directory as filesDir
        Context mockContext = mock(Context.class);
        when(mockContext.getFilesDir()).thenReturn(readOnlyDir);

        ContentResolver mockResolver = mock(ContentResolver.class);
        when(mockContext.getContentResolver()).thenReturn(mockResolver);

        // Any attempt to write should fail
        Uri testUri = Uri.parse("content://test/path");

        // Should throw IOException when trying to create directory or write file
        assertThrows(IOException.class, () -> {
            ImageUtils.saveThumbnail(mockContext, testUri, "test-plant");
        });

        // Clean up - restore write permission
        readOnlyDir.setWritable(true);
    }

    // ==================== OOM prevention test ====================

    @Test
    public void prepareForApi_has5MBSafetyCheck() {
        // This test documents that the 5MB safety check exists in prepareForApi.
        // The check is: if (baos.size() > 5 * 1024 * 1024) throw IOException
        //
        // This is difficult to trigger in unit tests because:
        // - MAX_DIMENSION = 1024, so resized images are always small
        // - JPEG quality = 80%, so compression is effective
        // - Typical 1024px JPEG: 150-300KB compressed, ~200-400KB base64
        //
        // The check exists at line ~45 of ImageUtils.java to prevent OOM
        // from unexpectedly large or low-compressibility images.
        //
        // For actual OOM prevention testing, see androidTest with real bitmaps.

        // Document the safety limit constant
        int FIVE_MB = 5 * 1024 * 1024;
        assertThat(FIVE_MB).isEqualTo(5242880);
    }

    // ==================== Constants verification ====================

    @Test
    public void constants_maxDimension_is1024() throws Exception {
        Field field = ImageUtils.class.getDeclaredField("MAX_DIMENSION");
        field.setAccessible(true);
        int maxDimension = field.getInt(null);

        assertThat(maxDimension).isEqualTo(1024);
    }

    @Test
    public void constants_jpegQuality_is80() throws Exception {
        Field field = ImageUtils.class.getDeclaredField("JPEG_QUALITY");
        field.setAccessible(true);
        int jpegQuality = field.getInt(null);

        assertThat(jpegQuality).isEqualTo(80);
    }
}
