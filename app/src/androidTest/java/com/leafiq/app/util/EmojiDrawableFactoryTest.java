package com.leafiq.app.util;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented tests for EmojiDrawableFactory.
 *
 * Tests validate that emoji characters are correctly rendered into BitmapDrawables
 * with appropriate dimensions and scaling behavior.
 */
@RunWith(AndroidJUnit4.class)
public class EmojiDrawableFactoryTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void createEmojiDrawable_returnsNonNullDrawable() {
        BitmapDrawable result = EmojiDrawableFactory.createEmojiDrawable(context, "🪴", 24);

        assertThat(result).isNotNull();
        assertThat(result.getBitmap()).isNotNull();
    }

    @Test
    public void createEmojiDrawable_bitmapHasCorrectDimensions() {
        float density = context.getResources().getDisplayMetrics().density;
        int expectedPx = (int) (24 * density);

        BitmapDrawable result = EmojiDrawableFactory.createEmojiDrawable(context, "🪴", 24);

        assertThat(result.getBitmap().getWidth()).isEqualTo(expectedPx);
        assertThat(result.getBitmap().getHeight()).isEqualTo(expectedPx);
    }

    @Test
    public void createEmojiDrawable_largerSizeProducesLargerBitmap() {
        BitmapDrawable small = EmojiDrawableFactory.createEmojiDrawable(context, "🪴", 24);
        BitmapDrawable large = EmojiDrawableFactory.createEmojiDrawable(context, "🪴", 48);

        assertThat(large.getBitmap().getWidth()).isGreaterThan(small.getBitmap().getWidth());
        assertThat(large.getBitmap().getHeight()).isGreaterThan(small.getBitmap().getHeight());
    }

    @Test
    public void createEmojiDrawable_emptyStringDoesNotCrash() {
        BitmapDrawable result = EmojiDrawableFactory.createEmojiDrawable(context, "", 24);

        assertThat(result).isNotNull();
        assertThat(result.getBitmap()).isNotNull();
    }

    @Test
    public void createEmojiDrawable_pottedPlantEmoji() {
        // Potted plant emoji U+1FAB4 represented as surrogate pair
        BitmapDrawable result = EmojiDrawableFactory.createEmojiDrawable(context, "\uD83E\uDEB4", 24);

        assertThat(result).isNotNull();
        assertThat(result.getBitmap()).isNotNull();

        // Verify dimensions match the 24dp specification
        float density = context.getResources().getDisplayMetrics().density;
        int expectedPx = (int) (24 * density);
        assertThat(result.getBitmap().getWidth()).isEqualTo(expectedPx);
    }
}
