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

public class GeminiProvider implements AIProvider {
    private static final String DEFAULT_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent";
    private final String apiKey;
    private final String apiUrl;
    private final OkHttpClient client;

    public GeminiProvider(String apiKey, OkHttpClient client) {
        this.apiKey = apiKey;
        this.apiUrl = DEFAULT_API_URL;
        this.client = client;
    }

    // Package-private constructor for testing with MockWebServer
    GeminiProvider(String apiKey, String apiUrl, OkHttpClient client) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.client = client;
    }

    @Override
    public PlantAnalysisResult analyzePhoto(String imageBase64, String prompt)
            throws AIProviderException {
        try {
            JSONObject requestBody = new JSONObject();

            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();

            JSONArray parts = new JSONArray();

            // Text part
            JSONObject textPart = new JSONObject();
            textPart.put("text", prompt);
            parts.put(textPart);

            // Image part
            JSONObject imagePart = new JSONObject();
            JSONObject inlineData = new JSONObject();
            inlineData.put("mimeType", "image/jpeg");
            inlineData.put("data", imageBase64);
            imagePart.put("inlineData", inlineData);
            parts.put(imagePart);

            content.put("parts", parts);
            contents.put(content);
            requestBody.put("contents", contents);

            // Generation config
            JSONObject generationConfig = new JSONObject();
            generationConfig.put("maxOutputTokens", 2048);
            generationConfig.put("temperature", 0.4);
            requestBody.put("generationConfig", generationConfig);

            RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
            );

            String url = apiUrl + "?key=" + apiKey;

            Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
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

                // Extract text from Gemini response
                JSONArray candidates = json.getJSONArray("candidates");
                JSONObject candidate = candidates.getJSONObject(0);
                JSONObject contentObj = candidate.getJSONObject("content");
                JSONArray partsArray = contentObj.getJSONArray("parts");
                String aiText = partsArray.getJSONObject(0).getString("text");

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
        return "Gemini (Google)";
    }

    @Override
    public boolean supportsVision() {
        return true;
    }
}
