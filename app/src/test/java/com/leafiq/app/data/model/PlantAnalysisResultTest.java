package com.leafiq.app.data.model;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import java.util.ArrayList;

public class PlantAnalysisResultTest {

    @Test
    public void identification_canBeCreated() {
        PlantAnalysisResult.Identification id = new PlantAnalysisResult.Identification();
        id.commonName = "Monstera";
        id.scientificName = "Monstera deliciosa";
        id.confidence = "high";
        id.notes = "A tropical plant";

        assertThat(id.commonName).isEqualTo("Monstera");
        assertThat(id.scientificName).isEqualTo("Monstera deliciosa");
        assertThat(id.confidence).isEqualTo("high");
        assertThat(id.notes).isEqualTo("A tropical plant");
    }

    @Test
    public void healthAssessment_canHoldIssues() {
        PlantAnalysisResult.HealthAssessment health = new PlantAnalysisResult.HealthAssessment();
        health.score = 7;
        health.summary = "Good overall health";
        health.issues = new ArrayList<>();

        PlantAnalysisResult.HealthAssessment.Issue issue = new PlantAnalysisResult.HealthAssessment.Issue();
        issue.name = "Yellow leaves";
        issue.severity = "low";
        issue.description = "Minor yellowing";
        issue.affectedArea = "Lower leaves";
        health.issues.add(issue);

        assertThat(health.score).isEqualTo(7);
        assertThat(health.issues).hasSize(1);
        assertThat(health.issues.get(0).name).isEqualTo("Yellow leaves");
    }

    @Test
    public void immediateAction_holdsPriorityInfo() {
        PlantAnalysisResult.ImmediateAction action = new PlantAnalysisResult.ImmediateAction();
        action.action = "Water the plant";
        action.priority = "urgent";
        action.detail = "Soil is completely dry";

        assertThat(action.action).isEqualTo("Water the plant");
        assertThat(action.priority).isEqualTo("urgent");
        assertThat(action.detail).isEqualTo("Soil is completely dry");
    }

    @Test
    public void carePlan_holdsAllCareInfo() {
        PlantAnalysisResult.CarePlan carePlan = new PlantAnalysisResult.CarePlan();

        carePlan.watering = new PlantAnalysisResult.CarePlan.Watering();
        carePlan.watering.frequency = "Weekly";
        carePlan.watering.amount = "Until drainage";
        carePlan.watering.notes = "Let dry between waterings";

        carePlan.light = new PlantAnalysisResult.CarePlan.Light();
        carePlan.light.ideal = "Bright indirect";
        carePlan.light.current = "Good";
        carePlan.light.adjustment = null;

        carePlan.fertilizer = new PlantAnalysisResult.CarePlan.Fertilizer();
        carePlan.fertilizer.type = "Balanced 10-10-10";
        carePlan.fertilizer.frequency = "Monthly";
        carePlan.fertilizer.nextApplication = "March";

        carePlan.pruning = new PlantAnalysisResult.CarePlan.Pruning();
        carePlan.pruning.needed = true;
        carePlan.pruning.instructions = "Remove dead leaves";
        carePlan.pruning.when = "Spring";

        carePlan.repotting = new PlantAnalysisResult.CarePlan.Repotting();
        carePlan.repotting.needed = false;
        carePlan.repotting.signs = "No signs of root bound";
        carePlan.repotting.recommendedPotSize = null;

        carePlan.seasonal = "Reduce watering in winter";

        assertThat(carePlan.watering.frequency).isEqualTo("Weekly");
        assertThat(carePlan.light.ideal).isEqualTo("Bright indirect");
        assertThat(carePlan.fertilizer.type).isEqualTo("Balanced 10-10-10");
        assertThat(carePlan.pruning.needed).isTrue();
        assertThat(carePlan.repotting.needed).isFalse();
        assertThat(carePlan.seasonal).isEqualTo("Reduce watering in winter");
    }

    @Test
    public void plantAnalysisResult_holdsAllComponents() {
        PlantAnalysisResult result = new PlantAnalysisResult();

        result.identification = new PlantAnalysisResult.Identification();
        result.identification.commonName = "Test Plant";

        result.healthAssessment = new PlantAnalysisResult.HealthAssessment();
        result.healthAssessment.score = 8;

        result.immediateActions = new ArrayList<>();
        result.carePlan = new PlantAnalysisResult.CarePlan();
        result.funFact = "Interesting fact";

        assertThat(result.identification.commonName).isEqualTo("Test Plant");
        assertThat(result.healthAssessment.score).isEqualTo(8);
        assertThat(result.funFact).isEqualTo("Interesting fact");
    }

    @Test
    public void rawResponse_canBeSetAndRead() {
        PlantAnalysisResult result = new PlantAnalysisResult();
        result.rawResponse = "{\"raw\":\"json response from API\"}";

        assertThat(result.rawResponse).isEqualTo("{\"raw\":\"json response from API\"}");
    }
}
