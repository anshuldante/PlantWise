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
 * Validates brightness, blur, and resolution to ensure good analysis results.
 *
 * Enhanced with two-tier rejection (borderline vs egregious) and Quick Diagnosis mode.
 * Borderline failures allow user override with clear warnings.
 * Egregious failures (extremely blurry/dark photos) do not allow override.
 */
public class PhotoQualityChecker {

    private static final String TAG = "QualityCheck";

    // Standard thresholds (borderline - override allowed)
    // MIN_BLUR_SCORE lowered from 80 to 45 to accept reasonable plant photos.
    // Value of 45 chosen based on Laplacian variance characteristics:
    // - Most clear plant photos score 50-150
    // - Slightly soft focus acceptable for AI identification (40-50 range)
    // - Below 20 is severely blurred and unidentifiable
    static final float MIN_BLUR_SCORE = 45f;
    static final float MIN_BRIGHTNESS = 0.15f;
    static final float MAX_BRIGHTNESS = 0.95f;

    // Quick Diagnosis mode: More lenient threshold (~60% of standard)
    // Quick mode prioritizes speed over perfect clarity
    static final float QUICK_DIAGNOSIS_BLUR_SCORE = 30f;
    static final float QUICK_DIAGNOSIS_MIN_BRIGHTNESS = 0.12f;
    static final float QUICK_DIAGNOSIS_MAX_BRIGHTNESS = 0.97f;

    // Egregious thresholds (no override allowed)
    // These represent photos that are completely unusable for AI analysis
    static final float EGREGIOUS_BLUR_SCORE = 15f;
    static final float EGREGIOUS_MIN_BRIGHTNESS = 0.05f;
    static final float EGREGIOUS_MAX_BRIGHTNESS = 0.98f;

    static final int MIN_RESOLUTION = 480;
    static final int QUALITY_CHECK_MAX_SIZE = 800;

    /**
     * Checks photo quality on multiple dimensions using standard thresholds.
     * @param contentResolver ContentResolver to access image
     * @param imageUri URI of the image to check
     * @return QualityResult with pass/fail and specific feedback
     */
    public static QualityResult checkQuality(ContentResolver contentResolver, Uri imageUri) {
        return checkQuality(contentResolver, imageUri, false);
    }

    /**
     * Checks photo quality on multiple dimensions.
     * @param contentResolver ContentResolver to access image
     * @param imageUri URI of the image to check
     * @param isQuickDiagnosis If true, uses more lenient thresholds for Quick Diagnosis mode
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
                    return QualityResult.fail("Could not open image", "access", false, "egregious", 0f, 0f);
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
                    false, // Resolution failures are egregious (no override)
                    "egregious",
                    0f, // No blur score for resolution failures
                    0f  // No brightness score for resolution failures
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
                    return QualityResult.fail("Could not open image", "access", false, "egregious", 0f, 0f);
                }
                bitmap = BitmapFactory.decodeStream(imageStream, null, decodeOptions);
                if (bitmap == null) {
                    return QualityResult.fail("Could not decode image", "decode", false, "egregious", 0f, 0f);
                }
            }

            try {
                // Step 4: Check brightness
                float brightness = calculateBrightness(bitmap);

                // Select thresholds based on mode
                float minBrightness = isQuickDiagnosis ? QUICK_DIAGNOSIS_MIN_BRIGHTNESS : MIN_BRIGHTNESS;
                float maxBrightness = isQuickDiagnosis ? QUICK_DIAGNOSIS_MAX_BRIGHTNESS : MAX_BRIGHTNESS;

                // Check for egregious brightness issues first
                boolean egregiouslyDark = brightness < EGREGIOUS_MIN_BRIGHTNESS;
                boolean egregiouslyBright = brightness > EGREGIOUS_MAX_BRIGHTNESS;

                if (egregiouslyDark) {
                    Log.i(TAG, String.format("Quality check: blur=N/A brightness=%.2f resolution=%dx%d passed=false override=false (egregious dark)",
                            brightness, width, height));
                    return QualityResult.egregiousFail(
                        "Photo is extremely dark and unusable. Take photo in better light.",
                        "dark",
                        0f, // Blur not checked yet
                        brightness
                    );
                }

                if (egregiouslyBright) {
                    Log.i(TAG, String.format("Quality check: blur=N/A brightness=%.2f resolution=%dx%d passed=false override=false (egregious bright)",
                            brightness, width, height));
                    return QualityResult.egregiousFail(
                        "Photo is completely washed out. Reduce lighting or avoid direct light.",
                        "bright",
                        0f, // Blur not checked yet
                        brightness
                    );
                }

                // Check for borderline brightness issues
                if (brightness < minBrightness) {
                    // Will check blur before failing
                }
                if (brightness > maxBrightness) {
                    // Will check blur before failing
                }

                // Step 5: Check blur
                float blurScore = calculateBlurScore(bitmap);
                float minBlur = isQuickDiagnosis ? QUICK_DIAGNOSIS_BLUR_SCORE : MIN_BLUR_SCORE;

                // Check for egregious blur first
                if (blurScore < EGREGIOUS_BLUR_SCORE) {
                    Log.i(TAG, String.format("Quality check: blur=%.1f brightness=%.2f resolution=%dx%d passed=false override=false (egregious blur)",
                            blurScore, brightness, width, height));
                    return QualityResult.egregiousFail(
                        "Photo is extremely blurry and unusable. Hold camera steady and focus on the plant.",
                        "blur",
                        blurScore,
                        brightness
                    );
                }

                // Now check borderline thresholds
                boolean brightnessPass = brightness >= minBrightness && brightness <= maxBrightness;
                boolean blurPass = blurScore >= minBlur;

                if (!brightnessPass || !blurPass) {
                    // Borderline failure - override allowed
                    String message;
                    String issueType;

                    if (!blurPass) {
                        message = "Photo appears blurry. Hold the camera steady and focus on the plant.";
                        issueType = "blur";
                    } else if (brightness < minBrightness) {
                        message = "Photo is too dark. Try taking it in better light.";
                        issueType = "dark";
                    } else {
                        message = "Photo is overexposed. Reduce lighting or avoid direct light.";
                        issueType = "bright";
                    }

                    Log.i(TAG, String.format("Quality check: blur=%.1f brightness=%.2f resolution=%dx%d passed=false override=true issue=%s",
                            blurScore, brightness, width, height, issueType));

                    return QualityResult.fail(message, issueType, true, "borderline", blurScore, brightness);
                }

                // All checks passed
                Log.i(TAG, String.format("Quality check: blur=%.1f brightness=%.2f resolution=%dx%d passed=true override=false",
                        blurScore, brightness, width, height));

                return QualityResult.ok(blurScore, brightness);

            } finally {
                bitmap.recycle();
            }

        } catch (Exception e) {
            // If quality check itself fails, return a generic error
            Log.e(TAG, "Quality check exception", e);
            return QualityResult.fail("Quality check failed: " + e.getMessage(), "error",
                    false, "egregious", 0f, 0f);
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

        // Sample every 10th pixel
        for (int y = 0; y < height; y += 10) {
            for (int x = 0; x < width; x += 10) {
                int pixel = bitmap.getPixel(x, y);
                // Calculate luminance: 0.299*R + 0.587*G + 0.114*B
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
     * Calculates blur score using Laplacian variance approximation.
     * Higher score = sharper image. Lower score = more blur.
     * Samples every 5th pixel for speed.
     * @return blur score (higher is sharper)
     */
    private static float calculateBlurScore(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        long sumOfSquares = 0;
        int count = 0;

        // Sample every 5th pixel, avoiding edges
        for (int y = 5; y < height - 5; y += 5) {
            for (int x = 5; x < width - 5; x += 5) {
                int center = bitmap.getPixel(x, y);
                int left = bitmap.getPixel(x - 1, y);
                int right = bitmap.getPixel(x + 1, y);
                int top = bitmap.getPixel(x, y - 1);
                int bottom = bitmap.getPixel(x, y + 1);

                int centerGray = getGrayscale(center);
                int leftGray = getGrayscale(left);
                int rightGray = getGrayscale(right);
                int topGray = getGrayscale(top);
                int bottomGray = getGrayscale(bottom);

                // Laplacian: 4*center - (left + right + top + bottom)
                int laplacian = 4 * centerGray - (leftGray + rightGray + topGray + bottomGray);
                sumOfSquares += laplacian * laplacian;
                count++;
            }
        }

        if (count == 0) return 0;

        // Return square root of variance as blur score
        double variance = sumOfSquares / (double) count;
        return (float) Math.sqrt(variance);
    }

    /**
     * Converts a pixel to grayscale value.
     * @param pixel ARGB pixel value
     * @return grayscale value (0-255)
     */
    private static int getGrayscale(int pixel) {
        return (int) (0.299 * Color.red(pixel) +
                     0.587 * Color.green(pixel) +
                     0.114 * Color.blue(pixel));
    }

    /**
     * Calculates appropriate sample size for downsampling.
     * Standard Android pattern for memory-efficient image loading.
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

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * Result of photo quality check.
     * Enhanced with override eligibility and actual quality scores for logging.
     */
    public static class QualityResult {
        public final boolean passed;
        public final String message;
        public final String issueType;
        public final boolean overrideAllowed;
        public final String issueSeverity; // "egregious" or "borderline" or null if passed
        public final float blurScore;
        public final float brightnessScore;

        private QualityResult(boolean passed, String message, String issueType,
                            boolean overrideAllowed, String issueSeverity,
                            float blurScore, float brightnessScore) {
            this.passed = passed;
            this.message = message;
            this.issueType = issueType;
            this.overrideAllowed = overrideAllowed;
            this.issueSeverity = issueSeverity;
            this.blurScore = blurScore;
            this.brightnessScore = brightnessScore;
        }

        /**
         * Creates a passing quality result.
         */
        public static QualityResult ok() {
            return new QualityResult(true, null, null, false, null, 0f, 0f);
        }

        /**
         * Creates a passing quality result with actual quality scores for logging.
         * @param blurScore Measured blur score
         * @param brightnessScore Measured brightness score
         */
        public static QualityResult ok(float blurScore, float brightnessScore) {
            return new QualityResult(true, null, null, false, null, blurScore, brightnessScore);
        }

        /**
         * Creates a failing quality result with specific feedback.
         * Backward compatible - assumes borderline failure with override allowed.
         * @param message User-facing error message
         * @param issueType Type of issue: "dark", "bright", "blur", "resolution"
         */
        public static QualityResult fail(String message, String issueType) {
            return new QualityResult(false, message, issueType, true, "borderline", 0f, 0f);
        }

        /**
         * Creates a failing quality result with override eligibility.
         * @param message User-facing error message
         * @param issueType Type of issue: "dark", "bright", "blur", "resolution"
         * @param overrideAllowed True for borderline failures, false for egregious
         * @param issueSeverity "borderline" or "egregious"
         * @param blurScore Measured blur score
         * @param brightnessScore Measured brightness score
         */
        public static QualityResult fail(String message, String issueType,
                                        boolean overrideAllowed, String issueSeverity,
                                        float blurScore, float brightnessScore) {
            return new QualityResult(false, message, issueType, overrideAllowed, issueSeverity,
                    blurScore, brightnessScore);
        }

        /**
         * Creates an egregious failure result (no override allowed).
         * @param message User-facing error message
         * @param issueType Type of issue: "dark", "bright", "blur", "resolution"
         * @param blurScore Measured blur score
         * @param brightnessScore Measured brightness score
         */
        public static QualityResult egregiousFail(String message, String issueType,
                                                 float blurScore, float brightnessScore) {
            return new QualityResult(false, message, issueType, false, "egregious",
                    blurScore, brightnessScore);
        }
    }
}
