package com.anshul.plantwise.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

/**
 * Unit tests for ImageUtils.
 * Note: Most ImageUtils methods require Android context/Uri,
 * so they are tested in androidTest. This tests what we can in JVM.
 */
@RunWith(MockitoJUnitRunner.class)
public class ImageUtilsTest {

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
}
