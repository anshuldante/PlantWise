package com.leafiq.app.util;

import com.leafiq.app.R;

/**
 * Utility class for health score color and label mapping.
 * Centralizes health score thresholds and formatting used across the app.
 * <p>
 * Thresholds match existing logic in PlantCardAdapter and PlantDetailActivity:
 * - Score >= 7: Healthy (green)
 * - Score >= 4: Needs Attention (yellow/warning)
 * - Score < 4: Critical (red)
 * <p>
 * Health scores are on 0-10 scale (normalized from AI-provided 0-100 scores).
 */
public final class HealthUtils {

    private HealthUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Gets the color resource for a health score.
     * Uses same thresholds as existing PlantCardAdapter and PlantDetailActivity.
     *
     * @param score Health score (0-10 scale)
     * @return Color resource ID (R.color.health_good/health_warning/health_bad)
     */
    public static int getHealthColorRes(int score) {
        if (score >= 7) {
            return R.color.health_good;
        } else if (score >= 4) {
            return R.color.health_warning;
        } else {
            return R.color.health_bad;
        }
    }

    /**
     * Gets a text label for a health score.
     * Used in timeline collapsed rows and filter chips.
     *
     * @param score Health score (0-10 scale)
     * @return Health label string (Healthy/Needs Attention/Critical)
     */
    public static String getHealthLabel(int score) {
        if (score >= 7) {
            return "Healthy";
        } else if (score >= 4) {
            return "Needs Attention";
        } else {
            return "Critical";
        }
    }
}
