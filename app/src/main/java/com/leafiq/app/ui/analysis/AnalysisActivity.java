package com.leafiq.app.ui.analysis;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.leafiq.app.R;
import com.leafiq.app.data.entity.CareSchedule;
import com.leafiq.app.data.model.PlantAnalysisResult;
import com.leafiq.app.util.KeystoreHelper;
import com.leafiq.app.util.PhotoQualityChecker;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * UI-only Activity for plant analysis screen.
 * Observes ViewModel state via LiveData and renders UI accordingly.
 * <p>
 * All business logic delegated to AnalysisViewModel:
 * - Analysis: viewModel.analyzeImage()
 * - Saving: viewModel.savePlant()
 * <p>
 * Activity responsibilities:
 * - View initialization and rendering
 * - Image preview loading
 * - User interaction handling
 * - API key prompting
 * - Temporary file management
 */
public class AnalysisActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URI = "extra_image_uri";
    public static final String EXTRA_PLANT_ID = "extra_plant_id";
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;

    private ImageView imagePreview;
    private View photoTipsContainer;
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
    private MaterialButton correctButton;
    private MaterialButton retryButton;

    private Uri imageUri;
    private Uri localImageUri; // Persistent local copy
    private String plantId;
    private PlantAnalysisResult analysisResult;

    private KeystoreHelper keystoreHelper;
    private AnalysisViewModel viewModel;
    private ExecutorService executor; // For image copy only

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        // Initialize KeystoreHelper for API key checks
        keystoreHelper = new KeystoreHelper(this);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this,
                new AnalysisViewModelFactory(getApplication()))
                .get(AnalysisViewModel.class);

        // Initialize executor for image copy (UI-layer file I/O)
        executor = Executors.newSingleThreadExecutor();

        initViews();

        // Observe UI state from ViewModel
        viewModel.getUiState().observe(this, this::onUiStateChanged);

        // Observe schedule update prompts for re-analysis conflicts
        viewModel.getScheduleUpdatePrompts().observe(this, this::onScheduleUpdatePrompts);

        // Load image URI from intent
        String uriString = getIntent().getStringExtra(EXTRA_IMAGE_URI);
        if (uriString != null) {
            imageUri = Uri.parse(uriString);
            Glide.with(this).load(imageUri).centerCrop().into(imagePreview);
            // Copy image to local storage immediately to preserve access
            copyImageToLocal();
        }

        plantId = getIntent().getStringExtra(EXTRA_PLANT_ID);

        // Set up button click listeners
        saveButton.setOnClickListener(v -> handleSaveClick());
        correctButton.setOnClickListener(v -> showCorrectionDialog());
        retryButton.setOnClickListener(v -> handleRetryClick());

        // Analysis will be triggered after copyImageToLocal() completes
    }

    private void initViews() {
        imagePreview = findViewById(R.id.image_preview);
        photoTipsContainer = findViewById(R.id.photo_tips_container);
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
        correctButton = findViewById(R.id.btn_correct);
        retryButton = findViewById(R.id.btn_retry);
    }

    /**
     * Handles UI state changes from ViewModel.
     * Called whenever ViewModel's LiveData<AnalysisUiState> emits a new state.
     */
    private void onUiStateChanged(AnalysisUiState state) {
        switch (state.getState()) {
            case IDLE:
                // Initial state, nothing to show
                break;
            case LOADING:
                showLoading();
                break;
            case SUCCESS:
                photoTipsContainer.setVisibility(View.GONE);
                analysisResult = state.getResult();
                displayResults();
                showQuickDiagnosisTooltipIfNeeded();
                break;
            case ERROR:
                photoTipsContainer.setVisibility(View.GONE);
                if (state.isVisionUnsupported()) {
                    showVisionNotSupportedDialog(state.getVisionUnsupportedProvider());
                } else {
                    showError(state.getErrorMessage());
                }
                break;
        }
    }

    /**
     * Copies image to local storage to preserve access after camera intent completes.
     * UI-layer concern (file I/O for temporary image).
     */
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

                // After copy completes, validate photo quality
                runOnUiThread(() -> validatePhotoQuality());

            } catch (Exception e) {
                android.util.Log.e("AnalysisActivity", "Failed to copy image locally: " + e.getMessage());
                runOnUiThread(() -> showError("Failed to copy image: " + e.getMessage()));
            }
        });
    }

    /**
     * Starts analysis by delegating to ViewModel.
     */
    private void startAnalysis() {
        Uri uriToUse = localImageUri != null ? localImageUri : imageUri;
        viewModel.analyzeImage(uriToUse, plantId);
    }

    /**
     * Shows dialog prompting user for API key.
     */
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

    /**
     * Gets display name for current provider.
     */
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

    /**
     * Shows dialog when provider doesn't support vision.
     */
    private void showVisionNotSupportedDialog(String providerName) {
        new AlertDialog.Builder(this)
            .setTitle("Image Analysis Not Supported")
            .setMessage(providerName + " doesn't support image analysis. "
                + "Please describe your plant or switch to another provider.")
            .setPositiveButton("OK", (dialog, which) -> {
                // Let user stay on screen - they can retry with different provider
                showError(providerName + " is text-only. "
                    + "Switch to Claude, ChatGPT, or Gemini in Settings for image analysis.");
            })
            .setNeutralButton("Open Settings", (dialog, which) -> {
                // Navigate back so user can access settings
                finish();
            })
            .setCancelable(false)
            .show();
    }

    /**
     * Handles save button click.
     */
    private void handleSaveClick() {
        if (analysisResult == null) return;

        Uri uriToSave = localImageUri != null ? localImageUri : imageUri;
        if (uriToSave == null) {
            Toast.makeText(this, "No image to save", Toast.LENGTH_SHORT).show();
            return;
        }

        saveButton.setEnabled(false);
        saveButton.setText("Saving...");

        viewModel.savePlant(uriToSave, plantId, analysisResult, new AnalysisViewModel.SaveCallback() {
            @Override
            public void onSuccess(String savedPlantId) {
                runOnUiThread(() -> {
                    Toast.makeText(AnalysisActivity.this, "Plant saved to library!", Toast.LENGTH_SHORT).show();

                    // Request notification permission on first save (Android 13+)
                    requestNotificationPermissionIfNeeded();

                    finish();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    saveButton.setEnabled(true);
                    saveButton.setText(R.string.save_to_library);
                    Toast.makeText(AnalysisActivity.this, "Failed to save: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Handles retry button click.
     */
    private void handleRetryClick() {
        Uri uriToUse = localImageUri != null ? localImageUri : imageUri;
        viewModel.analyzeImage(uriToUse, plantId);
    }

    /**
     * Displays analysis results in UI.
     */
    private void displayResults() {
        resultsContainer.setVisibility(View.VISIBLE);
        loadingContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
        correctButton.setVisibility(View.VISIBLE);

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

    /**
     * Validates photo quality before starting analysis.
     * Checks brightness, blur, and resolution on background thread.
     */
    private void validatePhotoQuality() {
        executor.execute(() -> {
            try {
                Uri uriToCheck = localImageUri != null ? localImageUri : imageUri;
                PhotoQualityChecker.QualityResult result =
                    PhotoQualityChecker.checkQuality(getContentResolver(), uriToCheck);

                runOnUiThread(() -> {
                    if (result.passed) {
                        proceedToAnalysis();
                    } else {
                        showQualityWarning(result.message);
                    }
                });
            } catch (Exception e) {
                // If quality check fails, proceed anyway (don't block on check failure)
                runOnUiThread(() -> proceedToAnalysis());
            }
        });
    }

    /**
     * Proceeds to analysis after quality check passes or is skipped.
     * Checks API key first, then starts analysis.
     */
    private void proceedToAnalysis() {
        if (!keystoreHelper.hasApiKey()) {
            promptForApiKey();
        } else {
            startAnalysis();
        }
    }

    /**
     * Shows quality warning dialog with override option.
     * @param message Specific quality issue message
     */
    private void showQualityWarning(String message) {
        new AlertDialog.Builder(this)
            .setTitle("Photo Quality Issue")
            .setMessage(message)
            .setPositiveButton("Use Anyway", (d, w) -> proceedToAnalysis())
            .setNegativeButton("Choose Different Photo", (d, w) -> finish())
            .setCancelable(false)
            .show();
    }

    /**
     * Shows correction dialog for user to provide corrections to AI analysis.
     * Allows re-analysis with corrections or saving field corrections without re-analysis.
     */
    private void showCorrectionDialog() {
        if (analysisResult == null) return;

        // Inflate dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_correction, null);
        com.google.android.material.textfield.TextInputEditText nameInput = dialogView.findViewById(R.id.correct_name_input);
        com.google.android.material.textfield.TextInputEditText healthInput = dialogView.findViewById(R.id.correct_health_input);
        com.google.android.material.textfield.TextInputEditText contextInput = dialogView.findViewById(R.id.additional_context_input);

        // Pre-fill current values
        if (analysisResult.identification != null && analysisResult.identification.commonName != null) {
            nameInput.setText(analysisResult.identification.commonName);
        }
        if (analysisResult.healthAssessment != null) {
            healthInput.setText(String.valueOf(analysisResult.healthAssessment.score));
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Correct Analysis")
            .setView(dialogView)
            .setPositiveButton("Re-analyze", null) // Set to null to override later
            .setNeutralButton("Save Without Re-analysis", null)
            .setNegativeButton("Cancel", null)
            .create();

        dialog.setOnShowListener(dialogInterface -> {
            // Override positive button to validate before closing
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String correctedName = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
                String healthText = healthInput.getText() != null ? healthInput.getText().toString().trim() : "";
                String additionalContext = contextInput.getText() != null ? contextInput.getText().toString().trim() : "";

                // Validate health score if provided
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

                // Check if name changed (different from original)
                String originalName = analysisResult.identification != null ? analysisResult.identification.commonName : "";
                boolean nameChanged = !correctedName.equals(originalName);

                // Check if there are any corrections to apply
                if (!nameChanged && additionalContext.isEmpty()) {
                    Toast.makeText(this, "Please provide corrections or additional context", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Close dialog
                dialog.dismiss();

                // Start re-analysis with corrections
                Uri uriToUse = localImageUri != null ? localImageUri : imageUri;
                String nameToSend = nameChanged ? correctedName : null;
                viewModel.reanalyzeWithCorrections(uriToUse, plantId, nameToSend, additionalContext.isEmpty() ? null : additionalContext);
            });

            // Override neutral button for save without re-analysis
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                String correctedName = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
                String healthText = healthInput.getText() != null ? healthInput.getText().toString().trim() : "";

                // Validate health score if provided
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
                String originalName = analysisResult.identification != null ? analysisResult.identification.commonName : "";
                int originalHealth = analysisResult.healthAssessment != null ? analysisResult.healthAssessment.score : 0;
                boolean nameChanged = !correctedName.isEmpty() && !correctedName.equals(originalName);
                boolean healthChanged = correctedHealth > 0 && correctedHealth != originalHealth;

                if (!nameChanged && !healthChanged) {
                    // Check if user only provided additional context
                    String additionalContext = contextInput.getText() != null ? contextInput.getText().toString().trim() : "";
                    if (!additionalContext.isEmpty()) {
                        Toast.makeText(this, "Additional context requires re-analysis. Use 'Re-analyze' instead.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "No changes to save", Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                    return;
                }

                // Close dialog
                dialog.dismiss();

                // Update displayed values immediately
                if (nameChanged) {
                    plantCommonName.setText(correctedName);
                    if (analysisResult.identification != null) {
                        analysisResult.identification.commonName = correctedName;
                    }
                }
                if (healthChanged) {
                    healthScore.setText(String.valueOf(correctedHealth));
                    setHealthScoreColor(correctedHealth);
                    if (analysisResult.healthAssessment != null) {
                        analysisResult.healthAssessment.score = correctedHealth;
                    }
                }

                // Save via ViewModel (Note: this is simplified - full implementation would need analysisId)
                Toast.makeText(this, "Corrections will be saved when you tap 'Save to Library'", Toast.LENGTH_SHORT).show();
            });
        });

        dialog.show();
    }

    /**
     * Requests notification permission on first save (Android 13+).
     * Shows rationale dialog first, then system permission dialog.
     */
    private void requestNotificationPermissionIfNeeded() {
        // Check if Android 13+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }

        // Check if already requested
        if (keystoreHelper.hasRequestedNotificationPermission()) {
            return;
        }

        // Check if already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            keystoreHelper.setNotificationPermissionRequested();
            return;
        }

        // Mark as requested before showing dialog (prevent repeated prompts)
        keystoreHelper.setNotificationPermissionRequested();

        // Show rationale dialog
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.enable_notifications_title)
                .setMessage(R.string.enable_notifications_rationale)
                .setPositiveButton(R.string.enable, (dialog, which) -> {
                    // Request permission
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            REQUEST_NOTIFICATION_PERMISSION);
                })
                .setNegativeButton(R.string.not_now, null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted - notifications will work
                Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show();
            } else {
                // Permission denied - show one-time banner if not dismissed
                if (!keystoreHelper.hasNotificationBannerDismissed()) {
                    showNotificationPermissionBanner();
                }
            }
        }
    }

    /**
     * Shows dismissable banner for enabling notifications in settings.
     */
    private void showNotificationPermissionBanner() {
        Snackbar snackbar = Snackbar.make(
                findViewById(android.R.id.content),
                R.string.enable_notifications_banner,
                Snackbar.LENGTH_INDEFINITE
        );

        snackbar.setAction(R.string.open_settings, v -> {
            // Open app notification settings
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
            snackbar.dismiss();
        });

        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                if (event == DISMISS_EVENT_MANUAL || event == DISMISS_EVENT_SWIPE) {
                    keystoreHelper.setNotificationBannerDismissed();
                }
            }
        });

        snackbar.show();
    }

    /**
     * Shows Quick Diagnosis tooltip after first analysis completion.
     */
    private void showQuickDiagnosisTooltipIfNeeded() {
        if (!keystoreHelper.hasShownQuickDiagnosisTooltip()) {
            Toast toast = Toast.makeText(this, R.string.quick_diagnosis_tooltip, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 200);
            toast.show();
            keystoreHelper.setQuickDiagnosisTooltipShown();
        }
    }

    /**
     * Handles schedule update prompts from ViewModel.
     * Shows dialog for each user-customized schedule that conflicts with new AI data.
     */
    private void onScheduleUpdatePrompts(List<CareSchedule> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return;
        }

        // Show dialog for each conflict (in sequence)
        for (CareSchedule schedule : schedules) {
            showScheduleUpdateDialog(schedule);
        }
    }

    /**
     * Shows dialog for a single schedule update conflict.
     */
    private void showScheduleUpdateDialog(CareSchedule schedule) {
        // Extract AI-recommended frequency from notes
        if (schedule.notes == null || !schedule.notes.startsWith("AI_RECOMMENDED:")) {
            return;
        }

        String[] parts = schedule.notes.split("\\|", 2);
        String freqPart = parts[0].substring("AI_RECOMMENDED:".length());
        int aiFrequency = Integer.parseInt(freqPart);
        int currentFrequency = schedule.frequencyDays;

        // Format care type for display
        String careTypeDisplay = schedule.careType;
        if ("water".equals(schedule.careType)) {
            careTypeDisplay = "watering";
        } else if ("fertilize".equals(schedule.careType)) {
            careTypeDisplay = "fertilizing";
        } else if ("repot".equals(schedule.careType)) {
            careTypeDisplay = "repotting";
        }

        String message = getString(R.string.ai_recommends_update, careTypeDisplay, aiFrequency, currentFrequency);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.schedule_update_title)
                .setMessage(message)
                .setPositiveButton(R.string.update_schedule, (dialog, which) -> {
                    // User accepts AI recommendation
                    viewModel.acceptScheduleUpdate(schedule);
                    Toast.makeText(this, "Schedule updated", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.keep_current, null)
                .show();
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
