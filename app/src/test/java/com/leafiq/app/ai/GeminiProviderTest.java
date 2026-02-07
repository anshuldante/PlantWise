package com.leafiq.app.ai;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class GeminiProviderTest {

    @Test
    public void supportsVision_returnsTrue() {
        GeminiProvider provider = new GeminiProvider("test-gemini-key");
        assertThat(provider.supportsVision()).isTrue();
    }

    @Test
    public void isConfigured_withApiKey_returnsTrue() {
        GeminiProvider provider = new GeminiProvider("test-gemini-key");
        assertThat(provider.isConfigured()).isTrue();
    }

    @Test
    public void isConfigured_withNullApiKey_returnsFalse() {
        GeminiProvider provider = new GeminiProvider(null);
        assertThat(provider.isConfigured()).isFalse();
    }

    @Test
    public void getDisplayName_returnsGeminiGoogle() {
        GeminiProvider provider = new GeminiProvider("test-gemini-key");
        assertThat(provider.getDisplayName()).isEqualTo("Gemini (Google)");
    }
}
