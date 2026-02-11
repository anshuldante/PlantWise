package com.leafiq.app.ui.analysis;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
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
import com.leafiq.app.databinding.ActivityAnalysisBinding;
import com.leafiq.app.util.KeystoreHelper;
import com.leafiq.app.util.PhotoQualityChecker;
import com.bumptech.glide.Glide;
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
    public static final String EXTRA_QUICK_DIAGNOSIS = "extra_quick_diagnosis";
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;

    private ActivityAnalysisBinding binding;

    private Uri imageUri;
    private Uri localImageUri; // Persistent local copy
    private String plantId;
    private PlantAnalysisResult analysisResult;
    private boolean isQuickDiagnosis;
    private boolean qualityOverridden = false;

    private KeystoreHelper keystoreHelper;
    private AnalysisViewModel viewModel;
    private ExecutorService executor; // For image copy only

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAnalysisBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize KeystoreHelper for API key checks
        keystoreHelper = new KeystoreHelper(this);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this,
                new AnalysisViewModelFactory(getApplication()))
                .get(AnalysisViewModel.class);

        // Initialize executor for image copy (UI-layer file I/O)
        executor = Executors.newSingleThreadExecutor();

        // Observe UI state from ViewModel
        viewModel.getUiState().observe(this, this::onUiStateChanged);

        // Observe schedule update prompts for re-analysis conflicts
        viewModel.getScheduleUpdatePrompts().observe(this, this::onScheduleUpdatePrompts);

        // Load image URI from intent
        String uriString = getIntent().getStringExtra(EXTRA_IMAGE_URI);
        if (uriString != null) {
            imageUri = Uri.parse(uriString);
            Glide.with(this).load(imageUri).centerCrop().into(binding.imagePreview);
            // Copy image to local storage immediately to preserve access
            copyImageToLocal();
        }

        plantId = getIntent().getStringExtra(EXTRA_PLANT_ID);
        isQuickDiagnosis = getIntent().getBooleanExtra(EXTRA_QUICK_DIAGNOSIS, false);

        // Wire Quick Diagnosis flag into ViewModel
        viewModel.setQuickDiagnosis(isQuickDiagnosis);

        // Set up button click listeners
        binding.btnSave.setOnClickListener(v -> handleSaveClick());
        binding.btnCorrect.setOnClickListener(v -> showCorrectionDialog());
        binding.btnRetry.setOnClickListener(v -> handleRetryClick());

        // Analysis will be triggered after copyImageToLocal() completes
    }

    /**
     * Handles UI state changes from ViewModel.
     * Called whenever ViewModel's LiveData<AnalysisUiState> emits a new state.
     * Delegates rendering to AnalysisRenderer.
     */
    private void onUiStateChanged(AnalysisUiState state) {
        // Delegate main rendering to AnalysisRenderer
        AnalysisRenderer.render(binding, state);

        // Handle Activity-specific concerns
        switch (state.getState()) {
            case SUCCESS:
                analysisResult = state.getResult();

                // Update identification notes with Quick Diagnosis and quality override info
                updateIdentificationNotes();

                showQuickDiagnosisTooltipIfNeeded();
                break;
            case ERROR:
                if (state.isVisionUnsupported()) {
                    showVisionNotSupportedDialog(state.getVisionUnsupportedProvider());
                }
                break;
        }
    }

    /**
     * Updates identification notes with Quick Diagnosis language and quality override badge.
     * This is Activity-specific logic that augments the renderer's output.
     */
    private void updateIdentificationNotes() {
        if (analysisResult == null || analysisResult.identification == null) {
            return;
        }

        StringBuilder notesBuilder = new StringBuilder();

        if (analysisResult.identification.notes != null && !analysisResult.identification.notes.isEmpty()) {
            notesBuilder.append(analysisResult.identification.notes);
        }

        // Add Quick Diagnosis language if in Quick mode
        if (isQuickDiagnosis) {
            if (notesBuilder.length() > 0) {
                notesBuilder.append("\n\n");
            }
            notesBuilder.append("Quick assessment — results may be less precise. For detailed analysis, use full analysis with a clearer photo.");
            Log.i("AnalysisFlow", String.format("quick_diagnosis_used: plantName=%s",
                    analysisResult.identification.commonName));
        }

        // Add quality override badge if user overrode quality warning
        if (qualityOverridden) {
            if (notesBuilder.length() > 0) {
                notesBuilder.append("\n\n");
            }
            notesBuilder.append("⚠ Quality override — photo quality was below recommended threshold");
        }

        if (notesBuilder.length() > 0) {
            binding.identificationNotes.setText(notesBuilder.toString());
            binding.identificationNotes.setVisibility(View.VISIBLE);
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

        binding.btnSave.setEnabled(false);
        binding.btnSave.setText("Saving...");

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
                    binding.btnSave.setEnabled(true);
                    binding.btnSave.setText(R.string.save_to_library);
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
     * Shows error state directly (for errors outside ViewModel flow).
     */
    private void showError(String message) {
        binding.loadingContainer.setVisibility(View.GONE);
        binding.resultsContainer.setVisibility(View.GONE);
        binding.errorContainer.setVisibility(View.VISIBLE);
        binding.errorMessage.setText(message);
    }

    /**
     * Validates photo quality before starting analysis.
     * Checks brightness, blur, and resolution on background thread.
     * Uses lenient thresholds for Quick Diagnosis mode.
     */
    private void validatePhotoQuality() {
        executor.execute(() -> {
            try {
                Uri uriToCheck = localImageUri != null ? localImageUri : imageUri;
                PhotoQualityChecker.QualityResult result =
                    PhotoQualityChecker.checkQuality(getContentResolver(), uriToCheck, isQuickDiagnosis);

                // Log quality check with actual scores
                Log.i("QualityCheck", String.format("photo_quality_check: blur=%.1f brightness=%.2f passed=%b override=%b quick=%b",
                        result.blurScore, result.brightnessScore, result.passed, result.overrideAllowed, isQuickDiagnosis));

                runOnUiThread(() -> {
                    if (result.passed) {
                        proceedToAnalysis();
                    } else {
                        showQualityWarning(result);
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
     * Shows quality warning dialog with two-tier rejection UX.
     * Borderline failures allow override, egregious failures do not.
     * @param result Quality check result with issue details
     */
    private void showQualityWarning(PhotoQualityChecker.QualityResult result) {
        if (result.overrideAllowed) {
            // Borderline failure - show override option with tips
            String messageWithTips = result.message + "\n\nTips:\n" +
                    "• Hold camera steady for sharper photos\n" +
                    "• Ensure good lighting on the plant\n" +
                    "• Get close enough to see leaf details";

            new AlertDialog.Builder(this)
                .setTitle("Photo Quality Issue")
                .setMessage(messageWithTips)
                .setPositiveButton("Use Anyway", (d, w) -> {
                    qualityOverridden = true;
                    viewModel.setQualityOverridden(true);
                    Log.i("QualityCheck", String.format("photo_quality_override_used: issueType=%s blur=%.1f brightness=%.2f",
                            result.issueType, result.blurScore, result.brightnessScore));
                    proceedToAnalysis();
                })
                .setNegativeButton("Choose Different Photo", (d, w) -> finish())
                .setCancelable(false)
                .show();
        } else {
            // Egregious failure - no override option
            String messageWithGuidance = result.message + "\n\n" +
                    "This photo's quality is too low for reliable analysis. Please take a new photo with better conditions.";

            new AlertDialog.Builder(this)
                .setTitle("Photo Cannot Be Analyzed")
                .setMessage(messageWithGuidance)
                .setPositiveButton("Choose Different Photo", (d, w) -> finish())
                .setCancelable(false)
                .show();
        }
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
                    binding.plantCommonName.setText(correctedName);
                    if (analysisResult.identification != null) {
                        analysisResult.identification.commonName = correctedName;
                    }
                }
                if (healthChanged) {
                    binding.healthScore.setText(String.valueOf(correctedHealth));

                    // Set health score color
                    int colorRes = AnalysisRenderer.getHealthScoreColorRes(correctedHealth);
                    ((GradientDrawable) binding.healthScore.getBackground()).setColor(
                        ContextCompat.getColor(AnalysisActivity.this, colorRes));

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
        binding = null; // Prevent memory leaks
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
