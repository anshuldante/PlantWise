package com.leafiq.app.ui.timeline;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.leafiq.app.LeafIQApplication;
import com.leafiq.app.R;
import com.leafiq.app.data.entity.Analysis;
import com.leafiq.app.data.entity.Plant;
import com.leafiq.app.data.model.PlantAnalysisResult;
import com.leafiq.app.data.repository.PlantRepository;
import com.leafiq.app.util.AppExecutors;
import com.leafiq.app.util.HealthUtils;
import com.leafiq.app.util.JsonParser;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Analysis Detail Activity - read-only analysis report screen.
 * <p>
 * Displays complete analysis information including:
 * - Original photo
 * - Identified species with confidence
 * - Health assessment and diagnosis
 * - Complete care plan
 * - Environmental context
 * - Analysis timestamp and provider info
 * <p>
 * Opened from:
 * - Global timeline (Plan 03)
 * - Per-plant analysis history (Plan 05)
 * <p>
 * Includes "Correct this" action for updating plant name and health score.
 * Does NOT include editing, navigation to other analyses, or re-analysis actions.
 */
public class AnalysisDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ANALYSIS_ID = "extra_analysis_id";
    public static final String EXTRA_PLANT_ID = "extra_plant_id";

    private ImageView analysisPhoto;
    private TextView plantName;
    private TextView scientificName;
    private TextView confidenceBadge;
    private TextView healthBadge;
    private TextView diagnosisText;
    private MaterialCardView carePlanCard;
    private TextView carePlanText;
    private MaterialCardView environmentalContextCard;
    private TextView assumptionsText;
    private TextView analysisTimestamp;
    private TextView analysisProvider;
    private MaterialButton btnCorrect;

    private String analysisId;
    private String plantId;
    private Analysis currentAnalysis;
    private Plant currentPlant;
    private PlantRepository repository;
    private AppExecutors executors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis_detail);

        // Get intent extras
        analysisId = getIntent().getStringExtra(EXTRA_ANALYSIS_ID);
        plantId = getIntent().getStringExtra(EXTRA_PLANT_ID);

        if (analysisId == null || plantId == null) {
            finish();
            return;
        }

        initViews();
        setupToolbar();

        // Get repository and executors from Application
        LeafIQApplication app = (LeafIQApplication) getApplication();
        repository = app.getPlantRepository();
        executors = app.getAppExecutors();

        // Load data on background thread
        loadData();

        // Setup "Correct this" button
        btnCorrect.setOnClickListener(v -> showCorrectionDialog());
    }

    private void initViews() {
        analysisPhoto = findViewById(R.id.analysis_photo);
        plantName = findViewById(R.id.plant_name);
        scientificName = findViewById(R.id.scientific_name);
        confidenceBadge = findViewById(R.id.confidence_badge);
        healthBadge = findViewById(R.id.health_badge);
        diagnosisText = findViewById(R.id.diagnosis_text);
        carePlanCard = findViewById(R.id.care_plan_card);
        carePlanText = findViewById(R.id.care_plan_text);
        environmentalContextCard = findViewById(R.id.environmental_context_card);
        assumptionsText = findViewById(R.id.assumptions_text);
        analysisTimestamp = findViewById(R.id.analysis_timestamp);
        analysisProvider = findViewById(R.id.analysis_provider);
        btnCorrect = findViewById(R.id.btn_correct);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Loads analysis and plant data on background thread.
     */
    private void loadData() {
        executors.io().execute(() -> {
            try {
                // Load analysis and plant from database
                currentAnalysis = repository.getAnalysisByIdSync(analysisId);
                currentPlant = repository.getPlantByIdSync(plantId);

                // Parse raw response if available
                PlantAnalysisResult parsedResult = null;
                if (currentAnalysis != null && currentAnalysis.rawResponse != null
                        && !currentAnalysis.rawResponse.isEmpty()) {
                    try {
                        parsedResult = JsonParser.parsePlantAnalysis(currentAnalysis.rawResponse);
                    } catch (Exception e) {
                        // If parsing fails, we'll use fallback data from Analysis entity
                    }
                }

                // Post to main thread to update UI
                final PlantAnalysisResult finalParsedResult = parsedResult;
                runOnUiThread(() -> displayAnalysis(finalParsedResult));

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Failed to load analysis", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    /**
     * Displays analysis data in UI.
     * Called on main thread.
     *
     * @param parsedResult Parsed PlantAnalysisResult (may be null if parsing failed)
     */
    private void displayAnalysis(PlantAnalysisResult parsedResult) {
        if (currentAnalysis == null || currentPlant == null) {
            return;
        }

        // Photo
        if (currentAnalysis.photoPath != null && !currentAnalysis.photoPath.isEmpty()) {
            Glide.with(this)
                    .load(new File(currentAnalysis.photoPath))
                    .centerCrop()
                    .into(analysisPhoto);
        }

        // Plant name (nickname takes precedence, then common name, fallback to "Unknown Plant")
        String displayName = "Unknown Plant";
        if (currentPlant.nickname != null && !currentPlant.nickname.isEmpty()) {
            displayName = currentPlant.nickname;
        } else if (currentPlant.commonName != null && !currentPlant.commonName.isEmpty()) {
            displayName = currentPlant.commonName;
        }
        plantName.setText(displayName);

        // Scientific name (from parsed result if available, else from plant entity)
        String scientificNameText = "";
        if (parsedResult != null && parsedResult.identification != null
                && parsedResult.identification.scientificName != null
                && !parsedResult.identification.scientificName.isEmpty()) {
            scientificNameText = parsedResult.identification.scientificName;
        } else if (currentPlant.scientificName != null && !currentPlant.scientificName.isEmpty()) {
            scientificNameText = currentPlant.scientificName;
        }

        if (!scientificNameText.isEmpty()) {
            scientificName.setText(scientificNameText);
            scientificName.setVisibility(View.VISIBLE);
        } else {
            scientificName.setVisibility(View.GONE);
        }

        // Confidence badge (from parsed result if available)
        if (parsedResult != null && parsedResult.identification != null
                && parsedResult.identification.confidence != null
                && !parsedResult.identification.confidence.isEmpty()) {
            String confidenceText = parsedResult.identification.confidence.substring(0, 1).toUpperCase()
                    + parsedResult.identification.confidence.substring(1) + " confidence";
            confidenceBadge.setText(confidenceText);
            confidenceBadge.setVisibility(View.VISIBLE);
        } else {
            confidenceBadge.setVisibility(View.GONE);
        }

        // Health badge (text label with color)
        String healthLabel = HealthUtils.getHealthLabel(currentAnalysis.healthScore);
        int healthColorRes = HealthUtils.getHealthColorRes(currentAnalysis.healthScore);
        healthBadge.setText(healthLabel);

        // Set health badge background color using GradientDrawable
        GradientDrawable healthBackground = (GradientDrawable) healthBadge.getBackground();
        healthBackground.setColor(ContextCompat.getColor(this, healthColorRes));

        // Diagnosis (from parsed result if available, else from analysis summary)
        String diagnosisTextContent = "";
        if (parsedResult != null && parsedResult.healthAssessment != null
                && parsedResult.healthAssessment.summary != null
                && !parsedResult.healthAssessment.summary.isEmpty()) {
            diagnosisTextContent = parsedResult.healthAssessment.summary;
        } else if (currentAnalysis.summary != null && !currentAnalysis.summary.isEmpty()) {
            diagnosisTextContent = currentAnalysis.summary;
        }

        if (!diagnosisTextContent.isEmpty()) {
            diagnosisText.setText(diagnosisTextContent);
        } else {
            diagnosisText.setText("No diagnosis available");
        }

        // Care plan (format from parsed result)
        if (parsedResult != null && parsedResult.carePlan != null) {
            String carePlanFormatted = formatCarePlan(parsedResult.carePlan);
            if (!carePlanFormatted.isEmpty()) {
                carePlanText.setText(carePlanFormatted);
                carePlanCard.setVisibility(View.VISIBLE);
            } else {
                carePlanCard.setVisibility(View.GONE);
            }
        } else {
            carePlanCard.setVisibility(View.GONE);
        }

        // Environmental context (location from plant)
        if (currentPlant.location != null && !currentPlant.location.isEmpty()) {
            assumptionsText.setText("Location: " + currentPlant.location);
            environmentalContextCard.setVisibility(View.VISIBLE);
        } else {
            environmentalContextCard.setVisibility(View.GONE);
        }

        // Timestamp
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault());
        String timestampText = "Analyzed on " + dateFormat.format(new Date(currentAnalysis.createdAt));
        analysisTimestamp.setText(timestampText);

        // Provider info (hide for now - would need to parse from rawResponse metadata)
        analysisProvider.setVisibility(View.GONE);
    }

    /**
     * Formats care plan from PlantAnalysisResult.CarePlan into readable text.
     *
     * @param carePlan Care plan object
     * @return Formatted care plan string
     */
    private String formatCarePlan(PlantAnalysisResult.CarePlan carePlan) {
        StringBuilder sb = new StringBuilder();

        // Watering
        if (carePlan.watering != null) {
            sb.append("💧 Watering\n");
            if (carePlan.watering.frequency != null && !carePlan.watering.frequency.isEmpty()) {
                sb.append("Frequency: ").append(carePlan.watering.frequency).append("\n");
            }
            if (carePlan.watering.amount != null && !carePlan.watering.amount.isEmpty()) {
                sb.append("Amount: ").append(carePlan.watering.amount).append("\n");
            }
            if (carePlan.watering.notes != null && !carePlan.watering.notes.isEmpty()) {
                sb.append(carePlan.watering.notes).append("\n");
            }
            sb.append("\n");
        }

        // Light
        if (carePlan.light != null) {
            sb.append("☀️ Light\n");
            if (carePlan.light.ideal != null && !carePlan.light.ideal.isEmpty()) {
                sb.append("Ideal: ").append(carePlan.light.ideal).append("\n");
            }
            if (carePlan.light.current != null && !carePlan.light.current.isEmpty()) {
                sb.append("Current: ").append(carePlan.light.current).append("\n");
            }
            if (carePlan.light.adjustment != null && !carePlan.light.adjustment.isEmpty()) {
                sb.append("Adjustment: ").append(carePlan.light.adjustment).append("\n");
            }
            sb.append("\n");
        }

        // Fertilizer
        if (carePlan.fertilizer != null) {
            sb.append("🌱 Fertilizer\n");
            if (carePlan.fertilizer.type != null && !carePlan.fertilizer.type.isEmpty()) {
                sb.append("Type: ").append(carePlan.fertilizer.type).append("\n");
            }
            if (carePlan.fertilizer.frequency != null && !carePlan.fertilizer.frequency.isEmpty()) {
                sb.append("Frequency: ").append(carePlan.fertilizer.frequency).append("\n");
            }
            if (carePlan.fertilizer.nextApplication != null && !carePlan.fertilizer.nextApplication.isEmpty()) {
                sb.append("Next: ").append(carePlan.fertilizer.nextApplication).append("\n");
            }
            sb.append("\n");
        }

        // Pruning
        if (carePlan.pruning != null && carePlan.pruning.needed) {
            sb.append("✂️ Pruning\n");
            if (carePlan.pruning.instructions != null && !carePlan.pruning.instructions.isEmpty()) {
                sb.append(carePlan.pruning.instructions).append("\n");
            }
            if (carePlan.pruning.when != null && !carePlan.pruning.when.isEmpty()) {
                sb.append("When: ").append(carePlan.pruning.when).append("\n");
            }
            sb.append("\n");
        }

        // Repotting
        if (carePlan.repotting != null && carePlan.repotting.needed) {
            sb.append("🪴 Repotting\n");
            if (carePlan.repotting.signs != null && !carePlan.repotting.signs.isEmpty()) {
                sb.append("Signs: ").append(carePlan.repotting.signs).append("\n");
            }
            if (carePlan.repotting.recommendedPotSize != null && !carePlan.repotting.recommendedPotSize.isEmpty()) {
                sb.append("Pot size: ").append(carePlan.repotting.recommendedPotSize).append("\n");
            }
            sb.append("\n");
        }

        // Seasonal notes
        if (carePlan.seasonal != null && !carePlan.seasonal.isEmpty()) {
            sb.append("📅 Seasonal Care\n");
            sb.append(carePlan.seasonal).append("\n");
        }

        return sb.toString().trim();
    }

    /**
     * Shows correction dialog for user to provide corrections to analysis.
     * Only allows "Save" from detail screen (no re-analysis, no additional context field).
     * For re-analysis, user should use the camera FAB on plant detail screen.
     */
    private void showCorrectionDialog() {
        if (currentPlant == null || currentAnalysis == null) return;

        // Inflate dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_correction, null);
        TextInputEditText nameInput = dialogView.findViewById(R.id.correct_name_input);
        TextInputEditText healthInput = dialogView.findViewById(R.id.correct_health_input);
        TextInputEditText contextInput = dialogView.findViewById(R.id.additional_context_input);

        // Pre-fill current values
        if (currentPlant.commonName != null) {
            nameInput.setText(currentPlant.commonName);
        }
        healthInput.setText(String.valueOf(currentAnalysis.healthScore));

        // Hide additional context field (not used for analysis detail correction)
        contextInput.setVisibility(View.GONE);
        ((View) contextInput.getParent().getParent()).setVisibility(View.GONE);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Correct Analysis")
                .setView(dialogView)
                .setPositiveButton("Save", null) // Set to null to override later
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            // Override positive button to validate before closing
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String correctedName = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
                String healthText = healthInput.getText() != null ? healthInput.getText().toString().trim() : "";

                // Validate health score
                int correctedHealth = 0;
                if (!healthText.isEmpty()) {
                    try {
                        correctedHealth = Integer.parseInt(healthText);
                        if (correctedHealth < 1 || correctedHealth > 10) {
                            Toast.makeText(this, "Health score must be between 1 and 10", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid health score", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                // Check if anything changed
                String originalName = currentPlant.commonName != null ? currentPlant.commonName : "";
                int originalHealth = currentAnalysis.healthScore;
                boolean nameChanged = !correctedName.isEmpty() && !correctedName.equals(originalName);
                boolean healthChanged = correctedHealth > 0 && correctedHealth != originalHealth;

                if (!nameChanged && !healthChanged) {
                    Toast.makeText(this, "No changes to save", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Close dialog
                dialog.dismiss();

                // Update plant name if changed
                if (nameChanged) {
                    currentPlant.commonName = correctedName;
                    currentPlant.updatedAt = System.currentTimeMillis();
                    repository.updatePlant(currentPlant, new PlantRepository.RepositoryCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            runOnUiThread(() -> {
                                // Update plant name display
                                String displayName = currentPlant.nickname != null && !currentPlant.nickname.isEmpty()
                                        ? currentPlant.nickname : currentPlant.commonName;
                                plantName.setText(displayName != null ? displayName : "Unknown Plant");
                                Toast.makeText(AnalysisDetailActivity.this, "Plant name updated", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            runOnUiThread(() ->
                                    Toast.makeText(AnalysisDetailActivity.this, "Failed to update plant name", Toast.LENGTH_SHORT).show()
                            );
                        }
                    });
                }

                // Update analysis health if changed
                if (healthChanged) {
                    final int finalCorrectedHealth = correctedHealth;
                    currentAnalysis.healthScore = finalCorrectedHealth;
                    currentPlant.latestHealthScore = finalCorrectedHealth;
                    currentPlant.updatedAt = System.currentTimeMillis();

                    // Update analysis
                    repository.updateAnalysis(currentAnalysis, new PlantRepository.RepositoryCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            // Also update plant's latest health score
                            repository.updatePlant(currentPlant, new PlantRepository.RepositoryCallback<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    runOnUiThread(() -> {
                                        // Update health badge
                                        String healthLabel = HealthUtils.getHealthLabel(finalCorrectedHealth);
                                        int healthColorRes = HealthUtils.getHealthColorRes(finalCorrectedHealth);
                                        healthBadge.setText(healthLabel);

                                        GradientDrawable healthBackground = (GradientDrawable) healthBadge.getBackground();
                                        healthBackground.setColor(ContextCompat.getColor(AnalysisDetailActivity.this, healthColorRes));

                                        Toast.makeText(AnalysisDetailActivity.this, "Health score updated", Toast.LENGTH_SHORT).show();
                                    });
                                }

                                @Override
                                public void onError(Exception e) {
                                    runOnUiThread(() ->
                                            Toast.makeText(AnalysisDetailActivity.this, "Failed to update plant", Toast.LENGTH_SHORT).show()
                                    );
                                }
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            runOnUiThread(() ->
                                    Toast.makeText(AnalysisDetailActivity.this, "Failed to update analysis", Toast.LENGTH_SHORT).show()
                            );
                        }
                    });
                }
            });
        });

        dialog.show();
    }
}
