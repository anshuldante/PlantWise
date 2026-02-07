package com.leafiq.app.ai;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class PerplexityProviderTest {

    @Test
    public void supportsVision_returnsFalse() {
        PerplexityProvider provider = new PerplexityProvider("pplx-test-key");
        assertThat(provider.supportsVision()).isFalse();
    }

    @Test
    public void isConfigured_withApiKey_returnsTrue() {
        PerplexityProvider provider = new PerplexityProvider("pplx-test-key");
        assertThat(provider.isConfigured()).isTrue();
    }

    @Test
    public void isConfigured_withNullApiKey_returnsFalse() {
        PerplexityProvider provider = new PerplexityProvider(null);
        assertThat(provider.isConfigured()).isFalse();
    }

    @Test
    public void isConfigured_withEmptyApiKey_returnsFalse() {
        PerplexityProvider provider = new PerplexityProvider("");
        assertThat(provider.isConfigured()).isFalse();
    }

    @Test
    public void getDisplayName_returnsPerplexity() {
        PerplexityProvider provider = new PerplexityProvider("pplx-test-key");
        assertThat(provider.getDisplayName()).isEqualTo("Perplexity");
    }
}
