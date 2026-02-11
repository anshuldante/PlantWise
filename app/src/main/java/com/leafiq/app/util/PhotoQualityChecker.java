package com.leafiq.app.util;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;

import java.io.InputStream;

/**
 * Utility class for checking photo quality before AI analysis.
 * Validates brightness and resolution to ensure good analysis results.
 *
 * Two-tier rejection (borderline vs egregious) with Quick Diagnosis mode.
 * Borderline failures allow user override with clear warnings.
 * Egregious failures (extremely dark/bright photos) do not allow override.
 *
 * Note: Blur detection was removed — Laplacian variance is unreliable on
 * downsampled images and AI providers handle moderate blur gracefully.
 */
public class PhotoQualityChecker {

    private static final String TAG = "QualityCheck";

    // Brightness thresholds (borderline - override allowed)
    static final float MIN_BRIGHTNESS = 0.15f;
    static final float MAX_BRIGHTNESS = 0.95f;

    // Quick Diagnosis mode: More lenient brightness thresholds
    static final float QUICK_DIAGNOSIS_MIN_BRIGHTNESS = 0.12f;
    static final float QUICK_DIAGNOSIS_MAX_BRIGHTNESS = 0.97f;

    // Egregious brightness thresholds (no override allowed)
    static final float EGREGIOUS_MIN_BRIGHTNESS = 0.05f;
    static final float EGREGIOUS_MAX_BRIGHTNESS = 0.98f;

    static final int MIN_RESOLUTION = 480;
    static final int QUALITY_CHECK_MAX_SIZE = 800;

    /**
     * Checks photo quality using standard thresholds.
     * @param contentResolver ContentResolver to access image
     * @param imageUri URI of the image to check
     * @return QualityResult with pass/fail and specific feedback
     */
    public static QualityResult checkQuality(ContentResolver contentResolver, Uri imageUri) {
        return checkQuality(contentResolver, imageUri, false);
    }

    /**
     * Checks photo quality on brightness and resolution.
     * @param contentResolver ContentResolver to access image
     * @param imageUri URI of the image to check
     * @param isQuickDiagnosis If true, uses more lenient thresholds
     * @return QualityResult with pass/fail, specific feedback, and override eligibility
     */
    public static QualityResult checkQuality(ContentResolver contentResolver, Uri imageUri,
                                            boolean isQuickDiagnosis) {
        try {
            // Step 1: Check resolution using bounds-only decode
            BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
            boundsOptions.inJustDecodeBounds = true;

            try (InputStream boundsStream = contentResolver.openInputStream(imageUri)) {
                if (boundsStream == null) {
                    return QualityResult.fail("Could not open image", "access", false, "egregious", 0f);
                }
                BitmapFactory.decodeStream(boundsStream, null, boundsOptions);
            }

            int width = boundsOptions.outWidth;
            int height = boundsOptions.outHeight;

            if (width < MIN_RESOLUTION || height < MIN_RESOLUTION) {
                return QualityResult.fail(
                    "Image resolution is too low (" + width + "x" + height +
                    "). Use a higher quality photo.",
                    "resolution",
                    false,
                    "egregious",
                    0f
                );
            }

            // Step 2: Calculate sample size for downsampled quality check
            int inSampleSize = calculateInSampleSize(boundsOptions,
                QUALITY_CHECK_MAX_SIZE, QUALITY_CHECK_MAX_SIZE);

            // Step 3: Decode downsampled bitmap
            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inSampleSize = inSampleSize;

            Bitmap bitmap;
            try (InputStream imageStream = contentResolver.openInputStream(imageUri)) {
                if (imageStream == null) {
                    return QualityResult.fail("Could not open image", "access", false, "egregious", 0f);
                }
                bitmap = BitmapFactory.decodeStream(imageStream, null, decodeOptions);
                if (bitmap == null) {
                    return QualityResult.fail("Could not decode image", "decode", false, "egregious", 0f);
                }
            }

            try {
                // Step 4: Check brightness
                float brightness = calculateBrightness(bitmap);

                // Select thresholds based on mode
                float minBrightness = isQuickDiagnosis ? QUICK_DIAGNOSIS_MIN_BRIGHTNESS : MIN_BRIGHTNESS;
                float maxBrightness = isQuickDiagnosis ? QUICK_DIAGNOSIS_MAX_BRIGHTNESS : MAX_BRIGHTNESS;

                // Check for egregious brightness issues first
                if (brightness < EGREGIOUS_MIN_BRIGHTNESS) {
                    Log.i(TAG, String.format("Quality check: brightness=%.2f resolution=%dx%d passed=false override=false (egregious dark)",
                            brightness, width, height));
                    return QualityResult.egregiousFail(
                        "Photo is extremely dark and unusable. Take photo in better light.",
                        "dark",
                        brightness
                    );
                }

                if (brightness > EGREGIOUS_MAX_BRIGHTNESS) {
                    Log.i(TAG, String.format("Quality check: brightness=%.2f resolution=%dx%d passed=false override=false (egregious bright)",
                            brightness, width, height));
                    return QualityResult.egregiousFail(
                        "Photo is completely washed out. Reduce lighting or avoid direct light.",
                        "bright",
                        brightness
                    );
                }

                // Check borderline brightness thresholds
                boolean brightnessPass = brightness >= minBrightness && brightness <= maxBrightness;

                if (!brightnessPass) {
                    String message;
                    String issueType;

                    if (brightness < minBrightness) {
                        message = "Photo is too dark. Try taking it in better light.";
                        issueType = "dark";
                    } else {
                        message = "Photo is overexposed. Reduce lighting or avoid direct light.";
                        issueType = "bright";
                    }

                    Log.i(TAG, String.format("Quality check: brightness=%.2f resolution=%dx%d passed=false override=true issue=%s",
                            brightness, width, height, issueType));

                    return QualityResult.fail(message, issueType, true, "borderline", brightness);
                }

                // All checks passed
                Log.i(TAG, String.format("Quality check: brightness=%.2f resolution=%dx%d passed=true",
                        brightness, width, height));

                return QualityResult.ok(brightness);

            } finally {
                bitmap.recycle();
            }

        } catch (Exception e) {
            Log.e(TAG, "Quality check exception", e);
            return QualityResult.fail("Quality check failed: " + e.getMessage(), "error",
                    false, "egregious", 0f);
        }
    }

    /**
     * Calculates average brightness of the image.
     * Samples every 10th pixel for speed.
     * @return brightness value between 0 (black) and 1 (white)
     */
    private static float calculateBrightness(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        long sum = 0;
        int count = 0;

        for (int y = 0; y < height; y += 10) {
            for (int x = 0; x < width; x += 10) {
                int pixel = bitmap.getPixel(x, y);
                float luminance = 0.299f * Color.red(pixel) +
                                 0.587f * Color.green(pixel) +
                                 0.114f * Color.blue(pixel);
                sum += luminance;
                count++;
            }
        }

        return (count > 0) ? (sum / (float) count) / 255f : 0;
    }

    /**
     * Calculates appropriate sample size for downsampling.
     * @return power-of-2 sample size
     */
    private static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * Result of photo quality check.
     */
    public static class QualityResult {
        public final boolean passed;
        public final String message;
        public final String issueType;
        public final boolean overrideAllowed;
        public final String issueSeverity;
        public final float brightnessScore;

        private QualityResult(boolean passed, String message, String issueType,
                            boolean overrideAllowed, String issueSeverity,
                            float brightnessScore) {
            this.passed = passed;
            this.message = message;
            this.issueType = issueType;
            this.overrideAllowed = overrideAllowed;
            this.issueSeverity = issueSeverity;
            this.brightnessScore = brightnessScore;
        }

        public static QualityResult ok() {
            return new QualityResult(true, null, null, false, null, 0f);
        }

        public static QualityResult ok(float brightnessScore) {
            return new QualityResult(true, null, null, false, null, brightnessScore);
        }

        public static QualityResult fail(String message, String issueType) {
            return new QualityResult(false, message, issueType, true, "borderline", 0f);
        }

        public static QualityResult fail(String message, String issueType,
                                        boolean overrideAllowed, String issueSeverity,
                                        float brightnessScore) {
            return new QualityResult(false, message, issueType, overrideAllowed, issueSeverity,
                    brightnessScore);
        }

        public static QualityResult egregiousFail(String message, String issueType,
                                                 float brightnessScore) {
            return new QualityResult(false, message, issueType, false, "egregious",
                    brightnessScore);
        }
    }
}
