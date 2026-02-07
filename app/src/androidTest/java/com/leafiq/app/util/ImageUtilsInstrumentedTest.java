package com.leafiq.app.util;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@RunWith(AndroidJUnit4.class)
public class ImageUtilsInstrumentedTest {

    private Context context;
    private Uri testImageUri;

    @Before
    public void setUp() throws IOException {
        context = ApplicationProvider.getApplicationContext();
        testImageUri = createTestImage();
    }

    private Uri createTestImage() throws IOException {
        // Create a simple test bitmap
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(0xFF00FF00); // Green color

        // Save to cache directory
        File cacheDir = context.getCacheDir();
        File testFile = new File(cacheDir, "test_image.jpg");
        try (FileOutputStream out = new FileOutputStream(testFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        }
        bitmap.recycle();

        return Uri.fromFile(testFile);
    }

    @Test
    public void prepareForApi_withValidUri_returnsBase64String() throws IOException {
        String base64 = ImageUtils.prepareForApi(context, testImageUri);

        assertThat(base64).isNotNull();
        assertThat(base64).isNotEmpty();
        // Base64 strings only contain these characters
        assertThat(base64).containsMatch("^[A-Za-z0-9+/=]+$");
    }

    @Test
    public void savePhoto_withValidUri_savesFile() throws IOException {
        String plantId = "test-plant-123";

        String savedPath = ImageUtils.savePhoto(context, testImageUri, plantId);

        assertThat(savedPath).isNotNull();
        File savedFile = new File(savedPath);
        assertThat(savedFile.exists()).isTrue();
        assertThat(savedFile.length()).isGreaterThan(0);

        // Cleanup
        savedFile.delete();
    }

    @Test
    public void saveThumbnail_withValidUri_savesFile() throws IOException {
        String plantId = "test-plant-456";

        String savedPath = ImageUtils.saveThumbnail(context, testImageUri, plantId);

        assertThat(savedPath).isNotNull();
        File savedFile = new File(savedPath);
        assertThat(savedFile.exists()).isTrue();
        assertThat(savedFile.length()).isGreaterThan(0);

        // Verify it's a valid image
        Bitmap thumbnail = BitmapFactory.decodeFile(savedPath);
        assertThat(thumbnail).isNotNull();
        // Thumbnail should be resized to max 256
        assertThat(thumbnail.getWidth()).isAtMost(256);
        assertThat(thumbnail.getHeight()).isAtMost(256);
        thumbnail.recycle();

        // Cleanup
        savedFile.delete();
    }

    @Test(expected = IOException.class)
    public void prepareForApi_withInvalidUri_throwsIOException() throws IOException {
        Uri invalidUri = Uri.parse("file:///nonexistent/path/image.jpg");
        ImageUtils.prepareForApi(context, invalidUri);
    }

    @Test(expected = IOException.class)
    public void savePhoto_withInvalidUri_throwsIOException() throws IOException {
        Uri invalidUri = Uri.parse("file:///nonexistent/path/image.jpg");
        ImageUtils.savePhoto(context, invalidUri, "test-id");
    }

    @Test(expected = IOException.class)
    public void saveThumbnail_withInvalidUri_throwsIOException() throws IOException {
        Uri invalidUri = Uri.parse("file:///nonexistent/path/image.jpg");
        ImageUtils.saveThumbnail(context, invalidUri, "test-id");
    }

    @Test
    public void savePhoto_createsDirectoryIfNotExists() throws IOException {
        // Delete the directory first if it exists
        File dir = new File(context.getFilesDir(), "plant_photos");
        if (dir.exists()) {
            for (File f : dir.listFiles()) {
                f.delete();
            }
            dir.delete();
        }

        String plantId = "new-plant";
        String savedPath = ImageUtils.savePhoto(context, testImageUri, plantId);

        assertThat(savedPath).isNotNull();
        assertThat(new File(savedPath).exists()).isTrue();

        // Cleanup
        new File(savedPath).delete();
    }

    @Test
    public void saveThumbnail_createsDirectoryIfNotExists() throws IOException {
        // Delete the directory first if it exists
        File dir = new File(context.getFilesDir(), "thumbnails");
        if (dir.exists()) {
            for (File f : dir.listFiles()) {
                f.delete();
            }
            dir.delete();
        }

        String plantId = "new-plant";
        String savedPath = ImageUtils.saveThumbnail(context, testImageUri, plantId);

        assertThat(savedPath).isNotNull();
        assertThat(new File(savedPath).exists()).isTrue();

        // Cleanup
        new File(savedPath).delete();
    }
}
