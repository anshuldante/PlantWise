package com.leafiq.app.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class KeystoreHelper {

    private static final String PREFS_NAME = "leafiq_secure_prefs";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_PROVIDER = "ai_provider";

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

    public void saveApiKey(String apiKey) {
        prefs.edit().putString(KEY_API_KEY, apiKey).apply();
    }

    public String getApiKey() {
        return prefs.getString(KEY_API_KEY, null);
    }

    public boolean hasApiKey() {
        String key = getApiKey();
        return key != null && !key.trim().isEmpty();
    }

    public void clearApiKey() {
        prefs.edit().remove(KEY_API_KEY).apply();
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
}
