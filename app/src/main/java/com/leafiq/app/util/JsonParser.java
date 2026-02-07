package com.leafiq.app.util;

import com.leafiq.app.data.model.PlantAnalysisResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class JsonParser {

    public static PlantAnalysisResult parsePlantAnalysis(String jsonString) throws JSONException {
        JSONObject json = new JSONObject(jsonString);
        PlantAnalysisResult result = new PlantAnalysisResult();

        // Parse identification
        if (json.has("identification")) {
            JSONObject idJson = json.getJSONObject("identification");
            result.identification = new PlantAnalysisResult.Identification();
            result.identification.commonName = idJson.optString("commonName", "Unknown");
            result.identification.scientificName = idJson.optString("scientificName", "");
            result.identification.confidence = idJson.optString("confidence", "low");
            result.identification.notes = idJson.optString("notes", "");
        }

        // Parse health assessment
        if (json.has("healthAssessment")) {
            JSONObject healthJson = json.getJSONObject("healthAssessment");
            result.healthAssessment = new PlantAnalysisResult.HealthAssessment();
            result.healthAssessment.score = healthJson.optInt("score", 5);
            result.healthAssessment.summary = healthJson.optString("summary", "");
            result.healthAssessment.issues = new ArrayList<>();

            if (healthJson.has("issues")) {
                JSONArray issuesArray = healthJson.getJSONArray("issues");
                for (int i = 0; i < issuesArray.length(); i++) {
                    JSONObject issueJson = issuesArray.getJSONObject(i);
                    PlantAnalysisResult.HealthAssessment.Issue issue =
                        new PlantAnalysisResult.HealthAssessment.Issue();
                    issue.name = issueJson.optString("name", "");
                    issue.severity = issueJson.optString("severity", "low");
                    issue.description = issueJson.optString("description", "");
                    issue.affectedArea = issueJson.optString("affectedArea", "");
                    result.healthAssessment.issues.add(issue);
                }
            }
        }

        // Parse immediate actions
        result.immediateActions = new ArrayList<>();
        if (json.has("immediateActions")) {
            JSONArray actionsArray = json.getJSONArray("immediateActions");
            for (int i = 0; i < actionsArray.length(); i++) {
                JSONObject actionJson = actionsArray.getJSONObject(i);
                PlantAnalysisResult.ImmediateAction action = new PlantAnalysisResult.ImmediateAction();
                action.action = actionJson.optString("action", "");
                action.priority = actionJson.optString("priority", "when_convenient");
                action.detail = actionJson.optString("detail", "");
                result.immediateActions.add(action);
            }
        }

        // Parse care plan
        if (json.has("carePlan")) {
            JSONObject careJson = json.getJSONObject("carePlan");
            result.carePlan = new PlantAnalysisResult.CarePlan();

            if (careJson.has("watering")) {
                JSONObject waterJson = careJson.getJSONObject("watering");
                result.carePlan.watering = new PlantAnalysisResult.CarePlan.Watering();
                result.carePlan.watering.frequency = waterJson.optString("frequency", "");
                result.carePlan.watering.amount = waterJson.optString("amount", "");
                result.carePlan.watering.notes = waterJson.optString("notes", "");
            }

            if (careJson.has("light")) {
                JSONObject lightJson = careJson.getJSONObject("light");
                result.carePlan.light = new PlantAnalysisResult.CarePlan.Light();
                result.carePlan.light.ideal = lightJson.optString("ideal", "");
                result.carePlan.light.current = lightJson.optString("current", "");
                result.carePlan.light.adjustment = lightJson.optString("adjustment", null);
            }

            if (careJson.has("fertilizer")) {
                JSONObject fertJson = careJson.getJSONObject("fertilizer");
                result.carePlan.fertilizer = new PlantAnalysisResult.CarePlan.Fertilizer();
                result.carePlan.fertilizer.type = fertJson.optString("type", "");
                result.carePlan.fertilizer.frequency = fertJson.optString("frequency", "");
                result.carePlan.fertilizer.nextApplication = fertJson.optString("nextApplication", "");
            }

            if (careJson.has("pruning")) {
                JSONObject pruneJson = careJson.getJSONObject("pruning");
                result.carePlan.pruning = new PlantAnalysisResult.CarePlan.Pruning();
                result.carePlan.pruning.needed = pruneJson.optBoolean("needed", false);
                result.carePlan.pruning.instructions = pruneJson.optString("instructions", "");
                result.carePlan.pruning.when = pruneJson.optString("when", "");
            }

            if (careJson.has("repotting")) {
                JSONObject repotJson = careJson.getJSONObject("repotting");
                result.carePlan.repotting = new PlantAnalysisResult.CarePlan.Repotting();
                result.carePlan.repotting.needed = repotJson.optBoolean("needed", false);
                result.carePlan.repotting.signs = repotJson.optString("signs", "");
                result.carePlan.repotting.recommendedPotSize = repotJson.optString("recommendedPotSize", null);
            }

            result.carePlan.seasonal = careJson.optString("seasonal", "");
        }

        result.funFact = json.optString("funFact", "");

        return result;
    }
}
