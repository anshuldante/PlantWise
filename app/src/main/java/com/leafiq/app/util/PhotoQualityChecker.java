package com.leafiq.app.util;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;

import java.io.InputStream;

/**
 * Utility class for checking photo quality before AI analysis.
 * Validates brightness, blur, and resolution to ensure good analysis results.
 */
public class PhotoQualityChecker {

    // Threshold constants
    private static final float MIN_BRIGHTNESS = 0.15f;
    private static final float MAX_BRIGHTNESS = 0.95f;
    private static final float MIN_BLUR_SCORE = 80f;
    private static final int MIN_RESOLUTION = 480;
    private static final int QUALITY_CHECK_MAX_SIZE = 800;

    /**
     * Checks photo quality on multiple dimensions.
     * @param contentResolver ContentResolver to access image
     * @param imageUri URI of the image to check
     * @return QualityResult with pass/fail and specific feedback
     */
    public static QualityResult checkQuality(ContentResolver contentResolver, Uri imageUri) {
        try {
            // Step 1: Check resolution using bounds-only decode
            BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
            boundsOptions.inJustDecodeBounds = true;

            try (InputStream boundsStream = contentResolver.openInputStream(imageUri)) {
                if (boundsStream == null) {
                    return QualityResult.fail("Could not open image", "access");
                }
                BitmapFactory.decodeStream(boundsStream, null, boundsOptions);
            }

            int width = boundsOptions.outWidth;
            int height = boundsOptions.outHeight;

            if (width < MIN_RESOLUTION || height < MIN_RESOLUTION) {
                return QualityResult.fail(
                    "Image resolution is too low (" + width + "x" + height +
                    "). Use a higher quality photo.",
                    "resolution"
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
                    return QualityResult.fail("Could not open image", "access");
                }
                bitmap = BitmapFactory.decodeStream(imageStream, null, decodeOptions);
                if (bitmap == null) {
                    return QualityResult.fail("Could not decode image", "decode");
                }
            }

            try {
                // Step 4: Check brightness
                float brightness = calculateBrightness(bitmap);
                if (brightness < MIN_BRIGHTNESS) {
                    return QualityResult.fail(
                        "Photo is too dark. Try taking it in better light.",
                        "dark"
                    );
                }
                if (brightness > MAX_BRIGHTNESS) {
                    return QualityResult.fail(
                        "Photo is overexposed. Reduce lighting or avoid direct light.",
                        "bright"
                    );
                }

                // Step 5: Check blur
                float blurScore = calculateBlurScore(bitmap);
                if (blurScore < MIN_BLUR_SCORE) {
                    return QualityResult.fail(
                        "Photo appears blurry. Hold the camera steady and focus on the plant.",
                        "blur"
                    );
                }

                // All checks passed
                return QualityResult.ok();

            } finally {
                bitmap.recycle();
            }

        } catch (Exception e) {
            // If quality check itself fails, return a generic error
            return QualityResult.fail("Quality check failed: " + e.getMessage(), "error");
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
     */
    public static class QualityResult {
        public final boolean passed;
        public final String message;
        public final String issueType;

        private QualityResult(boolean passed, String message, String issueType) {
            this.passed = passed;
            this.message = message;
            this.issueType = issueType;
        }

        /**
         * Creates a passing quality result.
         */
        public static QualityResult ok() {
            return new QualityResult(true, null, null);
        }

        /**
         * Creates a failing quality result with specific feedback.
         * @param message User-facing error message
         * @param issueType Type of issue: "dark", "bright", "blur", "resolution"
         */
        public static QualityResult fail(String message, String issueType) {
            return new QualityResult(false, message, issueType);
        }
    }
}
