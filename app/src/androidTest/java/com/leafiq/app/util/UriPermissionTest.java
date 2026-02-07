package com.leafiq.app.util;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Tests for URI permission handling.
 * These tests verify that images can be copied to local storage
 * and accessed later, even if the original URI permission expires.
 */
@RunWith(AndroidJUnit4.class)
public class UriPermissionTest {

    private Context context;
    private Uri testImageUri;

    @Before
    public void setUp() throws IOException {
        context = ApplicationProvider.getApplicationContext();
        testImageUri = createTestImage();
    }

    private Uri createTestImage() throws IOException {
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(0xFFFF0000); // Red color

        File cacheDir = context.getCacheDir();
        File testFile = new File(cacheDir, "uri_permission_test.jpg");
        try (FileOutputStream out = new FileOutputStream(testFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        }
        bitmap.recycle();

        return Uri.fromFile(testFile);
    }

    @Test
    public void copyImageToLocal_createsValidCopy() throws IOException {
        // Simulate the copy process that AnalysisActivity does
        File cacheDir = new File(context.getCacheDir(), "temp_images");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        String filename = "analysis_" + System.currentTimeMillis() + ".jpg";
        File localFile = new File(cacheDir, filename);

        try (InputStream in = context.getContentResolver().openInputStream(testImageUri);
             OutputStream out = new FileOutputStream(localFile)) {
            assertThat(in).isNotNull();
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }

        Uri localImageUri = Uri.fromFile(localFile);

        // Verify the local copy exists and is readable
        assertThat(localFile.exists()).isTrue();
        assertThat(localFile.length()).isGreaterThan(0);

        // Verify we can read from the local copy
        String base64 = ImageUtils.prepareForApi(context, localImageUri);
        assertThat(base64).isNotNull();
        assertThat(base64).isNotEmpty();

        // Cleanup
        localFile.delete();
    }

    @Test
    public void localCopy_canBeUsedForThumbnail() throws IOException {
        // Create local copy
        File cacheDir = new File(context.getCacheDir(), "temp_images");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        File localFile = new File(cacheDir, "local_copy.jpg");
        try (InputStream in = context.getContentResolver().openInputStream(testImageUri);
             OutputStream out = new FileOutputStream(localFile)) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }

        Uri localImageUri = Uri.fromFile(localFile);

        // Use local copy to create thumbnail
        String thumbnailPath = ImageUtils.saveThumbnail(context, localImageUri, "test-plant");

        assertThat(thumbnailPath).isNotNull();
        assertThat(new File(thumbnailPath).exists()).isTrue();

        // Cleanup
        localFile.delete();
        new File(thumbnailPath).delete();
    }

    @Test
    public void localCopy_canBeUsedForPhoto() throws IOException {
        // Create local copy
        File cacheDir = new File(context.getCacheDir(), "temp_images");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        File localFile = new File(cacheDir, "local_photo_copy.jpg");
        try (InputStream in = context.getContentResolver().openInputStream(testImageUri);
             OutputStream out = new FileOutputStream(localFile)) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }

        Uri localImageUri = Uri.fromFile(localFile);

        // Use local copy to save photo
        String photoPath = ImageUtils.savePhoto(context, localImageUri, "test-plant");

        assertThat(photoPath).isNotNull();
        assertThat(new File(photoPath).exists()).isTrue();

        // Cleanup
        localFile.delete();
        new File(photoPath).delete();
    }

    @Test
    public void localCopy_survivesOriginalDeletion() throws IOException {
        // Create local copy
        File cacheDir = new File(context.getCacheDir(), "temp_images");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        File localFile = new File(cacheDir, "survive_test.jpg");
        try (InputStream in = context.getContentResolver().openInputStream(testImageUri);
             OutputStream out = new FileOutputStream(localFile)) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }

        Uri localImageUri = Uri.fromFile(localFile);

        // Delete the original
        new File(testImageUri.getPath()).delete();

        // Local copy should still be accessible
        assertThat(localFile.exists()).isTrue();
        String base64 = ImageUtils.prepareForApi(context, localImageUri);
        assertThat(base64).isNotNull();

        // Cleanup
        localFile.delete();
    }

    @Test
    public void tempDirectory_canBeCreated() {
        File cacheDir = new File(context.getCacheDir(), "temp_images");

        if (cacheDir.exists()) {
            // Clean up first
            for (File f : cacheDir.listFiles()) {
                f.delete();
            }
            cacheDir.delete();
        }

        boolean created = cacheDir.mkdirs();

        assertThat(created || cacheDir.exists()).isTrue();
        assertThat(cacheDir.isDirectory()).isTrue();
        assertThat(cacheDir.canWrite()).isTrue();

        // Cleanup
        cacheDir.delete();
    }
}
