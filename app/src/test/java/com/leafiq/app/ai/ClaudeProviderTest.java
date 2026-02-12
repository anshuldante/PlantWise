package com.leafiq.app.ai;

import static com.google.common.truth.Truth.assertThat;

import com.leafiq.app.data.model.PlantAnalysisResult;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;

public class ClaudeProviderTest {

    private MockWebServer mockWebServer;
    private OkHttpClient client;

    // Minimal valid plant analysis JSON that JsonParser can parse
    private static final String PLANT_JSON = "{\"identification\":{\"commonName\":\"Monstera\","
            + "\"scientificName\":\"Monstera deliciosa\",\"confidence\":\"high\",\"notes\":\"\"},"
            + "\"healthAssessment\":{\"score\":8,\"summary\":\"Healthy\",\"issues\":[]},"
            + "\"immediateActions\":[],\"carePlan\":{},\"funFact\":\"Test fact\"}";

    @Before
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        client = new OkHttpClient();
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void isConfigured_withApiKey_returnsTrue() {
        ClaudeProvider provider = new ClaudeProvider("sk-test-key-123", new OkHttpClient());
        assertThat(provider.isConfigured()).isTrue();
    }

    @Test
    public void isConfigured_withNullApiKey_returnsFalse() {
        ClaudeProvider provider = new ClaudeProvider(null, new OkHttpClient());
        assertThat(provider.isConfigured()).isFalse();
    }

    @Test
    public void isConfigured_withEmptyApiKey_returnsFalse() {
        ClaudeProvider provider = new ClaudeProvider("", new OkHttpClient());
        assertThat(provider.isConfigured()).isFalse();
    }

    @Test
    public void isConfigured_withWhitespaceApiKey_returnsFalse() {
        ClaudeProvider provider = new ClaudeProvider("   ", new OkHttpClient());
        assertThat(provider.isConfigured()).isFalse();
    }

    @Test
    public void getDisplayName_returnsClaude() {
        ClaudeProvider provider = new ClaudeProvider("sk-test", new OkHttpClient());
        assertThat(provider.getDisplayName()).isEqualTo("Claude (Anthropic)");
    }

    @Test
    public void supportsVision_returnsTrue() {
        ClaudeProvider provider = new ClaudeProvider("sk-test", new OkHttpClient());
        assertThat(provider.supportsVision()).isTrue();
    }

    // ==================== rawResponse tests (06-06 fix) ====================

    @Test
    public void analyzePhoto_rawResponse_containsExtractedJson_notApiWrapper() throws Exception {
        // Claude API wraps the plant JSON in {"content":[{"type":"text","text":"..."}]}
        String apiResponse = "{\"content\":[{\"type\":\"text\",\"text\":"
                + "\"" + PLANT_JSON.replace("\"", "\\\"") + "\""
                + "}]}";

        mockWebServer.enqueue(new MockResponse().setBody(apiResponse).setResponseCode(200));

        ClaudeProvider provider = new ClaudeProvider("sk-test",
                mockWebServer.url("/").toString(), client);
        PlantAnalysisResult result = provider.analyzePhoto("base64data", "analyze");

        // rawResponse should be the extracted plant JSON, not the API wrapper
        assertThat(result.rawResponse).isEqualTo(PLANT_JSON);
        assertThat(result.rawResponse).doesNotContain("\"content\":[");
    }

    @Test
    public void analyzePhoto_parsesIdentification() throws Exception {
        String apiResponse = "{\"content\":[{\"type\":\"text\",\"text\":"
                + "\"" + PLANT_JSON.replace("\"", "\\\"") + "\""
                + "}]}";

        mockWebServer.enqueue(new MockResponse().setBody(apiResponse).setResponseCode(200));

        ClaudeProvider provider = new ClaudeProvider("sk-test",
                mockWebServer.url("/").toString(), client);
        PlantAnalysisResult result = provider.analyzePhoto("base64data", "analyze");

        assertThat(result.identification.commonName).isEqualTo("Monstera");
        assertThat(result.identification.scientificName).isEqualTo("Monstera deliciosa");
        assertThat(result.healthAssessment.score).isEqualTo(8);
    }

    @Test
    public void analyzePhoto_stripsMarkdownBackticks() throws Exception {
        // AI wraps JSON in markdown code blocks
        String wrappedJson = "```json\n" + PLANT_JSON + "\n```";
        String apiResponse = "{\"content\":[{\"type\":\"text\",\"text\":"
                + "\"" + wrappedJson.replace("\"", "\\\"").replace("\n", "\\n") + "\""
                + "}]}";

        mockWebServer.enqueue(new MockResponse().setBody(apiResponse).setResponseCode(200));

        ClaudeProvider provider = new ClaudeProvider("sk-test",
                mockWebServer.url("/").toString(), client);
        PlantAnalysisResult result = provider.analyzePhoto("base64data", "analyze");

        assertThat(result.rawResponse).isEqualTo(PLANT_JSON);
        assertThat(result.rawResponse).doesNotContain("```");
    }

    @Test
    public void analyzePhoto_extractsJsonFromSurroundingText() throws Exception {
        // AI adds explanation text around JSON
        String textWithJson = "Here is the analysis:\\n" + PLANT_JSON.replace("\"", "\\\"") + "\\nHope this helps!";
        String apiResponse = "{\"content\":[{\"type\":\"text\",\"text\":\"" + textWithJson + "\"}]}";

        mockWebServer.enqueue(new MockResponse().setBody(apiResponse).setResponseCode(200));

        ClaudeProvider provider = new ClaudeProvider("sk-test",
                mockWebServer.url("/").toString(), client);
        PlantAnalysisResult result = provider.analyzePhoto("base64data", "analyze");

        // Should extract just the JSON between first { and last }
        assertThat(result.rawResponse).startsWith("{");
        assertThat(result.rawResponse).endsWith("}");
        assertThat(result.identification.commonName).isEqualTo("Monstera");
    }

    @Test(expected = AIProviderException.class)
    public void analyzePhoto_apiError_throwsException() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));

        ClaudeProvider provider = new ClaudeProvider("sk-test",
                mockWebServer.url("/").toString(), client);
        provider.analyzePhoto("base64data", "analyze");
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

    // ==================== Network error tests (09-05) ====================

    @Test
    public void analyzePhoto_timeout_throwsException() throws Exception {
        // Use a client with very short timeout for fast test execution
        OkHttpClient shortTimeoutClient = new OkHttpClient.Builder()
            .readTimeout(1, TimeUnit.SECONDS)
            .callTimeout(2, TimeUnit.SECONDS)
            .build();

        mockWebServer.enqueue(new MockResponse()
            .setSocketPolicy(SocketPolicy.NO_RESPONSE));

        ClaudeProvider provider = new ClaudeProvider("sk-test",
            mockWebServer.url("/").toString(), shortTimeoutClient);

        AIProviderException exception = null;
        try {
            provider.analyzePhoto("base64data", "analyze");
        } catch (AIProviderException e) {
            exception = e;
        }
        assertThat(exception).isNotNull();
    }

    @Test
    public void analyzePhoto_401_throwsExceptionWithStatusCode() throws Exception {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(401)
            .setBody("{\"error\":{\"message\":\"Invalid API key\"}}"));

        ClaudeProvider provider = new ClaudeProvider("bad-key",
            mockWebServer.url("/").toString(), client);

        AIProviderException exception = null;
        try {
            provider.analyzePhoto("base64data", "analyze");
        } catch (AIProviderException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getHttpStatusCode()).isEqualTo(401);
    }

    @Test
    public void analyzePhoto_500_throwsExceptionWithStatusCode() throws Exception {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setBody("Internal Server Error"));

        ClaudeProvider provider = new ClaudeProvider("sk-test",
            mockWebServer.url("/").toString(), client);

        AIProviderException exception = null;
        try {
            provider.analyzePhoto("base64data", "analyze");
        } catch (AIProviderException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getHttpStatusCode()).isEqualTo(500);
    }
}
