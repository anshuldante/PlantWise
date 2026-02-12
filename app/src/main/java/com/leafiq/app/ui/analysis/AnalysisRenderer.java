package com.leafiq.app.ui.analysis;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import android.graphics.drawable.GradientDrawable;

import com.leafiq.app.R;
import com.leafiq.app.data.model.PlantAnalysisResult;
import com.leafiq.app.databinding.ActivityAnalysisBinding;

/**
 * Centralized view rendering logic for AnalysisActivity.
 * Maps AnalysisUiState to view operations (visibility, content, styling).
 * <p>
 * Extracted from AnalysisActivity to make rendering logic testable.
 * Pure rendering methods with no Activity dependencies.
 */
public class AnalysisRenderer {

    /**
     * Main rendering entry point.
     * Routes to specific rendering methods based on UI state.
     */
    public static void render(ActivityAnalysisBinding binding, AnalysisUiState state) {
        switch (state.getState()) {
            case IDLE:
                // Initial state, nothing to show
                break;
            case LOADING:
                renderLoading(binding);
                break;
            case SUCCESS:
                binding.photoTipsContainer.setVisibility(View.GONE);
                renderResults(binding, state.getResult());
                break;
            case ERROR:
                binding.photoTipsContainer.setVisibility(View.GONE);
                renderError(binding, state.getErrorMessage());
                break;
        }
    }

    /**
     * Renders loading state.
     */
    private static void renderLoading(ActivityAnalysisBinding binding) {
        binding.loadingContainer.setVisibility(View.VISIBLE);
        binding.resultsContainer.setVisibility(View.GONE);
        binding.errorContainer.setVisibility(View.GONE);
    }

    /**
     * Renders error state.
     */
    private static void renderError(ActivityAnalysisBinding binding, String errorMessage) {
        binding.loadingContainer.setVisibility(View.GONE);
        binding.resultsContainer.setVisibility(View.GONE);
        binding.errorContainer.setVisibility(View.VISIBLE);
        binding.errorMessage.setText(errorMessage);
    }

    /**
     * Renders success state with analysis results.
     */
    private static void renderResults(ActivityAnalysisBinding binding, PlantAnalysisResult result) {
        binding.resultsContainer.setVisibility(View.VISIBLE);
        binding.loadingContainer.setVisibility(View.GONE);
        binding.errorContainer.setVisibility(View.GONE);
        binding.btnCorrect.setVisibility(View.VISIBLE);

        Context context = binding.getRoot().getContext();

        // Render identification section
        if (result.identification != null) {
            binding.plantCommonName.setText(result.identification.commonName);
            binding.plantScientificName.setText(result.identification.scientificName);

            if (result.identification.notes != null && !result.identification.notes.isEmpty()) {
                binding.identificationNotes.setText(result.identification.notes);
                binding.identificationNotes.setVisibility(View.VISIBLE);
            }
        }

        // Render health assessment section
        if (result.healthAssessment != null) {
            int score = result.healthAssessment.score;
            binding.healthScore.setText(String.valueOf(score));

            // Set health score color
            int colorRes = getHealthScoreColorRes(score);
            ((GradientDrawable) binding.healthScore.getBackground()).setColor(
                ContextCompat.getColor(context, colorRes));

            binding.healthSummary.setText(result.healthAssessment.summary);

            if (result.healthAssessment.issues != null && !result.healthAssessment.issues.isEmpty()) {
                binding.healthIssuesLabel.setVisibility(View.VISIBLE);
                binding.issuesContainer.removeAllViews();
                for (PlantAnalysisResult.HealthAssessment.Issue issue : result.healthAssessment.issues) {
                    renderIssue(context, binding.issuesContainer, issue);
                }
            }
        }

        // Render immediate actions section
        if (result.immediateActions != null && !result.immediateActions.isEmpty()) {
            binding.actionsCard.setVisibility(View.VISIBLE);
            binding.actionsContainer.removeAllViews();
            for (PlantAnalysisResult.ImmediateAction action : result.immediateActions) {
                renderAction(context, binding.actionsContainer, action);
            }
        }

        // Render care plan section
        if (result.carePlan != null) {
            binding.carePlanContainer.removeAllViews();
            renderCarePlan(context, binding.carePlanContainer, result.carePlan);
        }

        // Render fun fact section
        if (result.funFact != null && !result.funFact.isEmpty()) {
            binding.funFactCard.setVisibility(View.VISIBLE);
            binding.funFactText.setText(result.funFact);
        }
    }

    /**
     * Renders a single health issue.
     */
    private static void renderIssue(Context context, LinearLayout container,
                                    PlantAnalysisResult.HealthAssessment.Issue issue) {
        TextView tv = new TextView(context);
        tv.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        tv.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        tv.setPadding(0, 8, 0, 8);

        String severityEmoji = getSeverityEmoji(issue.severity);
        tv.setText(severityEmoji + " " + issue.name + ": " + issue.description);

        container.addView(tv);
    }

    /**
     * Renders a single immediate action.
     */
    private static void renderAction(Context context, LinearLayout container,
                                     PlantAnalysisResult.ImmediateAction action) {
        TextView tv = new TextView(context);
        tv.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        tv.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        tv.setPadding(0, 8, 0, 8);

        String priorityPrefix = getPriorityPrefix(action.priority);
        tv.setText(priorityPrefix + action.action + "\n   " + action.detail);

        container.addView(tv);
    }

    /**
     * Renders care plan section with all care items.
     */
    private static void renderCarePlan(Context context, LinearLayout container,
                                      PlantAnalysisResult.CarePlan carePlan) {
        if (carePlan.watering != null) {
            renderCarePlanItem(context, container, "Watering",
                carePlan.watering.frequency + " - " + carePlan.watering.amount +
                (carePlan.watering.notes != null ? "\n" + carePlan.watering.notes : ""));
        }

        if (carePlan.light != null) {
            renderCarePlanItem(context, container, "Light",
                "Ideal: " + carePlan.light.ideal +
                (carePlan.light.adjustment != null ? "\nAdjustment: " + carePlan.light.adjustment : ""));
        }

        if (carePlan.fertilizer != null) {
            renderCarePlanItem(context, container, "Fertilizer",
                carePlan.fertilizer.type + " - " + carePlan.fertilizer.frequency);
        }

        if (carePlan.pruning != null && carePlan.pruning.needed) {
            renderCarePlanItem(context, container, "Pruning", carePlan.pruning.instructions);
        }

        if (carePlan.repotting != null && carePlan.repotting.needed) {
            renderCarePlanItem(context, container, "Repotting",
                carePlan.repotting.signs +
                (carePlan.repotting.recommendedPotSize != null ?
                    "\nRecommended pot: " + carePlan.repotting.recommendedPotSize : ""));
        }

        if (carePlan.seasonal != null && !carePlan.seasonal.isEmpty()) {
            renderCarePlanItem(context, container, "Seasonal Notes", carePlan.seasonal);
        }
    }

    /**
     * Renders a single care plan item with title and content.
     */
    private static void renderCarePlanItem(Context context, LinearLayout container,
                                          String title, String content) {
        LinearLayout itemContainer = new LinearLayout(context);
        itemContainer.setOrientation(LinearLayout.VERTICAL);
        itemContainer.setPadding(0, 8, 0, 8);

        TextView titleTv = new TextView(context);
        titleTv.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge);
        titleTv.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        titleTv.setText(title);

        TextView contentTv = new TextView(context);
        contentTv.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        contentTv.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        contentTv.setText(content);

        itemContainer.addView(titleTv);
        itemContainer.addView(contentTv);
        container.addView(itemContainer);
    }

    /**
     * Gets severity emoji for health issues.
     * Pure helper function (no Android dependencies).
     */
    public static String getSeverityEmoji(String severity) {
        if (severity == null) return "-";
        switch (severity.toLowerCase()) {
            case "high": return "!";
            case "medium": return "*";
            default: return "-";
        }
    }

    /**
     * Gets priority prefix for immediate actions.
     * Pure helper function (no Android dependencies).
     */
    public static String getPriorityPrefix(String priority) {
        if (priority == null) return "- ";
        switch (priority.toLowerCase()) {
            case "urgent": return "[URGENT] ";
            case "soon": return "[Soon] ";
            default: return "- ";
        }
    }

    /**
     * Gets health score color resource ID.
     * Pure helper function returning resource ID.
     */
    public static int getHealthScoreColorRes(int score) {
        if (score >= 7) {
            return R.color.health_good;
        } else if (score >= 4) {
            return R.color.health_warning;
        } else {
            return R.color.health_bad;
        }
    }
}
