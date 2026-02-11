package com.leafiq.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class KeystoreHelper {

    private static final String TAG = "KeystoreHelper";
    private static final String PREFS_NAME = "leafiq_secure_prefs";
    private static final String KEY_API_KEY_GEMINI = "api_key_gemini";
    private static final String KEY_API_KEY_CLAUDE = "api_key_claude";
    private static final String KEY_API_KEY_OPENAI = "api_key_openai";
    // Legacy key for migration
    private static final String KEY_API_KEY_LEGACY = "api_key";
    private static final String KEY_PROVIDER = "ai_provider";
    private static final String KEY_PREFERRED_REMINDER_TIME = "preferred_reminder_time";
    private static final String KEY_REMINDERS_PAUSED = "reminders_paused";
    private static final String KEY_NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested";
    private static final String KEY_QUICK_DIAGNOSIS_TOOLTIP_SHOWN = "quick_diagnosis_tooltip_shown";
    private static final String KEY_NOTIFICATION_BANNER_DISMISSED = "notification_banner_dismissed";

    public static final String PROVIDER_OPENAI = "openai";
    public static final String PROVIDER_CLAUDE = "claude";
    public static final String PROVIDER_GEMINI = "gemini";

    private SharedPreferences prefs;
    private boolean encryptionHealthy = false;

    public KeystoreHelper(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

            SharedPreferences tempPrefs = EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            this.prefs = tempPrefs;
            this.encryptionHealthy = true;
            Log.i(TAG, "Encryption health check: PASSED");

        } catch (GeneralSecurityException e) {
            Log.e(TAG, "Encryption health check FAILED (GeneralSecurityException)", e);
            this.prefs = null;
            this.encryptionHealthy = false;
        } catch (IOException e) {
            Log.e(TAG, "Encryption health check FAILED (IOException)", e);
            this.prefs = null;
            this.encryptionHealthy = false;
        }
    }

    /**
     * Package-private test constructor for unit testing encryption failure behavior.
     * Allows setting prefs and encryptionHealthy fields directly without Android dependencies.
     */
    KeystoreHelper(SharedPreferences prefs, boolean encryptionHealthy) {
        this.prefs = prefs;
        this.encryptionHealthy = encryptionHealthy;
    }

    public boolean isEncryptionHealthy() {
        return encryptionHealthy;
    }

    private String getKeyForProvider(String provider) {
        switch (provider) {
            case PROVIDER_GEMINI: return KEY_API_KEY_GEMINI;
            case PROVIDER_CLAUDE: return KEY_API_KEY_CLAUDE;
            case PROVIDER_OPENAI: return KEY_API_KEY_OPENAI;
            default: return KEY_API_KEY_GEMINI;
        }
    }

    public void saveApiKey(String apiKey) {
        if (!encryptionHealthy || prefs == null) {
            Log.w(TAG, "Attempted to save API key with unhealthy encryption - blocked");
            throw new IllegalStateException("Secure storage unavailable. Cannot save API keys.");
        }
        String key = getKeyForProvider(getProvider());
        prefs.edit().putString(key, apiKey).apply();
    }

    public String getApiKey() {
        if (prefs == null) {
            Log.w(TAG, "Cannot read API key: encryption unavailable");
            return null;
        }
        String key = getKeyForProvider(getProvider());
        String apiKey = prefs.getString(key, null);
        if (apiKey == null) {
            // Migration: check legacy single-key storage
            apiKey = prefs.getString(KEY_API_KEY_LEGACY, null);
            if (apiKey != null) {
                // Migrate legacy key to current provider's slot
                prefs.edit().putString(key, apiKey).remove(KEY_API_KEY_LEGACY).apply();
            }
        }
        return apiKey;
    }

    public boolean hasApiKey() {
        if (prefs == null) return false;
        String key = getApiKey();
        return key != null && !key.trim().isEmpty();
    }

    public void clearApiKey() {
        if (prefs == null) {
            Log.w(TAG, "Cannot clear API key: encryption unavailable");
            return;
        }
        String key = getKeyForProvider(getProvider());
        prefs.edit().remove(key).apply();
    }

    public void saveProvider(String provider) {
        if (prefs == null) {
            Log.w(TAG, "Cannot save provider: encryption unavailable");
            return;
        }
        prefs.edit().putString(KEY_PROVIDER, provider).apply();
    }

    public String getProvider() {
        if (prefs == null) return PROVIDER_GEMINI;  // Default when encryption unavailable
        return prefs.getString(KEY_PROVIDER, PROVIDER_GEMINI);  // Default to Gemini (free tier)
    }

    public boolean isOpenAI() {
        return PROVIDER_OPENAI.equals(getProvider());
    }

    public boolean isClaude() {
        return PROVIDER_CLAUDE.equals(getProvider());
    }

    public boolean hasApiKeyForProvider(String provider) {
        if (prefs == null) return false;
        String key = getKeyForProvider(provider);
        String apiKey = prefs.getString(key, null);
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    // Reminder settings
    public void savePreferredReminderTime(int hourOfDay, int minute) {
        if (prefs == null) {
            Log.w(TAG, "Cannot save preferred reminder time: encryption unavailable");
            return;
        }
        int minutesSinceMidnight = hourOfDay * 60 + minute;
        prefs.edit().putInt(KEY_PREFERRED_REMINDER_TIME, minutesSinceMidnight).apply();
    }

    public int[] getPreferredReminderTime() {
        if (prefs == null) return new int[]{8, 0};  // Default 8:00 AM when encryption unavailable
        int minutesSinceMidnight = prefs.getInt(KEY_PREFERRED_REMINDER_TIME, 8 * 60); // Default 8:00 AM
        int hour = minutesSinceMidnight / 60;
        int minute = minutesSinceMidnight % 60;
        return new int[]{hour, minute};
    }

    public void setRemindersPaused(boolean paused) {
        if (prefs == null) {
            Log.w(TAG, "Cannot set reminders paused state: encryption unavailable");
            return;
        }
        prefs.edit().putBoolean(KEY_REMINDERS_PAUSED, paused).apply();
    }

    public boolean areRemindersPaused() {
        if (prefs == null) return false;  // Default: not paused when encryption unavailable
        return prefs.getBoolean(KEY_REMINDERS_PAUSED, false);
    }

    // One-time state tracking for UI flows
    public boolean hasRequestedNotificationPermission() {
        if (prefs == null) return false;
        return prefs.getBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, false);
    }

    public void setNotificationPermissionRequested() {
        if (prefs == null) {
            Log.w(TAG, "Cannot set notification permission requested: encryption unavailable");
            return;
        }
        prefs.edit().putBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, true).apply();
    }

    public boolean hasShownQuickDiagnosisTooltip() {
        if (prefs == null) return false;
        return prefs.getBoolean(KEY_QUICK_DIAGNOSIS_TOOLTIP_SHOWN, false);
    }

    public void setQuickDiagnosisTooltipShown() {
        if (prefs == null) {
            Log.w(TAG, "Cannot set quick diagnosis tooltip shown: encryption unavailable");
            return;
        }
        prefs.edit().putBoolean(KEY_QUICK_DIAGNOSIS_TOOLTIP_SHOWN, true).apply();
    }

    public boolean hasNotificationBannerDismissed() {
        if (prefs == null) return false;
        return prefs.getBoolean(KEY_NOTIFICATION_BANNER_DISMISSED, false);
    }

    public void setNotificationBannerDismissed() {
        if (prefs == null) {
            Log.w(TAG, "Cannot set notification banner dismissed: encryption unavailable");
            return;
        }
        prefs.edit().putBoolean(KEY_NOTIFICATION_BANNER_DISMISSED, true).apply();
    }
}
