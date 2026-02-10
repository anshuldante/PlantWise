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

public class OpenAIProviderTest {

    private MockWebServer mockWebServer;
    private OkHttpClient client;

    private static final String PLANT_JSON = "{\"identification\":{\"commonName\":\"Pothos\","
            + "\"scientificName\":\"Epipremnum aureum\",\"confidence\":\"high\",\"notes\":\"\"},"
            + "\"healthAssessment\":{\"score\":7,\"summary\":\"Good\",\"issues\":[]},"
            + "\"immediateActions\":[],\"carePlan\":{},\"funFact\":\"Easy to grow\"}";

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
    public void supportsVision_returnsTrue() {
        OpenAIProvider provider = new OpenAIProvider("sk-test-openai-key", new OkHttpClient());
        assertThat(provider.supportsVision()).isTrue();
    }

    @Test
    public void isConfigured_withApiKey_returnsTrue() {
        OpenAIProvider provider = new OpenAIProvider("sk-test-openai-key", new OkHttpClient());
        assertThat(provider.isConfigured()).isTrue();
    }

    @Test
    public void isConfigured_withNullApiKey_returnsFalse() {
        OpenAIProvider provider = new OpenAIProvider(null, new OkHttpClient());
        assertThat(provider.isConfigured()).isFalse();
    }

    @Test
    public void getDisplayName_returnsChatGPTOpenAI() {
        OpenAIProvider provider = new OpenAIProvider("sk-test-openai-key", new OkHttpClient());
        assertThat(provider.getDisplayName()).isEqualTo("ChatGPT (OpenAI)");
    }

    // ==================== rawResponse tests (06-06 fix) ====================

    @Test
    public void analyzePhoto_rawResponse_containsExtractedJson_notApiWrapper() throws Exception {
        // OpenAI wraps in {"choices":[{"message":{"content":"..."}}]}
        String apiResponse = "{\"choices\":[{\"message\":{\"content\":"
                + "\"" + PLANT_JSON.replace("\"", "\\\"") + "\""
                + "}}]}";

        mockWebServer.enqueue(new MockResponse().setBody(apiResponse).setResponseCode(200));

        OpenAIProvider provider = new OpenAIProvider("sk-test",
                mockWebServer.url("/").toString(), client);
        PlantAnalysisResult result = provider.analyzePhoto("base64data", "analyze");

        assertThat(result.rawResponse).isEqualTo(PLANT_JSON);
        assertThat(result.rawResponse).doesNotContain("\"choices\"");
        assertThat(result.rawResponse).doesNotContain("\"message\"");
    }

    @Test
    public void analyzePhoto_parsesPlantData() throws Exception {
        String apiResponse = "{\"choices\":[{\"message\":{\"content\":"
                + "\"" + PLANT_JSON.replace("\"", "\\\"") + "\""
                + "}}]}";

        mockWebServer.enqueue(new MockResponse().setBody(apiResponse).setResponseCode(200));

        OpenAIProvider provider = new OpenAIProvider("sk-test",
                mockWebServer.url("/").toString(), client);
        PlantAnalysisResult result = provider.analyzePhoto("base64data", "analyze");

        assertThat(result.identification.commonName).isEqualTo("Pothos");
        assertThat(result.identification.scientificName).isEqualTo("Epipremnum aureum");
        assertThat(result.healthAssessment.score).isEqualTo(7);
    }

    @Test
    public void analyzePhoto_stripsMarkdownBackticks() throws Exception {
        String wrappedJson = "```json\\n" + PLANT_JSON.replace("\"", "\\\"") + "\\n```";
        String apiResponse = "{\"choices\":[{\"message\":{\"content\":"
                + "\"" + wrappedJson + "\""
                + "}}]}";

        mockWebServer.enqueue(new MockResponse().setBody(apiResponse).setResponseCode(200));

        OpenAIProvider provider = new OpenAIProvider("sk-test",
                mockWebServer.url("/").toString(), client);
        PlantAnalysisResult result = provider.analyzePhoto("base64data", "analyze");

        assertThat(result.rawResponse).isEqualTo(PLANT_JSON);
        assertThat(result.rawResponse).doesNotContain("```");
    }

    @Test(expected = AIProviderException.class)
    public void analyzePhoto_apiError_throwsException() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(401).setBody("Unauthorized"));

        OpenAIProvider provider = new OpenAIProvider("sk-test",
                mockWebServer.url("/").toString(), client);
        provider.analyzePhoto("base64data", "analyze");
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

        OpenAIProvider provider = new OpenAIProvider("sk-test",
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

        OpenAIProvider provider = new OpenAIProvider("bad-key",
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

        OpenAIProvider provider = new OpenAIProvider("sk-test",
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
