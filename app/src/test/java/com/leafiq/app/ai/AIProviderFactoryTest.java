package com.leafiq.app.ai;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class AIProviderFactoryTest {

    @Test
    public void create_gemini_returnsGeminiProvider() {
        AIProvider provider = AIProviderFactory.create("gemini", "test-key");
        assertThat(provider).isInstanceOf(GeminiProvider.class);
    }

    @Test
    public void create_claude_returnsClaudeProvider() {
        AIProvider provider = AIProviderFactory.create("claude", "test-key");
        assertThat(provider).isInstanceOf(ClaudeProvider.class);
    }

    @Test
    public void create_openai_returnsOpenAIProvider() {
        AIProvider provider = AIProviderFactory.create("openai", "test-key");
        assertThat(provider).isInstanceOf(OpenAIProvider.class);
    }

    @Test
    public void create_perplexity_returnsPerplexityProvider() {
        AIProvider provider = AIProviderFactory.create("perplexity", "test-key");
        assertThat(provider).isInstanceOf(PerplexityProvider.class);
    }

    @Test
    public void create_caseInsensitive_returnsCorrectProvider() {
        assertThat(AIProviderFactory.create("GEMINI", "key")).isInstanceOf(GeminiProvider.class);
        assertThat(AIProviderFactory.create("Claude", "key")).isInstanceOf(ClaudeProvider.class);
        assertThat(AIProviderFactory.create("OpenAI", "key")).isInstanceOf(OpenAIProvider.class);
        assertThat(AIProviderFactory.create("PERPLEXITY", "key")).isInstanceOf(PerplexityProvider.class);
    }

    @Test
    public void create_providerIsConfigured_withValidKey() {
        AIProvider provider = AIProviderFactory.create("claude", "sk-test-key");
        assertThat(provider.isConfigured()).isTrue();
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_nullProviderName_throwsIllegalArgument() {
        AIProviderFactory.create(null, "test-key");
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_nullApiKey_throwsIllegalArgument() {
        AIProviderFactory.create("claude", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_unknownProvider_throwsIllegalArgument() {
        AIProviderFactory.create("unknown_provider", "test-key");
    }

    @Test
    public void create_unknownProvider_errorMessageContainsProviderName() {
        try {
            AIProviderFactory.create("foobar", "test-key");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("foobar");
            assertThat(e.getMessage()).contains("Supported");
        }
    }
}
