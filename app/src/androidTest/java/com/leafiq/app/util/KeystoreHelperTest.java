package com.leafiq.app.util;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class KeystoreHelperTest {

    private Context context;
    private KeystoreHelper keystoreHelper;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        keystoreHelper = new KeystoreHelper(context);
        // Clear all keys before each test
        clearAllKeys();
    }

    @After
    public void tearDown() {
        clearAllKeys();
    }

    private void clearAllKeys() {
        keystoreHelper.saveProvider(KeystoreHelper.PROVIDER_CLAUDE);
        keystoreHelper.clearApiKey();
        keystoreHelper.saveProvider(KeystoreHelper.PROVIDER_GEMINI);
        keystoreHelper.clearApiKey();
        keystoreHelper.saveProvider(KeystoreHelper.PROVIDER_OPENAI);
        keystoreHelper.clearApiKey();
        keystoreHelper.saveProvider(KeystoreHelper.PROVIDER_PERPLEXITY);
        keystoreHelper.clearApiKey();
        // Reset to default provider
        keystoreHelper.saveProvider(KeystoreHelper.PROVIDER_GEMINI);
    }

    @Test
    public void saveAndGetApiKey_perProvider_independent() {
        // Save key for Claude
        keystoreHelper.saveProvider(KeystoreHelper.PROVIDER_CLAUDE);
        keystoreHelper.saveApiKey("claude-key-123");

        // Save key for Gemini
        keystoreHelper.saveProvider(KeystoreHelper.PROVIDER_GEMINI);
        keystoreHelper.saveApiKey("gemini-key-456");

        // Verify each provider retrieves its own key
        keystoreHelper.saveProvider(KeystoreHelper.PROVIDER_CLAUDE);
        assertThat(keystoreHelper.getApiKey()).isEqualTo("claude-key-123");

        keystoreHelper.saveProvider(KeystoreHelper.PROVIDER_GEMINI);
        assertThat(keystoreHelper.getApiKey()).isEqualTo("gemini-key-456");
    }

    @Test
    public void hasApiKeyForProvider_withKey_returnsTrue() {
        keystoreHelper.saveProvider(KeystoreHelper.PROVIDER_OPENAI);
        keystoreHelper.saveApiKey("sk-openai-key");

        assertThat(keystoreHelper.hasApiKeyForProvider(KeystoreHelper.PROVIDER_OPENAI)).isTrue();
    }

    @Test
    public void hasApiKeyForProvider_withoutKey_returnsFalse() {
        assertThat(keystoreHelper.hasApiKeyForProvider(KeystoreHelper.PROVIDER_PERPLEXITY)).isFalse();
    }

    @Test
    public void getApiKey_withLegacyKey_migratesAndRemovesLegacy() throws Exception {
        // Write directly to the legacy "api_key" slot using the underlying prefs
        MasterKey masterKey = new MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build();
        SharedPreferences prefs = EncryptedSharedPreferences.create(
            context,
            "leafiq_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
        prefs.edit().putString("api_key", "legacy-key-789").apply();

        // Ensure no key exists for the current provider slot
        keystoreHelper.saveProvider(KeystoreHelper.PROVIDER_GEMINI);

        // getApiKey should migrate the legacy key
        String apiKey = keystoreHelper.getApiKey();
        assertThat(apiKey).isEqualTo("legacy-key-789");

        // Legacy key should be removed after migration
        String legacyKey = prefs.getString("api_key", null);
        assertThat(legacyKey).isNull();
    }

    @Test
    public void clearApiKey_removesOnlyCurrentProvider() {
        // Save keys for two providers
        keystoreHelper.saveProvider(KeystoreHelper.PROVIDER_CLAUDE);
        keystoreHelper.saveApiKey("claude-key");

        keystoreHelper.saveProvider(KeystoreHelper.PROVIDER_OPENAI);
        keystoreHelper.saveApiKey("openai-key");

        // Clear only OpenAI's key
        keystoreHelper.clearApiKey();

        // OpenAI key should be gone
        assertThat(keystoreHelper.hasApiKeyForProvider(KeystoreHelper.PROVIDER_OPENAI)).isFalse();

        // Claude key should remain
        assertThat(keystoreHelper.hasApiKeyForProvider(KeystoreHelper.PROVIDER_CLAUDE)).isTrue();
    }
}
