package com.leafiq.app.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class KeystoreHelper {

    private static final String PREFS_NAME = "leafiq_secure_prefs";
    private static final String KEY_API_KEY_GEMINI = "api_key_gemini";
    private static final String KEY_API_KEY_CLAUDE = "api_key_claude";
    private static final String KEY_API_KEY_OPENAI = "api_key_openai";
    private static final String KEY_API_KEY_PERPLEXITY = "api_key_perplexity";
    // Legacy key for migration
    private static final String KEY_API_KEY_LEGACY = "api_key";
    private static final String KEY_PROVIDER = "ai_provider";
    private static final String KEY_PREFERRED_REMINDER_TIME = "preferred_reminder_time";
    private static final String KEY_REMINDERS_PAUSED = "reminders_paused";

    public static final String PROVIDER_OPENAI = "openai";
    public static final String PROVIDER_CLAUDE = "claude";
    public static final String PROVIDER_PERPLEXITY = "perplexity";
    public static final String PROVIDER_GEMINI = "gemini";

    private final SharedPreferences prefs;

    public KeystoreHelper(Context context) {
        SharedPreferences tempPrefs;
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

            tempPrefs = EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            // Fallback to regular prefs if encryption fails (should not happen)
            tempPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
        this.prefs = tempPrefs;
    }

    private String getKeyForProvider(String provider) {
        switch (provider) {
            case PROVIDER_GEMINI: return KEY_API_KEY_GEMINI;
            case PROVIDER_CLAUDE: return KEY_API_KEY_CLAUDE;
            case PROVIDER_OPENAI: return KEY_API_KEY_OPENAI;
            case PROVIDER_PERPLEXITY: return KEY_API_KEY_PERPLEXITY;
            default: return KEY_API_KEY_GEMINI;
        }
    }

    public void saveApiKey(String apiKey) {
        String key = getKeyForProvider(getProvider());
        prefs.edit().putString(key, apiKey).apply();
    }

    public String getApiKey() {
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
        String key = getApiKey();
        return key != null && !key.trim().isEmpty();
    }

    public void clearApiKey() {
        String key = getKeyForProvider(getProvider());
        prefs.edit().remove(key).apply();
    }

    public void saveProvider(String provider) {
        prefs.edit().putString(KEY_PROVIDER, provider).apply();
    }

    public String getProvider() {
        return prefs.getString(KEY_PROVIDER, PROVIDER_GEMINI);  // Default to Gemini (free tier)
    }

    public boolean isOpenAI() {
        return PROVIDER_OPENAI.equals(getProvider());
    }

    public boolean isClaude() {
        return PROVIDER_CLAUDE.equals(getProvider());
    }

    public boolean hasApiKeyForProvider(String provider) {
        String key = getKeyForProvider(provider);
        String apiKey = prefs.getString(key, null);
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    // Reminder settings
    public void savePreferredReminderTime(int hourOfDay, int minute) {
        int minutesSinceMidnight = hourOfDay * 60 + minute;
        prefs.edit().putInt(KEY_PREFERRED_REMINDER_TIME, minutesSinceMidnight).apply();
    }

    public int[] getPreferredReminderTime() {
        int minutesSinceMidnight = prefs.getInt(KEY_PREFERRED_REMINDER_TIME, 8 * 60); // Default 8:00 AM
        int hour = minutesSinceMidnight / 60;
        int minute = minutesSinceMidnight % 60;
        return new int[]{hour, minute};
    }

    public void setRemindersPaused(boolean paused) {
        prefs.edit().putBoolean(KEY_REMINDERS_PAUSED, paused).apply();
    }

    public boolean areRemindersPaused() {
        return prefs.getBoolean(KEY_REMINDERS_PAUSED, false);
    }
}
