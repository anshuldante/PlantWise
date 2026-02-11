package com.leafiq.app.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages photo tips display logic using SharedPreferences.
 * Controls when photo tips bottom sheet should appear:
 * - First-ever analysis (user has never seen tips)
 * - After quality failure (tips re-appear with contextual highlighting)
 */
public class PhotoTipsManager {

    private static final String PREFS_NAME = "photo_tips_prefs";
    private static final String KEY_HAS_SEEN_TIPS = "has_seen_tips";
    private static final String KEY_QUALITY_FAILURE_REASON = "quality_failure_reason";

    private final SharedPreferences prefs;

    /**
     * Constructor for production use.
     * @param context Application or Activity context
     */
    public PhotoTipsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Package-private constructor for testing.
     * Allows tests to inject mock SharedPreferences.
     * @param prefs SharedPreferences instance
     */
    PhotoTipsManager(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    /**
     * Returns true if tips should be shown (first time OR after quality failure).
     * @return true if tips bottom sheet should appear
     */
    public boolean shouldShowTips() {
        if (!prefs.getBoolean(KEY_HAS_SEEN_TIPS, false)) {
            return true; // First-ever analysis
        }
        return prefs.getString(KEY_QUALITY_FAILURE_REASON, null) != null;
    }

    /**
     * Mark that tips have been seen. Clears quality failure reason.
     * Only call this when user explicitly clicks "Got It" button.
     */
    public void markTipsSeen() {
        prefs.edit()
            .putBoolean(KEY_HAS_SEEN_TIPS, true)
            .remove(KEY_QUALITY_FAILURE_REASON)
            .apply();
    }

    /**
     * Record a quality failure reason to trigger contextual tips on next camera launch.
     * @param reason Quality failure type: "blur", "dark", "bright", or "resolution"
     */
    public void recordQualityFailure(String reason) {
        prefs.edit()
            .putString(KEY_QUALITY_FAILURE_REASON, reason)
            .apply();
    }

    /**
     * Get the quality failure reason (null if none).
     * @return Quality failure type or null
     */
    public String getQualityFailureReason() {
        return prefs.getString(KEY_QUALITY_FAILURE_REASON, null);
    }

    /**
     * Check if this is the very first time (never seen tips).
     * @return true if user has never seen tips
     */
    public boolean isFirstTime() {
        return !prefs.getBoolean(KEY_HAS_SEEN_TIPS, false);
    }
}
