package com.leafiq.app.ai;

import androidx.annotation.Nullable;

import com.leafiq.app.data.entity.Analysis;

import java.util.List;

public class PromptBuilder {

    public static String buildAnalysisPrompt(
            @Nullable String knownPlantName,
            @Nullable List<Analysis> previousAnalyses,
            @Nullable String location) {

        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert botanist and plant care specialist. ");
        sb.append("Analyze this plant photo and provide a detailed assessment.\n\n");

        if (knownPlantName != null) {
            sb.append("This plant was previously identified as: ")
              .append(knownPlantName).append("\n");
        } else {
            sb.append("Identify this plant.\n");
        }

        if (location != null) {
            sb.append("Location: ").append(location).append("\n");
        }

        if (previousAnalyses != null && !previousAnalyses.isEmpty()) {
            sb.append("\nPrevious analyses for this plant:\n");
            for (Analysis a : previousAnalyses) {
                sb.append("- ").append(a.createdAt)
                  .append(": Health ").append(a.healthScore)
                  .append("/10, Summary: ").append(a.summary)
                  .append("\n");
            }
            sb.append("\nCompare the current photo with previous assessments ");
            sb.append("and note any improvements or deterioration.\n");
        }

        sb.append("\nRespond ONLY with valid JSON in this exact format ");
        sb.append("(no markdown, no backticks, no explanatory text before or after):\n");
        sb.append(getJsonTemplate());

        sb.append("\n\nBe specific and actionable. ");
        sb.append("If you can see the pot, soil, or surroundings, ");
        sb.append("factor those into your assessment. ");
        sb.append("If you're unsure about something, say so rather than guessing.");

        return sb.toString();
    }

    private static String getJsonTemplate() {
        return "{\n"
            + "  \"identification\": {\n"
            + "    \"commonName\": \"string\",\n"
            + "    \"scientificName\": \"string\",\n"
            + "    \"confidence\": \"high | medium | low\",\n"
            + "    \"notes\": \"string\"\n"
            + "  },\n"
            + "  \"healthAssessment\": {\n"
            + "    \"score\": 1-10,\n"
            + "    \"summary\": \"1-2 sentence overview\",\n"
            + "    \"issues\": [\n"
            + "      {\n"
            + "        \"name\": \"string\",\n"
            + "        \"severity\": \"low | medium | high\",\n"
            + "        \"description\": \"string\",\n"
            + "        \"affectedArea\": \"string\"\n"
            + "      }\n"
            + "    ]\n"
            + "  },\n"
            + "  \"immediateActions\": [\n"
            + "    {\n"
            + "      \"action\": \"string\",\n"
            + "      \"priority\": \"urgent | soon | when_convenient\",\n"
            + "      \"detail\": \"string\"\n"
            + "    }\n"
            + "  ],\n"
            + "  \"carePlan\": {\n"
            + "    \"watering\": {\n"
            + "      \"frequency\": \"string\",\n"
            + "      \"amount\": \"string\",\n"
            + "      \"notes\": \"string\"\n"
            + "    },\n"
            + "    \"light\": {\n"
            + "      \"ideal\": \"string\",\n"
            + "      \"current\": \"string\",\n"
            + "      \"adjustment\": \"string or null\"\n"
            + "    },\n"
            + "    \"fertilizer\": {\n"
            + "      \"type\": \"string\",\n"
            + "      \"frequency\": \"string\",\n"
            + "      \"nextApplication\": \"string\"\n"
            + "    },\n"
            + "    \"pruning\": {\n"
            + "      \"needed\": true/false,\n"
            + "      \"instructions\": \"string\",\n"
            + "      \"when\": \"string\"\n"
            + "    },\n"
            + "    \"repotting\": {\n"
            + "      \"needed\": true/false,\n"
            + "      \"signs\": \"string\",\n"
            + "      \"recommendedPotSize\": \"string or null\"\n"
            + "    },\n"
            + "    \"seasonal\": \"string\"\n"
            + "  },\n"
            + "  \"funFact\": \"string\"\n"
            + "}";
    }
}
