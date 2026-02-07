package com.leafiq.app.ai;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class OpenAIProviderTest {

    @Test
    public void supportsVision_returnsTrue() {
        OpenAIProvider provider = new OpenAIProvider("sk-test-openai-key");
        assertThat(provider.supportsVision()).isTrue();
    }

    @Test
    public void isConfigured_withApiKey_returnsTrue() {
        OpenAIProvider provider = new OpenAIProvider("sk-test-openai-key");
        assertThat(provider.isConfigured()).isTrue();
    }

    @Test
    public void isConfigured_withNullApiKey_returnsFalse() {
        OpenAIProvider provider = new OpenAIProvider(null);
        assertThat(provider.isConfigured()).isFalse();
    }

    @Test
    public void getDisplayName_returnsChatGPTOpenAI() {
        OpenAIProvider provider = new OpenAIProvider("sk-test-openai-key");
        assertThat(provider.getDisplayName()).isEqualTo("ChatGPT (OpenAI)");
    }
}
