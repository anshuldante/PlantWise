package com.leafiq.app.ai;

import static com.google.common.truth.Truth.assertThat;

import com.leafiq.app.data.model.PlantAnalysisResult;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class PerplexityProviderTest {

    private MockWebServer mockWebServer;
    private OkHttpClient client;

    // Perplexity is text-only, so plant JSON may have less data
    private static final String PLANT_JSON = "{\"identification\":{\"commonName\":\"Aloe Vera\","
            + "\"scientificName\":\"Aloe barbadensis miller\",\"confidence\":\"medium\",\"notes\":\"Based on description\"},"
            + "\"healthAssessment\":{\"score\":6,\"summary\":\"Needs attention\",\"issues\":[]},"
            + "\"immediateActions\":[],\"carePlan\":{},\"funFact\":\"Medicinal uses\"}";

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

    // ==================== rawResponse tests (06-06 fix) ====================

    @Test
    public void analyzePhoto_rawResponse_containsExtractedJson_notApiWrapper() throws Exception {
        // Perplexity uses same format as OpenAI: {"choices":[{"message":{"content":"..."}}]}
        String apiResponse = "{\"choices\":[{\"message\":{\"content\":"
                + "\"" + PLANT_JSON.replace("\"", "\\\"") + "\""
                + "}}]}";

        mockWebServer.enqueue(new MockResponse().setBody(apiResponse).setResponseCode(200));

        PerplexityProvider provider = new PerplexityProvider("pplx-test",
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

        PerplexityProvider provider = new PerplexityProvider("pplx-test",
                mockWebServer.url("/").toString(), client);
        PlantAnalysisResult result = provider.analyzePhoto("base64data", "analyze");

        assertThat(result.identification.commonName).isEqualTo("Aloe Vera");
        assertThat(result.identification.scientificName).isEqualTo("Aloe barbadensis miller");
        assertThat(result.healthAssessment.score).isEqualTo(6);
    }

    @Test
    public void analyzePhoto_stripsMarkdownBackticks() throws Exception {
        String wrappedJson = "```json\\n" + PLANT_JSON.replace("\"", "\\\"") + "\\n```";
        String apiResponse = "{\"choices\":[{\"message\":{\"content\":"
                + "\"" + wrappedJson + "\""
                + "}}]}";

        mockWebServer.enqueue(new MockResponse().setBody(apiResponse).setResponseCode(200));

        PerplexityProvider provider = new PerplexityProvider("pplx-test",
                mockWebServer.url("/").toString(), client);
        PlantAnalysisResult result = provider.analyzePhoto("base64data", "analyze");

        assertThat(result.rawResponse).isEqualTo(PLANT_JSON);
        assertThat(result.rawResponse).doesNotContain("```");
    }

    @Test(expected = AIProviderException.class)
    public void analyzePhoto_apiError_throwsException() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(429).setBody("Rate limited"));

        PerplexityProvider provider = new PerplexityProvider("pplx-test",
                mockWebServer.url("/").toString(), client);
        provider.analyzePhoto("base64data", "analyze");
    }
}
