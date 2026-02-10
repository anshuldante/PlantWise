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

public class OpenAIProvider implements AIProvider {
    private static final String DEFAULT_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o";
    private final String apiKey;
    private final String apiUrl;
    private final OkHttpClient client;

    public OpenAIProvider(String apiKey, OkHttpClient client) {
        this.apiKey = apiKey;
        this.apiUrl = DEFAULT_API_URL;
        this.client = client;
    }

    // Package-private constructor for testing with MockWebServer
    OpenAIProvider(String apiKey, String apiUrl, OkHttpClient client) {
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

            // Text block first for OpenAI
            JSONObject textBlock = new JSONObject();
            textBlock.put("type", "text");
            textBlock.put("text", prompt);
            content.put(textBlock);

            // Image block
            JSONObject imageBlock = new JSONObject();
            imageBlock.put("type", "image_url");
            JSONObject imageUrl = new JSONObject();
            imageUrl.put("url", "data:image/jpeg;base64," + imageBase64);
            imageBlock.put("image_url", imageUrl);
            content.put(imageBlock);

            userMessage.put("content", content);
            messages.put(userMessage);
            requestBody.put("messages", messages);

            RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new AIProviderException(
                        "API error: " + response.code() + " " + response.message(), null, response.code());
                }

                String responseBody = response.body().string();
                JSONObject json = new JSONObject(responseBody);
                JSONArray choices = json.getJSONArray("choices");
                String aiText = choices.getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

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
            throw new AIProviderException("Analysis failed: " + e.getMessage(), e, 0);
        }
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    @Override
    public String getDisplayName() {
        return "ChatGPT (OpenAI)";
    }

    @Override
    public boolean supportsVision() {
        return true;
    }
}
