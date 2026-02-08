package com.leafiq.app.ai;

import static com.google.common.truth.Truth.assertThat;

import com.leafiq.app.data.entity.Analysis;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PromptBuilderTest {

    @Test
    public void buildAnalysisPrompt_withNoContext_includesBasicInstructions() {
        String prompt = PromptBuilder.buildAnalysisPrompt(null, null, null);

        assertThat(prompt).contains("expert botanist");
        assertThat(prompt).contains("Analyze this plant photo");
        assertThat(prompt).contains("Identify this plant");
        assertThat(prompt).contains("JSON");
    }

    @Test
    public void buildAnalysisPrompt_withKnownPlantName_includesName() {
        String prompt = PromptBuilder.buildAnalysisPrompt("Monstera Deliciosa", null, null);

        assertThat(prompt).contains("previously identified as");
        assertThat(prompt).contains("Monstera Deliciosa");
        assertThat(prompt).doesNotContain("Identify this plant");
    }

    @Test
    public void buildAnalysisPrompt_withLocation_includesLocation() {
        String prompt = PromptBuilder.buildAnalysisPrompt(null, null, "Living room, north-facing window");

        assertThat(prompt).contains("Location:");
        assertThat(prompt).contains("Living room, north-facing window");
    }

    @Test
    public void buildAnalysisPrompt_withPreviousAnalyses_includesHistory() {
        List<Analysis> previousAnalyses = new ArrayList<>();

        Analysis analysis1 = new Analysis();
        analysis1.id = "1";
        analysis1.createdAt = 1700000000000L;
        analysis1.healthScore = 7;
        analysis1.summary = "Healthy but needs water";
        previousAnalyses.add(analysis1);

        Analysis analysis2 = new Analysis();
        analysis2.id = "2";
        analysis2.createdAt = 1700100000000L;
        analysis2.healthScore = 8;
        analysis2.summary = "Improved after watering";
        previousAnalyses.add(analysis2);

        String prompt = PromptBuilder.buildAnalysisPrompt("Snake Plant", previousAnalyses, null);

        assertThat(prompt).contains("Previous analyses");
        assertThat(prompt).contains("Health 7/10");
        assertThat(prompt).contains("Health 8/10");
        assertThat(prompt).contains("Healthy but needs water");
        assertThat(prompt).contains("Improved after watering");
        assertThat(prompt).contains("improvements or deterioration");
    }

    @Test
    public void buildAnalysisPrompt_alwaysIncludesJsonTemplate() {
        String prompt = PromptBuilder.buildAnalysisPrompt(null, null, null);

        assertThat(prompt).contains("identification");
        assertThat(prompt).contains("commonName");
        assertThat(prompt).contains("scientificName");
        assertThat(prompt).contains("healthAssessment");
        assertThat(prompt).contains("score");
        assertThat(prompt).contains("issues");
        assertThat(prompt).contains("immediateActions");
        assertThat(prompt).contains("carePlan");
        assertThat(prompt).contains("watering");
        assertThat(prompt).contains("light");
        assertThat(prompt).contains("fertilizer");
        assertThat(prompt).contains("pruning");
        assertThat(prompt).contains("repotting");
        assertThat(prompt).contains("funFact");
    }

    @Test
    public void buildAnalysisPrompt_instructsNoMarkdown() {
        String prompt = PromptBuilder.buildAnalysisPrompt(null, null, null);

        assertThat(prompt).contains("no markdown");
        assertThat(prompt).contains("no backticks");
        assertThat(prompt).contains("ONLY with valid JSON");
    }

    @Test
    public void buildAnalysisPrompt_includesActionableAdvice() {
        String prompt = PromptBuilder.buildAnalysisPrompt(null, null, null);

        assertThat(prompt).contains("specific and actionable");
        assertThat(prompt).contains("pot, soil, or surroundings");
    }

    @Test
    public void buildAnalysisPrompt_withEmptyPreviousAnalyses_treatsAsNull() {
        List<Analysis> emptyList = new ArrayList<>();

        String prompt = PromptBuilder.buildAnalysisPrompt(null, emptyList, null);

        assertThat(prompt).doesNotContain("Previous analyses");
        assertThat(prompt).doesNotContain("improvements or deterioration");
    }

    @Test
    public void buildAnalysisPrompt_withAllParameters_combinesCorrectly() {
        List<Analysis> previousAnalyses = new ArrayList<>();
        Analysis analysis = new Analysis();
        analysis.id = "1";
        analysis.createdAt = 1700000000000L;
        analysis.healthScore = 6;
        analysis.summary = "Needs more light";
        previousAnalyses.add(analysis);

        String prompt = PromptBuilder.buildAnalysisPrompt(
            "Fiddle Leaf Fig",
            previousAnalyses,
            "Office desk"
        );

        assertThat(prompt).contains("Fiddle Leaf Fig");
        assertThat(prompt).contains("Office desk");
        assertThat(prompt).contains("Health 6/10");
        assertThat(prompt).contains("Needs more light");
    }

    @Test
    public void buildAnalysisPrompt_withPreviousAnalyses_formatsTimestampsAsDate() {
        long timestamp = 1700000000000L;
        List<Analysis> previousAnalyses = new ArrayList<>();

        Analysis analysis = new Analysis();
        analysis.id = "1";
        analysis.createdAt = timestamp;
        analysis.healthScore = 7;
        analysis.summary = "Healthy";
        previousAnalyses.add(analysis);

        String prompt = PromptBuilder.buildAnalysisPrompt("Test Plant", previousAnalyses, null);

        // Compute expected date string in the local timezone (matches PromptBuilder logic)
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
        String expectedDate = sdf.format(new Date(timestamp));

        // Should contain formatted date, not raw epoch long
        assertThat(prompt).contains(expectedDate);
        assertThat(prompt).doesNotContain("1700000000000");
    }

    @Test
    public void buildAnalysisPrompt_withZeroTimestamp_showsUnknownDate() {
        List<Analysis> previousAnalyses = new ArrayList<>();

        Analysis analysis = new Analysis();
        analysis.id = "1";
        analysis.createdAt = 0L;
        analysis.healthScore = 5;
        analysis.summary = "Unknown timing";
        previousAnalyses.add(analysis);

        String prompt = PromptBuilder.buildAnalysisPrompt("Test Plant", previousAnalyses, null);

        assertThat(prompt).contains("Unknown date");
    }

    // ==================== buildCorrectionPrompt tests ====================

    @Test
    public void buildCorrectionPrompt_withNoContext_includesReAnalyzeInstructions() {
        String prompt = PromptBuilder.buildCorrectionPrompt(null, null, null, null);

        assertThat(prompt).contains("expert botanist");
        assertThat(prompt).contains("corrections to a previous analysis");
        assertThat(prompt).contains("re-analyze");
    }

    @Test
    public void buildCorrectionPrompt_withCorrectedName_includesUserConfirmation() {
        String prompt = PromptBuilder.buildCorrectionPrompt("Monstera Deliciosa", null, null, null);

        assertThat(prompt).contains("user confirms this plant is");
        assertThat(prompt).contains("Monstera Deliciosa");
        assertThat(prompt).contains("correct identification");
    }

    @Test
    public void buildCorrectionPrompt_withAdditionalContext_includesContext() {
        String prompt = PromptBuilder.buildCorrectionPrompt(null, "Leaves turning yellow recently", null, null);

        assertThat(prompt).contains("Additional context from the user");
        assertThat(prompt).contains("Leaves turning yellow recently");
        assertThat(prompt).contains("Incorporate this information");
    }

    @Test
    public void buildCorrectionPrompt_withLocation_includesLocation() {
        String prompt = PromptBuilder.buildCorrectionPrompt(null, null, null, "Kitchen windowsill");

        assertThat(prompt).contains("Location:");
        assertThat(prompt).contains("Kitchen windowsill");
    }

    @Test
    public void buildCorrectionPrompt_withPreviousAnalyses_includesHistory() {
        List<Analysis> previousAnalyses = new ArrayList<>();

        Analysis analysis = new Analysis();
        analysis.id = "1";
        analysis.createdAt = 1700000000000L;
        analysis.healthScore = 6;
        analysis.summary = "Needs more water";
        previousAnalyses.add(analysis);

        String prompt = PromptBuilder.buildCorrectionPrompt(null, null, previousAnalyses, null);

        assertThat(prompt).contains("Previous analyses");
        assertThat(prompt).contains("Health 6/10");
        assertThat(prompt).contains("Needs more water");
    }

    @Test
    public void buildCorrectionPrompt_alwaysIncludesJsonTemplate() {
        String prompt = PromptBuilder.buildCorrectionPrompt(null, null, null, null);

        assertThat(prompt).contains("identification");
        assertThat(prompt).contains("commonName");
        assertThat(prompt).contains("healthAssessment");
        assertThat(prompt).contains("carePlan");
        assertThat(prompt).contains("ONLY with valid JSON");
    }

    @Test
    public void buildCorrectionPrompt_withAllParameters_combinesCorrectly() {
        List<Analysis> previousAnalyses = new ArrayList<>();
        Analysis analysis = new Analysis();
        analysis.id = "1";
        analysis.createdAt = 1700000000000L;
        analysis.healthScore = 7;
        analysis.summary = "Recovering well";
        previousAnalyses.add(analysis);

        String prompt = PromptBuilder.buildCorrectionPrompt(
            "Peace Lily",
            "It was recently repotted",
            previousAnalyses,
            "Bathroom"
        );

        assertThat(prompt).contains("Peace Lily");
        assertThat(prompt).contains("It was recently repotted");
        assertThat(prompt).contains("Bathroom");
        assertThat(prompt).contains("Health 7/10");
        assertThat(prompt).contains("Recovering well");
    }
}
