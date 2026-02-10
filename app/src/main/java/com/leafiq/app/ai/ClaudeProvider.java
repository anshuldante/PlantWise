package com.leafiq.app.ai;

import com.leafiq.app.data.model.PlantAnalysisResult;
import com.leafiq.app.util.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ClaudeProvider implements AIProvider {
    private static final String DEFAULT_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-sonnet-4-20250514";
    private static final String API_VERSION = "2023-06-01";
    private final String apiKey;
    private final String apiUrl;
    private final OkHttpClient client;

    public ClaudeProvider(String apiKey, OkHttpClient client) {
        this.apiKey = apiKey;
        this.apiUrl = DEFAULT_API_URL;
        this.client = client;
    }

    // Package-private constructor for testing with MockWebServer
    ClaudeProvider(String apiKey, String apiUrl, OkHttpClient client) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.client = client;
    }

    @Override
    public PlantAnalysisResult analyzePhoto(String imageBase64, String prompt)
            throws AIProviderException {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", MODEL);
            requestBody.put("max_tokens", 2048);

            JSONArray messages = new JSONArray();
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");

            JSONArray content = new JSONArray();

            // Image block
            JSONObject imageBlock = new JSONObject();
            imageBlock.put("type", "image");
            JSONObject source = new JSONObject();
            source.put("type", "base64");
            source.put("media_type", "image/jpeg");
            source.put("data", imageBase64);
            imageBlock.put("source", source);
            content.put(imageBlock);

            // Text block
            JSONObject textBlock = new JSONObject();
            textBlock.put("type", "text");
            textBlock.put("text", prompt);
            content.put(textBlock);

            userMessage.put("content", content);
            messages.put(userMessage);
            requestBody.put("messages", messages);

            RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", API_VERSION)
                .addHeader("content-type", "application/json")
                .post(body)
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    throw new AIProviderException(
                        "API error: " + response.code() + " " + response.message() + " - " + errorBody);
                }

                String responseBody = response.body().string();
                JSONObject json = new JSONObject(responseBody);
                JSONArray contentArray = json.getJSONArray("content");
                String aiText = contentArray.getJSONObject(0).getString("text");

                // Defensive: strip markdown backticks if AI wrapped the JSON
                aiText = aiText.trim();
                if (aiText.startsWith("```")) {
                    aiText = aiText.replaceAll("^```json?\\s*", "")
                                   .replaceAll("\\s*```$", "");
                }

                // Find first { and last } to handle any extra text
                int start = aiText.indexOf('{');
                int end = aiText.lastIndexOf('}');
                if (start >= 0 && end > start) {
                    aiText = aiText.substring(start, end + 1);
                }

                PlantAnalysisResult result = JsonParser.parsePlantAnalysis(aiText);
                result.rawResponse = aiText;
                return result;
            }
        } catch (JSONException | IOException e) {
            throw new AIProviderException("Analysis failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    @Override
    public String getDisplayName() {
        return "Claude (Anthropic)";
    }

    @Override
    public boolean supportsVision() {
        return true;
    }
}
