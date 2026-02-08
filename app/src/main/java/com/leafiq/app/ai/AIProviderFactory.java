package com.leafiq.app.ai;

/**
 * Static factory for creating AIProvider instances based on provider name.
 * <p>
 * Supported providers: gemini, claude, openai, perplexity
 * <p>
 * Usage:
 * <pre>
 * String provider = keystoreHelper.getProvider();
 * String apiKey = keystoreHelper.getApiKey();
 * AIProvider aiProvider = AIProviderFactory.create(provider, apiKey);
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
     * @return Configured AIProvider instance
     * @throws IllegalArgumentException if providerName or apiKey is null, or if provider is unknown
     */
    public static AIProvider create(String providerName, String apiKey) {
        if (providerName == null) {
            throw new IllegalArgumentException("Provider name cannot be null");
        }
        if (apiKey == null) {
            throw new IllegalArgumentException("API key cannot be null");
        }

        switch (providerName.toLowerCase()) {
            case "gemini":
                return new GeminiProvider(apiKey);
            case "claude":
                return new ClaudeProvider(apiKey);
            case "openai":
                return new OpenAIProvider(apiKey);
            case "perplexity":
                return new PerplexityProvider(apiKey);
            default:
                throw new IllegalArgumentException(
                    "Unknown provider: " + providerName +
                    ". Supported: gemini, claude, openai, perplexity"
                );
        }
    }
}
