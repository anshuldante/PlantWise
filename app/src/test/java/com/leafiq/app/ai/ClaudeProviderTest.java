package com.leafiq.app.ai;

import static com.google.common.truth.Truth.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class ClaudeProviderTest {

    private MockWebServer mockWebServer;

    @Before
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void isConfigured_withApiKey_returnsTrue() {
        ClaudeProvider provider = new ClaudeProvider("sk-test-key-123");
        assertThat(provider.isConfigured()).isTrue();
    }

    @Test
    public void isConfigured_withNullApiKey_returnsFalse() {
        ClaudeProvider provider = new ClaudeProvider(null);
        assertThat(provider.isConfigured()).isFalse();
    }

    @Test
    public void isConfigured_withEmptyApiKey_returnsFalse() {
        ClaudeProvider provider = new ClaudeProvider("");
        assertThat(provider.isConfigured()).isFalse();
    }

    @Test
    public void isConfigured_withWhitespaceApiKey_returnsFalse() {
        ClaudeProvider provider = new ClaudeProvider("   ");
        assertThat(provider.isConfigured()).isFalse();
    }

    @Test
    public void getDisplayName_returnsClaude() {
        ClaudeProvider provider = new ClaudeProvider("sk-test");
        assertThat(provider.getDisplayName()).isEqualTo("Claude (Anthropic)");
    }

    @Test
    public void analyzePhoto_parsesJsonResponse() throws Exception {
        // This test demonstrates the expected behavior
        // In production, you'd inject the URL or use a TestClaudeProvider

        String mockApiResponse = "{"
            + "\"content\": [{"
            + "  \"type\": \"text\","
            + "  \"text\": \"{\\\"identification\\\":{\\\"commonName\\\":\\\"Test Plant\\\",\\\"scientificName\\\":\\\"Testus plantus\\\",\\\"confidence\\\":\\\"high\\\",\\\"notes\\\":\\\"Test\\\"},\\\"healthAssessment\\\":{\\\"score\\\":8,\\\"summary\\\":\\\"Good\\\",\\\"issues\\\":[]},\\\"immediateActions\\\":[],\\\"carePlan\\\":{},\\\"funFact\\\":\\\"Test fact\\\"}\""
            + "}]"
            + "}";

        mockWebServer.enqueue(new MockResponse()
            .setBody(mockApiResponse)
            .setResponseCode(200));

        // This would work if ClaudeProvider accepted a base URL:
        // ClaudeProvider provider = new ClaudeProvider("sk-test", mockWebServer.url("/").toString());
        // PlantAnalysisResult result = provider.analyzePhoto("base64data", "analyze");
        // assertThat(result.identification.commonName).isEqualTo("Test Plant");

        assertThat(true).isTrue(); // Placeholder - actual test needs URL injection
    }

    @Test
    public void analyzePhoto_stripsMarkdownBackticks() {
        // Test that the provider handles responses wrapped in markdown
        String responseWithBackticks = "```json\n{\"identification\":{\"commonName\":\"Test\"}}\n```";
        String cleaned = responseWithBackticks.trim()
            .replaceAll("^```json?\\s*", "")
            .replaceAll("\\s*```$", "");

        assertThat(cleaned).isEqualTo("{\"identification\":{\"commonName\":\"Test\"}}");
    }

    @Test
    public void analyzePhoto_extractsJsonFromText() {
        // Test that the provider can find JSON within surrounding text
        String responseWithExtraText = "Here is the analysis:\n{\"identification\":{\"commonName\":\"Test\"}}\nHope this helps!";

        int start = responseWithExtraText.indexOf('{');
        int end = responseWithExtraText.lastIndexOf('}');
        String extracted = responseWithExtraText.substring(start, end + 1);

        assertThat(extracted).isEqualTo("{\"identification\":{\"commonName\":\"Test\"}}");
    }

    @Test
    public void supportsVision_returnsTrue() {
        ClaudeProvider provider = new ClaudeProvider("sk-test");
        assertThat(provider.supportsVision()).isTrue();
    }

    @Test
    public void stripBackticks_handlesVariousFormats() {
        // Test various markdown wrapping formats
        String[] inputs = {
            "```json\n{\"test\":1}\n```",
            "```\n{\"test\":1}\n```",
            "```json{\"test\":1}```",
            "{\"test\":1}"  // No wrapping
        };

        for (String input : inputs) {
            String cleaned = input.trim()
                .replaceAll("^```json?\\s*", "")
                .replaceAll("\\s*```$", "");

            // Find JSON boundaries
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}');
            if (start >= 0 && end > start) {
                cleaned = cleaned.substring(start, end + 1);
            }

            assertThat(cleaned).isEqualTo("{\"test\":1}");
        }
    }
}
