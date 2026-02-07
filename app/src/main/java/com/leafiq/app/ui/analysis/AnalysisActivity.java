package com.leafiq.app.ui.analysis;

import android.content.DialogInterface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.leafiq.app.R;
import com.leafiq.app.ai.AIProvider;
import com.leafiq.app.ai.AIProviderException;
import com.leafiq.app.ai.ClaudeProvider;
import com.leafiq.app.ai.GeminiProvider;
import com.leafiq.app.ai.OpenAIProvider;
import com.leafiq.app.ai.PerplexityProvider;
import com.leafiq.app.ai.PromptBuilder;
import com.leafiq.app.data.db.AppDatabase;
import com.leafiq.app.data.entity.Analysis;
import com.leafiq.app.data.entity.Plant;
import com.leafiq.app.data.model.PlantAnalysisResult;
import com.leafiq.app.util.ImageUtils;
import com.leafiq.app.util.KeystoreHelper;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AnalysisActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URI = "extra_image_uri";
    public static final String EXTRA_PLANT_ID = "extra_plant_id";

    private ImageView imagePreview;
    private View loadingContainer;
    private View resultsContainer;
    private View errorContainer;
    private TextView errorMessage;

    private TextView plantCommonName;
    private TextView plantScientificName;
    private TextView identificationNotes;
    private TextView healthScore;
    private TextView healthSummary;
    private TextView healthIssuesLabel;
    private LinearLayout issuesContainer;
    private MaterialCardView actionsCard;
    private LinearLayout actionsContainer;
    private LinearLayout carePlanContainer;
    private MaterialCardView funFactCard;
    private TextView funFactText;
    private MaterialButton saveButton;
    private MaterialButton retryButton;

    private Uri imageUri;
    private Uri localImageUri; // Persistent local copy
    private String plantId;
    private PlantAnalysisResult analysisResult;
    private String rawResponse;

    private KeystoreHelper keystoreHelper;
    private ExecutorService executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        keystoreHelper = new KeystoreHelper(this);
        executor = Executors.newSingleThreadExecutor();

        initViews();

        String uriString = getIntent().getStringExtra(EXTRA_IMAGE_URI);
        if (uriString != null) {
            imageUri = Uri.parse(uriString);
            Glide.with(this).load(imageUri).centerCrop().into(imagePreview);
            // Copy image to local storage immediately to preserve access
            copyImageToLocal();
        }

        plantId = getIntent().getStringExtra(EXTRA_PLANT_ID);

        saveButton.setOnClickListener(v -> savePlant());
        retryButton.setOnClickListener(v -> startAnalysis());

        // Analysis will be triggered after copyImageToLocal() completes
        // Don't start analysis here - avoid race condition
    }

    private void copyImageToLocal() {
        executor.execute(() -> {
            try {
                java.io.File cacheDir = new java.io.File(getCacheDir(), "temp_images");
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs();
                }

                String filename = "analysis_" + System.currentTimeMillis() + ".jpg";
                java.io.File localFile = new java.io.File(cacheDir, filename);

                try (java.io.InputStream in = getContentResolver().openInputStream(imageUri);
                     java.io.OutputStream out = new java.io.FileOutputStream(localFile)) {
                    if (in == null) {
                        runOnUiThread(() -> showError("Could not open image. Please try again."));
                        return;
                    }
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                }

                localImageUri = Uri.fromFile(localFile);
                android.util.Log.d("AnalysisActivity", "Image copied to: " + localImageUri);

                // After copy completes, check API key and start analysis
                runOnUiThread(() -> {
                    if (!keystoreHelper.hasApiKey()) {
                        promptForApiKey();
                    } else {
                        startAnalysis();
                    }
                });

            } catch (Exception e) {
                android.util.Log.e("AnalysisActivity", "Failed to copy image locally: " + e.getMessage());
                runOnUiThread(() -> showError("Failed to copy image: " + e.getMessage()));
            }
        });
    }

    private void initViews() {
        imagePreview = findViewById(R.id.image_preview);
        loadingContainer = findViewById(R.id.loading_container);
        resultsContainer = findViewById(R.id.results_container);
        errorContainer = findViewById(R.id.error_container);
        errorMessage = findViewById(R.id.error_message);

        plantCommonName = findViewById(R.id.plant_common_name);
        plantScientificName = findViewById(R.id.plant_scientific_name);
        identificationNotes = findViewById(R.id.identification_notes);
        healthScore = findViewById(R.id.health_score);
        healthSummary = findViewById(R.id.health_summary);
        healthIssuesLabel = findViewById(R.id.health_issues_label);
        issuesContainer = findViewById(R.id.issues_container);
        actionsCard = findViewById(R.id.actions_card);
        actionsContainer = findViewById(R.id.actions_container);
        carePlanContainer = findViewById(R.id.care_plan_container);
        funFactCard = findViewById(R.id.fun_fact_card);
        funFactText = findViewById(R.id.fun_fact_text);
        saveButton = findViewById(R.id.btn_save);
        retryButton = findViewById(R.id.btn_retry);
    }

    private void promptForApiKey() {
        String providerName = getProviderDisplayName();

        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint(R.string.enter_api_key);

        new AlertDialog.Builder(this)
            .setTitle(R.string.api_key)
            .setMessage("Enter your " + providerName + " API key to analyze plants.\n\nYou can change the provider in Settings.")
            .setView(input)
            .setPositiveButton(R.string.save, (dialog, which) -> {
                String key = input.getText().toString().trim();
                if (!key.isEmpty()) {
                    keystoreHelper.saveApiKey(key);
                    Toast.makeText(this, R.string.api_key_saved, Toast.LENGTH_SHORT).show();
                    startAnalysis();
                } else {
                    showError(getString(R.string.api_key_required));
                }
            })
            .setNegativeButton(R.string.cancel, (dialog, which) -> finish())
            .setCancelable(false)
            .show();
    }

    private String getProviderDisplayName() {
        String provider = keystoreHelper.getProvider();
        if (KeystoreHelper.PROVIDER_GEMINI.equals(provider)) {
            return "Gemini";
        } else if (KeystoreHelper.PROVIDER_CLAUDE.equals(provider)) {
            return "Claude";
        } else if (KeystoreHelper.PROVIDER_PERPLEXITY.equals(provider)) {
            return "Perplexity";
        } else {
            return "OpenAI";
        }
    }

    private AIProvider createProvider() {
        String provider = keystoreHelper.getProvider();
        String apiKey = keystoreHelper.getApiKey();

        if (KeystoreHelper.PROVIDER_GEMINI.equals(provider)) {
            return new GeminiProvider(apiKey);
        } else if (KeystoreHelper.PROVIDER_CLAUDE.equals(provider)) {
            return new ClaudeProvider(apiKey);
        } else if (KeystoreHelper.PROVIDER_PERPLEXITY.equals(provider)) {
            return new PerplexityProvider(apiKey);
        } else {
            return new OpenAIProvider(apiKey);
        }
    }

    private void startAnalysis() {
        showLoading();

        executor.execute(() -> {
            try {
                // Use local copy if available, otherwise fall back to original URI
                Uri uriToUse = localImageUri != null ? localImageUri : imageUri;
                String base64Image = ImageUtils.prepareForApi(this, uriToUse);

                List<Analysis> previousAnalyses = null;
                String knownPlantName = null;

                if (plantId != null) {
                    AppDatabase db = AppDatabase.getInstance(this);
                    Plant existingPlant = db.plantDao().getPlantByIdSync(plantId);
                    if (existingPlant != null) {
                        knownPlantName = existingPlant.commonName;
                        previousAnalyses = db.analysisDao().getRecentAnalysesSync(plantId);
                    }
                }

                String prompt = PromptBuilder.buildAnalysisPrompt(knownPlantName, previousAnalyses, null);

                AIProvider provider = createProvider();
                analysisResult = provider.analyzePhoto(base64Image, prompt);

                runOnUiThread(this::displayResults);

            } catch (IOException e) {
                runOnUiThread(() -> showError("Failed to process image: " + e.getMessage()));
            } catch (AIProviderException e) {
                runOnUiThread(() -> showError("Analysis failed: " + e.getMessage()));
            }
        });
    }

    private void displayResults() {
        resultsContainer.setVisibility(View.VISIBLE);
        loadingContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);

        if (analysisResult.identification != null) {
            plantCommonName.setText(analysisResult.identification.commonName);
            plantScientificName.setText(analysisResult.identification.scientificName);
            if (analysisResult.identification.notes != null && !analysisResult.identification.notes.isEmpty()) {
                identificationNotes.setText(analysisResult.identification.notes);
                identificationNotes.setVisibility(View.VISIBLE);
            }
        }

        if (analysisResult.healthAssessment != null) {
            int score = analysisResult.healthAssessment.score;
            healthScore.setText(String.valueOf(score));
            setHealthScoreColor(score);
            healthSummary.setText(analysisResult.healthAssessment.summary);

            if (analysisResult.healthAssessment.issues != null && !analysisResult.healthAssessment.issues.isEmpty()) {
                healthIssuesLabel.setVisibility(View.VISIBLE);
                issuesContainer.removeAllViews();
                for (PlantAnalysisResult.HealthAssessment.Issue issue : analysisResult.healthAssessment.issues) {
                    addIssueView(issue);
                }
            }
        }

        if (analysisResult.immediateActions != null && !analysisResult.immediateActions.isEmpty()) {
            actionsCard.setVisibility(View.VISIBLE);
            actionsContainer.removeAllViews();
            for (PlantAnalysisResult.ImmediateAction action : analysisResult.immediateActions) {
                addActionView(action);
            }
        }

        if (analysisResult.carePlan != null) {
            carePlanContainer.removeAllViews();
            addCarePlanSection(analysisResult.carePlan);
        }

        if (analysisResult.funFact != null && !analysisResult.funFact.isEmpty()) {
            funFactCard.setVisibility(View.VISIBLE);
            funFactText.setText(analysisResult.funFact);
        }
    }

    private void setHealthScoreColor(int score) {
        int colorRes;
        if (score >= 7) {
            colorRes = R.color.health_good;
        } else if (score >= 4) {
            colorRes = R.color.health_warning;
        } else {
            colorRes = R.color.health_bad;
        }
        ((GradientDrawable) healthScore.getBackground()).setColor(
            ContextCompat.getColor(this, colorRes));
    }

    private void addIssueView(PlantAnalysisResult.HealthAssessment.Issue issue) {
        TextView tv = new TextView(this);
        tv.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        tv.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        tv.setPadding(0, 8, 0, 8);

        String severityEmoji = getSeverityEmoji(issue.severity);
        tv.setText(severityEmoji + " " + issue.name + ": " + issue.description);

        issuesContainer.addView(tv);
    }

    private String getSeverityEmoji(String severity) {
        if (severity == null) return "-";
        switch (severity.toLowerCase()) {
            case "high": return "!";
            case "medium": return "*";
            default: return "-";
        }
    }

    private void addActionView(PlantAnalysisResult.ImmediateAction action) {
        TextView tv = new TextView(this);
        tv.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        tv.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        tv.setPadding(0, 8, 0, 8);

        String priorityPrefix = getPriorityPrefix(action.priority);
        tv.setText(priorityPrefix + action.action + "\n   " + action.detail);

        actionsContainer.addView(tv);
    }

    private String getPriorityPrefix(String priority) {
        if (priority == null) return "- ";
        switch (priority.toLowerCase()) {
            case "urgent": return "[URGENT] ";
            case "soon": return "[Soon] ";
            default: return "- ";
        }
    }

    private void addCarePlanSection(PlantAnalysisResult.CarePlan carePlan) {
        if (carePlan.watering != null) {
            addCarePlanItem("Watering",
                carePlan.watering.frequency + " - " + carePlan.watering.amount +
                (carePlan.watering.notes != null ? "\n" + carePlan.watering.notes : ""));
        }

        if (carePlan.light != null) {
            addCarePlanItem("Light",
                "Ideal: " + carePlan.light.ideal +
                (carePlan.light.adjustment != null ? "\nAdjustment: " + carePlan.light.adjustment : ""));
        }

        if (carePlan.fertilizer != null) {
            addCarePlanItem("Fertilizer",
                carePlan.fertilizer.type + " - " + carePlan.fertilizer.frequency);
        }

        if (carePlan.pruning != null && carePlan.pruning.needed) {
            addCarePlanItem("Pruning", carePlan.pruning.instructions);
        }

        if (carePlan.repotting != null && carePlan.repotting.needed) {
            addCarePlanItem("Repotting",
                carePlan.repotting.signs +
                (carePlan.repotting.recommendedPotSize != null ?
                    "\nRecommended pot: " + carePlan.repotting.recommendedPotSize : ""));
        }

        if (carePlan.seasonal != null && !carePlan.seasonal.isEmpty()) {
            addCarePlanItem("Seasonal Notes", carePlan.seasonal);
        }
    }

    private void addCarePlanItem(String title, String content) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, 8, 0, 8);

        TextView titleTv = new TextView(this);
        titleTv.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge);
        titleTv.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        titleTv.setText(title);

        TextView contentTv = new TextView(this);
        contentTv.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        contentTv.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        contentTv.setText(content);

        container.addView(titleTv);
        container.addView(contentTv);
        carePlanContainer.addView(container);
    }

    private void showLoading() {
        loadingContainer.setVisibility(View.VISIBLE);
        resultsContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
    }

    private void showError(String message) {
        loadingContainer.setVisibility(View.GONE);
        resultsContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.VISIBLE);
        errorMessage.setText(message);
    }

    private void savePlant() {
        if (analysisResult == null) return;

        // Use local copy if available, otherwise try original URI
        Uri uriToSave = localImageUri != null ? localImageUri : imageUri;
        if (uriToSave == null) {
            Toast.makeText(this, "No image to save", Toast.LENGTH_SHORT).show();
            return;
        }

        saveButton.setEnabled(false);
        saveButton.setText("Saving...");

        executor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(this);
                long now = System.currentTimeMillis();

                // Create or update plant
                Plant plant;
                boolean isNewPlant = (plantId == null);

                if (isNewPlant) {
                    plantId = UUID.randomUUID().toString();
                    plant = new Plant();
                    plant.id = plantId;
                    plant.createdAt = now;
                } else {
                    plant = db.plantDao().getPlantByIdSync(plantId);
                    if (plant == null) {
                        plant = new Plant();
                        plant.id = plantId;
                        plant.createdAt = now;
                    }
                }

                plant.commonName = analysisResult.identification != null ?
                    analysisResult.identification.commonName : "Unknown";
                plant.scientificName = analysisResult.identification != null ?
                    analysisResult.identification.scientificName : "";
                plant.latestHealthScore = analysisResult.healthAssessment != null ?
                    analysisResult.healthAssessment.score : 5;
                plant.updatedAt = now;

                // Save thumbnail and photo using local copy
                String thumbnailPath = null;
                String photoPath = null;
                try {
                    thumbnailPath = ImageUtils.saveThumbnail(this, uriToSave, plantId);
                    photoPath = ImageUtils.savePhoto(this, uriToSave, plantId);
                } catch (IOException e) {
                    // If image save fails, continue without thumbnail
                    android.util.Log.e("AnalysisActivity", "Failed to save image: " + e.getMessage());
                }
                plant.thumbnailPath = thumbnailPath;

                db.plantDao().insertPlant(plant);

                // Save analysis
                Analysis analysis = new Analysis();
                analysis.id = UUID.randomUUID().toString();
                analysis.plantId = plantId;
                analysis.photoPath = photoPath;
                analysis.healthScore = plant.latestHealthScore;
                analysis.summary = analysisResult.healthAssessment != null ?
                    analysisResult.healthAssessment.summary : "";
                analysis.createdAt = now;

                db.analysisDao().insertAnalysis(analysis);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Plant saved to library!", Toast.LENGTH_SHORT).show();
                    finish();
                });

            } catch (Exception e) {
                android.util.Log.e("AnalysisActivity", "Save failed", e);
                runOnUiThread(() -> {
                    saveButton.setEnabled(true);
                    saveButton.setText(R.string.save_to_library);
                    Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
        // Cleanup temporary image file
        if (localImageUri != null) {
            java.io.File tempFile = new java.io.File(localImageUri.getPath());
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}
