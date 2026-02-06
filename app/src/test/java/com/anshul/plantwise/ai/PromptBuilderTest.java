package com.anshul.plantwise.ai;

import static com.google.common.truth.Truth.assertThat;

import com.anshul.plantwise.data.entity.Analysis;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

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
}
