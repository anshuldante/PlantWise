package com.leafiq.app.ai;

import okhttp3.OkHttpClient;

/**
 * Static factory for creating AIProvider instances based on provider name.
 * <p>
 * Supported providers: gemini, claude, openai, perplexity
 * <p>
 * Usage:
 * <pre>
 * String provider = keystoreHelper.getProvider();
 * String apiKey = keystoreHelper.getApiKey();
 * OkHttpClient client = ((LeafIQApplication) getApplication()).getHttpClient();
 * AIProvider aiProvider = AIProviderFactory.create(provider, apiKey, client);
 * if (aiProvider.isConfigured() && aiProvider.supportsVision()) {
 *     PlantAnalysisResult result = aiProvider.analyzePhoto(imageBase64, prompt);
 * }
 * </pre>
 */
public class AIProviderFactory {

    /**
     * Private constructor prevents instantiation of utility class.
     */
    private AIProviderFactory() {
        throw new AssertionError("Utility class - do not instantiate");
    }

    /**
     * Creates an AIProvider instance for the specified provider name.
     *
     * @param providerName One of: "gemini", "claude", "openai", "perplexity" (case-insensitive)
     * @param apiKey The API key for the provider
     * @param client The shared OkHttpClient instance (from LeafIQApplication)
     * @return Configured AIProvider instance
     * @throws IllegalArgumentException if providerName, apiKey, or client is null, or if provider is unknown
     */
    public static AIProvider create(String providerName, String apiKey, OkHttpClient client) {
        if (providerName == null) {
            throw new IllegalArgumentException("Provider name cannot be null");
        }
        if (apiKey == null) {
            throw new IllegalArgumentException("API key cannot be null");
        }
        if (client == null) {
            throw new IllegalArgumentException("HTTP client cannot be null");
        }

        switch (providerName.toLowerCase()) {
            case "gemini":
                return new GeminiProvider(apiKey, client);
            case "claude":
                return new ClaudeProvider(apiKey, client);
            case "openai":
                return new OpenAIProvider(apiKey, client);
            case "perplexity":
                return new PerplexityProvider(apiKey, client);
            default:
                throw new IllegalArgumentException(
                    "Unknown provider: " + providerName +
                    ". Supported: gemini, claude, openai, perplexity"
                );
        }
    }
}
