package com.leafiq.app.util;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import android.content.SharedPreferences;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

/**
 * Unit tests for KeystoreHelper encryption failure behavior.
 *
 * Tests verify that when encryption is unhealthy:
 * - saveApiKey() throws IllegalStateException
 * - Read methods return sensible defaults
 * - Write methods (other than saveApiKey) fail silently
 * - isEncryptionHealthy() correctly reports state
 *
 * Uses package-private test constructor to avoid Android dependencies.
 */
@RunWith(JUnit4.class)
public class KeystoreHelperEncryptionTest {

    @Test
    public void isEncryptionHealthy_whenUnhealthy_returnsFalse() {
        KeystoreHelper helper = new KeystoreHelper(null, false);
        assertThat(helper.isEncryptionHealthy()).isFalse();
    }

    @Test
    public void isEncryptionHealthy_whenHealthy_returnsTrue() {
        SharedPreferences mockPrefs = Mockito.mock(SharedPreferences.class);
        KeystoreHelper helper = new KeystoreHelper(mockPrefs, true);
        assertThat(helper.isEncryptionHealthy()).isTrue();
    }

    @Test
    public void saveApiKey_whenUnhealthy_throwsIllegalStateException() {
        KeystoreHelper helper = new KeystoreHelper(null, false);

        try {
            helper.saveApiKey("test-key");
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).contains("Secure storage unavailable");
        }
    }

    @Test
    public void getApiKey_whenUnhealthy_returnsNull() {
        KeystoreHelper helper = new KeystoreHelper(null, false);
        assertThat(helper.getApiKey()).isNull();
    }

    @Test
    public void getProvider_whenUnhealthy_returnsDefault() {
        KeystoreHelper helper = new KeystoreHelper(null, false);
        assertThat(helper.getProvider()).isEqualTo(KeystoreHelper.PROVIDER_GEMINI);
    }

    @Test
    public void hasApiKey_whenUnhealthy_returnsFalse() {
        KeystoreHelper helper = new KeystoreHelper(null, false);
        assertThat(helper.hasApiKey()).isFalse();
    }

    @Test
    public void saveApiKey_whenHealthy_succeeds() {
        // Create mock SharedPreferences with edit chain
        SharedPreferences.Editor mockEditor = Mockito.mock(SharedPreferences.Editor.class);
        Mockito.when(mockEditor.putString(Mockito.anyString(), Mockito.anyString())).thenReturn(mockEditor);
        Mockito.doNothing().when(mockEditor).apply();

        SharedPreferences mockPrefs = Mockito.mock(SharedPreferences.class);
        Mockito.when(mockPrefs.edit()).thenReturn(mockEditor);
        Mockito.when(mockPrefs.getString(Mockito.eq("ai_provider"), Mockito.anyString()))
                .thenReturn(KeystoreHelper.PROVIDER_GEMINI);

        KeystoreHelper helper = new KeystoreHelper(mockPrefs, true);

        // Should not throw
        helper.saveApiKey("test-key");

        // Verify prefs.edit().putString() was called
        Mockito.verify(mockEditor).putString(Mockito.anyString(), Mockito.eq("test-key"));
        Mockito.verify(mockEditor).apply();
    }

    @Test
    public void saveProvider_whenUnhealthy_doesNotThrow() {
        KeystoreHelper helper = new KeystoreHelper(null, false);

        // Should not throw, just fail silently
        helper.saveProvider(KeystoreHelper.PROVIDER_CLAUDE);

        // No exception = test passes
    }

    @Test
    public void getPreferredReminderTime_whenUnhealthy_returnsDefault() {
        KeystoreHelper helper = new KeystoreHelper(null, false);
        int[] time = helper.getPreferredReminderTime();

        assertThat(time).isNotNull();
        assertThat(time).hasLength(2);
        assertThat(time[0]).isEqualTo(8);  // 8:00 AM hour
        assertThat(time[1]).isEqualTo(0);  // 0 minutes
    }

    @Test
    public void areRemindersPaused_whenUnhealthy_returnsFalse() {
        KeystoreHelper helper = new KeystoreHelper(null, false);
        assertThat(helper.areRemindersPaused()).isFalse();
    }
}
