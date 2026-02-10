package com.leafiq.app.ai;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;

import okhttp3.OkHttpClient;

public class AIProviderFactoryTest {

    private OkHttpClient client;

    @Before
    public void setUp() {
        client = new OkHttpClient();
    }

    @Test
    public void create_gemini_returnsGeminiProvider() {
        AIProvider provider = AIProviderFactory.create("gemini", "test-key", client);
        assertThat(provider).isInstanceOf(GeminiProvider.class);
    }

    @Test
    public void create_claude_returnsClaudeProvider() {
        AIProvider provider = AIProviderFactory.create("claude", "test-key", client);
        assertThat(provider).isInstanceOf(ClaudeProvider.class);
    }

    @Test
    public void create_openai_returnsOpenAIProvider() {
        AIProvider provider = AIProviderFactory.create("openai", "test-key", client);
        assertThat(provider).isInstanceOf(OpenAIProvider.class);
    }

    @Test
    public void create_perplexity_returnsPerplexityProvider() {
        AIProvider provider = AIProviderFactory.create("perplexity", "test-key", client);
        assertThat(provider).isInstanceOf(PerplexityProvider.class);
    }

    @Test
    public void create_caseInsensitive_returnsCorrectProvider() {
        assertThat(AIProviderFactory.create("GEMINI", "key", client)).isInstanceOf(GeminiProvider.class);
        assertThat(AIProviderFactory.create("Claude", "key", client)).isInstanceOf(ClaudeProvider.class);
        assertThat(AIProviderFactory.create("OpenAI", "key", client)).isInstanceOf(OpenAIProvider.class);
        assertThat(AIProviderFactory.create("PERPLEXITY", "key", client)).isInstanceOf(PerplexityProvider.class);
    }

    @Test
    public void create_providerIsConfigured_withValidKey() {
        AIProvider provider = AIProviderFactory.create("claude", "sk-test-key", client);
        assertThat(provider.isConfigured()).isTrue();
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_nullProviderName_throwsIllegalArgument() {
        AIProviderFactory.create(null, "test-key", client);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_nullApiKey_throwsIllegalArgument() {
        AIProviderFactory.create("claude", null, client);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_unknownProvider_throwsIllegalArgument() {
        AIProviderFactory.create("unknown_provider", "test-key", client);
    }

    @Test
    public void create_unknownProvider_errorMessageContainsProviderName() {
        try {
            AIProviderFactory.create("foobar", "test-key", client);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("foobar");
            assertThat(e.getMessage()).contains("Supported");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_nullClient_throwsIllegalArgument() {
        AIProviderFactory.create("gemini", "test-key", null);
    }
}
