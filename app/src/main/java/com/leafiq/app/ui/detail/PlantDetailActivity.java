package com.leafiq.app.ui.detail;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.leafiq.app.R;
import com.leafiq.app.data.entity.Analysis;
import com.leafiq.app.data.entity.CareSchedule;
import com.leafiq.app.data.entity.Plant;
import com.leafiq.app.ui.analysis.AnalysisActivity;
import com.leafiq.app.ui.camera.CameraActivity;
import com.leafiq.app.ui.timeline.AnalysisDetailActivity;
import com.leafiq.app.ui.timeline.SparklineView;
import com.leafiq.app.util.ImageUtils;
import com.leafiq.app.util.WindowInsetsHelper;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PlantDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PLANT_ID = "extra_plant_id";

    private PlantDetailViewModel viewModel;

    private CollapsingToolbarLayout collapsingToolbar;
    private ImageView plantImage;
    private TextView scientificName;
    private TextView healthScore;
    private MaterialCardView summaryCard;
    private TextView latestSummary;
    private TextView lastAnalyzedDate;
    private FloatingActionButton fabReanalyze;
    private MaterialButton deleteButton;
    private MaterialButton correctButton;
    private TextInputEditText nicknameInput;
    private AutoCompleteTextView locationInput;
    private SparklineView healthSparkline;
    private TextView sparklineHint;
    private MaterialCardView careRemindersCard;
    private SwitchMaterial switchReminders;
    private LinearLayout schedulesContainer;
    private TextView snoozeSuggestion;
    private LinearLayout onboardingCtaBlock;
    private MaterialButton onboardingAnalyzeButton;
    private TextView onboardingCareLink;
    private LinearLayout careHistorySection;
    private TextView careHistoryViewAll;
    private RecyclerView careHistoryInlineRecycler;
    private TextView careHistoryEmpty;
    private LinearLayout analysisHistorySection;
    private TextView analysisHistoryViewAll;
    private RecyclerView analysisHistoryInlineRecycler;
    private TextView analysisHistoryEmpty;

    private String plantId;
    private Plant currentPlant;
    private Analysis latestAnalysis;
    private AnalysisHistoryAdapter adapter;
    private CareHistoryAdapter careHistoryAdapter;
    private List<CareSchedule> currentSchedules;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable edge-to-edge for transparent navigation bar
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_plant_detail);

        plantId = getIntent().getStringExtra(EXTRA_PLANT_ID);
        if (plantId == null) {
            finish();
            return;
        }

        initViews();
        setupToolbar();
        setupRecyclerView();

        viewModel = new ViewModelProvider(this).get(PlantDetailViewModel.class);
        observeData();

        fabReanalyze.setOnClickListener(v -> {
            Intent intent = new Intent(this, CameraActivity.class);
            intent.putExtra(CameraActivity.EXTRA_PLANT_ID, plantId);
            startActivity(intent);
        });

        deleteButton.setOnClickListener(v -> showDeleteConfirmation());
        correctButton.setOnClickListener(v -> showCorrectionDialog());
    }

    private void initViews() {
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        plantImage = findViewById(R.id.plant_image);
        scientificName = findViewById(R.id.scientific_name);
        healthScore = findViewById(R.id.health_score);
        summaryCard = findViewById(R.id.summary_card);
        latestSummary = findViewById(R.id.latest_summary);
        lastAnalyzedDate = findViewById(R.id.last_analyzed_date);
        fabReanalyze = findViewById(R.id.fab_reanalyze);
        deleteButton = findViewById(R.id.btn_delete);
        correctButton = findViewById(R.id.btn_correct_analysis);
        nicknameInput = findViewById(R.id.nickname_input);
        locationInput = findViewById(R.id.location_input);
        healthSparkline = findViewById(R.id.health_sparkline);
        sparklineHint = findViewById(R.id.sparkline_hint);
        careRemindersCard = findViewById(R.id.care_reminders_card);
        switchReminders = findViewById(R.id.switch_reminders);
        schedulesContainer = findViewById(R.id.schedules_container);
        snoozeSuggestion = findViewById(R.id.snooze_suggestion);
        onboardingCtaBlock = findViewById(R.id.onboarding_cta_block);
        onboardingAnalyzeButton = findViewById(R.id.onboarding_analyze_button);
        onboardingCareLink = findViewById(R.id.onboarding_care_link);
        careHistorySection = findViewById(R.id.care_history_section);
        careHistoryViewAll = findViewById(R.id.care_history_view_all);
        careHistoryInlineRecycler = findViewById(R.id.care_history_inline_recycler);
        careHistoryEmpty = findViewById(R.id.care_history_empty);
        analysisHistorySection = findViewById(R.id.analysis_history_section);
        analysisHistoryViewAll = findViewById(R.id.analysis_history_view_all);
        analysisHistoryInlineRecycler = findViewById(R.id.analysis_history_inline_recycler);
        analysisHistoryEmpty = findViewById(R.id.analysis_history_empty);

        setupInputListeners();
        setupReminderToggle();
        setupCareHistoryRecycler();

        // Tap image to open full-screen viewer
        plantImage.setOnClickListener(v -> {
            if (currentPlant != null) {
                // Prefer original photo, fall back to high-res thumbnail
                viewModel.getLatestPhotoPath(currentPlant.id, photoPath -> {
                    runOnUiThread(() -> {
                        String imagePath = photoPath;
                        if (imagePath == null && currentPlant.highResThumbnailPath != null) {
                            imagePath = currentPlant.highResThumbnailPath;
                        }
                        if (imagePath == null && currentPlant.thumbnailPath != null) {
                            imagePath = currentPlant.thumbnailPath;
                        }
                        if (imagePath != null) {
                            Intent intent = new Intent(PlantDetailActivity.this, ImageViewerActivity.class);
                            intent.putExtra(ImageViewerActivity.EXTRA_IMAGE_PATH, imagePath);
                            startActivity(intent);
                        }
                    });
                });
            }
        });

        // Apply bottom insets to scrollable content so it's visible above system nav
        androidx.core.widget.NestedScrollView scrollContent = findViewById(R.id.scroll_content);
        WindowInsetsHelper.applyBottomInsets(scrollContent);
    }

    private void setupInputListeners() {
        // Nickname auto-save on focus lost
        nicknameInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && currentPlant != null) {
                String newNickname = nicknameInput.getText() != null ? nicknameInput.getText().toString().trim() : "";
                String oldNickname = currentPlant.nickname != null ? currentPlant.nickname : "";

                if (!newNickname.equals(oldNickname)) {
                    currentPlant.nickname = newNickname.isEmpty() ? null : newNickname;
                    currentPlant.updatedAt = System.currentTimeMillis();

                    viewModel.updatePlant(currentPlant, new com.leafiq.app.data.repository.PlantRepository.RepositoryCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            runOnUiThread(() -> {
                                // Update toolbar title
                                String displayName = currentPlant.nickname != null && !currentPlant.nickname.isEmpty()
                                    ? currentPlant.nickname : currentPlant.commonName;
                                collapsingToolbar.setTitle(displayName != null ? displayName : "Unknown Plant");
                                Toast.makeText(PlantDetailActivity.this, "Nickname saved", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            runOnUiThread(() ->
                                Toast.makeText(PlantDetailActivity.this, "Failed to save nickname", Toast.LENGTH_SHORT).show()
                            );
                        }
                    });
                }
            }
        });

        nicknameInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                nicknameInput.clearFocus();
                android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(nicknameInput.getWindowToken(), 0);
                return true;
            }
            return false;
        });

        // Location auto-save on focus lost
        locationInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // Load location autocomplete suggestions when focused
                viewModel.getDistinctLocations(new com.leafiq.app.data.repository.PlantRepository.RepositoryCallback<java.util.List<String>>() {
                    @Override
                    public void onSuccess(java.util.List<String> locations) {
                        runOnUiThread(() -> {
                            if (locations != null && !locations.isEmpty()) {
                                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                    PlantDetailActivity.this,
                                    android.R.layout.simple_dropdown_item_1line,
                                    locations
                                );
                                locationInput.setAdapter(adapter);
                                locationInput.setThreshold(1);
                            }
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        // Silently fail - autocomplete just won't work
                    }
                });
            } else if (currentPlant != null) {
                // Save location when focus is lost
                String newLocation = locationInput.getText() != null ? locationInput.getText().toString().trim() : "";
                String oldLocation = currentPlant.location != null ? currentPlant.location : "";

                if (!newLocation.equals(oldLocation)) {
                    currentPlant.location = newLocation.isEmpty() ? null : newLocation;
                    currentPlant.updatedAt = System.currentTimeMillis();

                    viewModel.updatePlant(currentPlant, new com.leafiq.app.data.repository.PlantRepository.RepositoryCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            runOnUiThread(() ->
                                Toast.makeText(PlantDetailActivity.this, "Location saved", Toast.LENGTH_SHORT).show()
                            );
                        }

                        @Override
                        public void onError(Exception e) {
                            runOnUiThread(() ->
                                Toast.makeText(PlantDetailActivity.this, "Failed to save location", Toast.LENGTH_SHORT).show()
                            );
                        }
                    });
                }
            }
        });

        locationInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                locationInput.clearFocus();
                android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(locationInput.getWindowToken(), 0);
                return true;
            }
            return false;
        });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new AnalysisHistoryAdapter(analysis -> {
            // Open AnalysisDetailActivity when history entry is clicked
            Intent intent = new Intent(this, AnalysisDetailActivity.class);
            intent.putExtra(AnalysisDetailActivity.EXTRA_ANALYSIS_ID, analysis.id);
            intent.putExtra(AnalysisDetailActivity.EXTRA_PLANT_ID, plantId);
            startActivity(intent);
        });
        analysisHistoryInlineRecycler.setLayoutManager(new LinearLayoutManager(this));
        analysisHistoryInlineRecycler.setAdapter(adapter);
    }

    private void setupCareHistoryRecycler() {
        careHistoryAdapter = new CareHistoryAdapter();
        careHistoryInlineRecycler.setLayoutManager(new LinearLayoutManager(this));
        careHistoryInlineRecycler.setAdapter(careHistoryAdapter);
    }

    private void observeData() {
        viewModel.getPlant(plantId).observe(this, this::displayPlant);
        viewModel.getAnalyses(plantId).observe(this, analyses -> {
            if (analyses != null && !analyses.isEmpty()) {
                analysisHistorySection.setVisibility(View.VISIBLE);

                // Limit to 3 entries for inline display
                List<Analysis> limitedAnalyses = analyses.size() > 3
                    ? analyses.subList(0, 3)
                    : analyses;
                adapter.submitList(limitedAnalyses);

                // Show latest analysis summary
                Analysis latest = analyses.get(0);
                latestAnalysis = latest;
                if (latest.summary != null && !latest.summary.isEmpty()) {
                    summaryCard.setVisibility(View.VISIBLE);
                    correctButton.setVisibility(View.VISIBLE);
                    latestSummary.setText(latest.summary);

                    SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
                    lastAnalyzedDate.setText("Analyzed on " + sdf.format(new Date(latest.createdAt)));
                }

                // Update sparkline with health score trend
                updateSparkline(analyses, currentPlant != null ? currentPlant.latestHealthScore : 0);
            }
        });

        // Observe care schedules
        viewModel.getSchedulesForPlant(plantId).observe(this, schedules -> {
            currentSchedules = schedules;
            if (schedules != null && !schedules.isEmpty()) {
                careRemindersCard.setVisibility(View.VISIBLE);
                displaySchedules(schedules);
            } else {
                careRemindersCard.setVisibility(View.GONE);
            }
        });

        // Observe care completions (limited to 3 for inline display)
        viewModel.getLimitedCompletions(plantId, 3).observe(this, completions -> {
            if (completions != null && !completions.isEmpty() && currentSchedules != null) {
                careHistorySection.setVisibility(View.VISIBLE);

                // Build careTypeMap from schedules
                java.util.Map<String, String> careTypeMap = new java.util.HashMap<>();
                for (CareSchedule schedule : currentSchedules) {
                    careTypeMap.put(schedule.id, schedule.careType);
                }

                // Update adapter
                careHistoryAdapter.setCareTypeMap(careTypeMap);
                careHistoryAdapter.submitList(completions);

                // Show/hide empty state
                careHistoryInlineRecycler.setVisibility(View.VISIBLE);
                careHistoryEmpty.setVisibility(View.GONE);
            } else if (currentSchedules != null && !currentSchedules.isEmpty()) {
                // Have schedules but no completions yet
                careHistorySection.setVisibility(View.VISIBLE);
                careHistoryInlineRecycler.setVisibility(View.GONE);
                careHistoryEmpty.setVisibility(View.VISIBLE);
            } else {
                // No schedules, hide entire section
                careHistorySection.setVisibility(View.GONE);
            }
        });

        // Observe counts for "View All" links
        viewModel.getAnalysisCount(plantId).observe(this, count -> {
            if (count != null && count > 0) {
                analysisHistoryViewAll.setText("(" + count + ") >");
            }
        });

        viewModel.getCareCompletionCount(plantId).observe(this, count -> {
            if (count != null && count > 0) {
                careHistoryViewAll.setText("(" + count + ") >");
            }
        });
    }

    /**
     * Updates the health sparkline with analysis history.
     * Shows sparkline if 2+ analyses, hint if 1 analysis, nothing if 0.
     *
     * @param analyses          List of analyses (reverse chronological - newest first)
     * @param latestHealthScore Latest health score for color determination
     */
    private void updateSparkline(List<Analysis> analyses, int latestHealthScore) {
        if (analyses == null || analyses.isEmpty()) {
            healthSparkline.setVisibility(View.GONE);
            sparklineHint.setVisibility(View.GONE);
            return;
        }

        if (analyses.size() == 1) {
            // Single analysis - show hint
            healthSparkline.setVisibility(View.GONE);
            sparklineHint.setVisibility(View.VISIBLE);
        } else {
            // Multiple analyses - show sparkline
            sparklineHint.setVisibility(View.GONE);

            // Extract health scores and reverse to chronological order (oldest first)
            List<Integer> healthScores = new ArrayList<>();
            for (Analysis analysis : analyses) {
                healthScores.add(analysis.healthScore);
            }
            Collections.reverse(healthScores);

            healthSparkline.setData(healthScores, latestHealthScore);
        }
    }

    private void displayPlant(Plant plant) {
        if (plant == null) return;

        currentPlant = plant;

        String displayName = plant.nickname != null && !plant.nickname.isEmpty()
            ? plant.nickname : plant.commonName;
        collapsingToolbar.setTitle(displayName != null ? displayName : "Unknown Plant");

        scientificName.setText(plant.scientificName != null ? plant.scientificName : "");

        healthScore.setText(String.valueOf(plant.latestHealthScore));
        setHealthScoreColor(plant.latestHealthScore);

        if (plant.thumbnailPath != null && !plant.thumbnailPath.isEmpty()) {
            File lowRes = new File(plant.thumbnailPath);

            if (plant.highResThumbnailPath != null && !plant.highResThumbnailPath.isEmpty()) {
                File highRes = new File(plant.highResThumbnailPath);
                if (highRes.exists()) {
                    // Progressive load: low-res first, then high-res
                    Glide.with(this)
                        .load(highRes)
                        .thumbnail(Glide.with(this).load(lowRes))
                        .centerCrop()
                        .into(plantImage);
                } else {
                    // High-res path set but file missing — fall back and regenerate
                    Glide.with(this)
                        .load(lowRes)
                        .centerCrop()
                        .into(plantImage);
                    regenerateHighResThumbnail(plant);
                }
            } else {
                // No high-res path — show low-res and trigger lazy migration
                Glide.with(this)
                    .load(lowRes)
                    .centerCrop()
                    .into(plantImage);
                regenerateHighResThumbnail(plant);
            }
        }

        // Populate nickname and location inputs
        if (plant.nickname != null) {
            nicknameInput.setText(plant.nickname);
        }
        if (plant.location != null) {
            locationInput.setText(plant.location);
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

    /**
     * Lazy migration: generate high-res thumbnail for existing plants that only have 256px.
     * Runs on background thread, updates UI when ready.
     */
    private void regenerateHighResThumbnail(Plant plant) {
        // Find the original photo path from the latest analysis
        viewModel.getLatestPhotoPath(plant.id, photoPath -> {
            if (photoPath == null) return;

            // Generate high-res on background thread
            String highResPath = ImageUtils.generateHighResThumbnailFromFile(
                getApplicationContext(), photoPath, plant.id);

            if (highResPath != null) {
                // Update plant record
                plant.highResThumbnailPath = highResPath;
                plant.updatedAt = System.currentTimeMillis();
                viewModel.updatePlant(plant, new com.leafiq.app.data.repository.PlantRepository.RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        // Swap to high-res in UI
                        runOnUiThread(() -> {
                            if (!isFinishing()) {
                                Glide.with(PlantDetailActivity.this)
                                    .load(new File(highResPath))
                                    .centerCrop()
                                    .into(plantImage);
                            }
                        });
                    }
                    @Override
                    public void onError(Exception e) {
                        // Silently fail — low-res thumbnail still visible
                    }
                });
            }
        });
    }

    private void showDeleteConfirmation() {
        if (currentPlant == null) return;

        String displayName = currentPlant.nickname != null && !currentPlant.nickname.isEmpty()
                ? currentPlant.nickname
                : (currentPlant.commonName != null && !currentPlant.commonName.isEmpty()
                        ? currentPlant.commonName
                        : "this plant");

        new AlertDialog.Builder(this)
                .setTitle("Delete Plant")
                .setMessage("Delete " + displayName + "? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    viewModel.deletePlant(currentPlant, new com.leafiq.app.data.repository.PlantRepository.RepositoryCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            runOnUiThread(() -> {
                                Toast.makeText(PlantDetailActivity.this, "Plant deleted", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            runOnUiThread(() ->
                                    Toast.makeText(PlantDetailActivity.this, "Failed to delete plant", Toast.LENGTH_SHORT).show()
                            );
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Shows correction dialog for user to provide corrections to past analysis.
     * Only allows "Save Without Re-analysis" from detail screen.
     * For re-analysis, user should use the camera FAB.
     */
    private void showCorrectionDialog() {
        if (currentPlant == null || latestAnalysis == null) return;

        // Inflate dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_correction, null);
        com.google.android.material.textfield.TextInputEditText nameInput = dialogView.findViewById(R.id.correct_name_input);
        com.google.android.material.textfield.TextInputEditText healthInput = dialogView.findViewById(R.id.correct_health_input);
        com.google.android.material.textfield.TextInputEditText contextInput = dialogView.findViewById(R.id.additional_context_input);

        // Pre-fill current values
        if (currentPlant.commonName != null) {
            nameInput.setText(currentPlant.commonName);
        }
        healthInput.setText(String.valueOf(latestAnalysis.healthScore));

        // Hide additional context field for detail screen (not used without re-analysis)
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
                int originalHealth = latestAnalysis.healthScore;
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
                    viewModel.updatePlant(currentPlant, new com.leafiq.app.data.repository.PlantRepository.RepositoryCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            runOnUiThread(() -> {
                                String displayName = currentPlant.nickname != null && !currentPlant.nickname.isEmpty()
                                    ? currentPlant.nickname : currentPlant.commonName;
                                collapsingToolbar.setTitle(displayName != null ? displayName : "Unknown Plant");
                                Toast.makeText(PlantDetailActivity.this, "Plant name updated", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            runOnUiThread(() ->
                                Toast.makeText(PlantDetailActivity.this, "Failed to update plant name", Toast.LENGTH_SHORT).show()
                            );
                        }
                    });
                }

                // Update analysis health if changed
                if (healthChanged) {
                    final int finalCorrectedHealth = correctedHealth;
                    latestAnalysis.healthScore = finalCorrectedHealth;
                    currentPlant.latestHealthScore = finalCorrectedHealth;
                    currentPlant.updatedAt = System.currentTimeMillis();

                    // Update analysis
                    viewModel.updateAnalysis(latestAnalysis, new com.leafiq.app.data.repository.PlantRepository.RepositoryCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            // Also update plant's latest health score
                            viewModel.updatePlant(currentPlant, new com.leafiq.app.data.repository.PlantRepository.RepositoryCallback<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    runOnUiThread(() -> {
                                        healthScore.setText(String.valueOf(finalCorrectedHealth));
                                        setHealthScoreColor(finalCorrectedHealth);
                                        Toast.makeText(PlantDetailActivity.this, "Health score updated", Toast.LENGTH_SHORT).show();
                                    });
                                }

                                @Override
                                public void onError(Exception e) {
                                    runOnUiThread(() ->
                                        Toast.makeText(PlantDetailActivity.this, "Failed to update plant", Toast.LENGTH_SHORT).show()
                                    );
                                }
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            runOnUiThread(() ->
                                Toast.makeText(PlantDetailActivity.this, "Failed to update analysis", Toast.LENGTH_SHORT).show()
                            );
                        }
                    });
                }
            });
        });

        dialog.show();
    }

    private void setupReminderToggle() {
        switchReminders.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (plantId == null) return;

            viewModel.toggleReminders(plantId, isChecked, new com.leafiq.app.data.repository.PlantRepository.RepositoryCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    runOnUiThread(() -> {
                        String message = isChecked ? getString(R.string.reminders_enabled) : getString(R.string.reminders_paused);
                        Toast.makeText(PlantDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(PlantDetailActivity.this, "Failed to update reminders", Toast.LENGTH_SHORT).show();
                        // Revert switch state
                        switchReminders.setChecked(!isChecked);
                    });
                }
            });
        });
    }

    private void displaySchedules(List<CareSchedule> schedules) {
        schedulesContainer.removeAllViews();

        // Set switch state from first schedule's isEnabled (all share same state)
        if (!schedules.isEmpty()) {
            switchReminders.setOnCheckedChangeListener(null); // Remove listener to avoid triggering during set
            switchReminders.setChecked(schedules.get(0).isEnabled);
            setupReminderToggle(); // Re-attach listener
        }

        boolean showSnoozeSuggestion = false;

        for (CareSchedule schedule : schedules) {
            // Create schedule row
            View scheduleRow = createScheduleRow(schedule);
            schedulesContainer.addView(scheduleRow);

            // Check for snooze suggestion marker
            if (schedule.notes != null && schedule.notes.contains("[SUGGEST_ADJUST]")) {
                showSnoozeSuggestion = true;
            }
        }

        snoozeSuggestion.setVisibility(showSnoozeSuggestion ? View.VISIBLE : View.GONE);
    }

    private View createScheduleRow(CareSchedule schedule) {
        // Create horizontal LinearLayout for schedule row
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.topMargin = (int) (8 * getResources().getDisplayMetrics().density); // 8dp
        row.setLayoutParams(rowParams);
        row.setPadding(
                (int) (12 * getResources().getDisplayMetrics().density),
                (int) (12 * getResources().getDisplayMetrics().density),
                (int) (12 * getResources().getDisplayMetrics().density),
                (int) (12 * getResources().getDisplayMetrics().density)
        );
        row.setBackground(ContextCompat.getDrawable(this, R.drawable.health_score_background));
        row.setClickable(true);
        row.setFocusable(true);

        // Care type emoji
        TextView emojiView = new TextView(this);
        emojiView.setTextSize(24);
        emojiView.setText(getCareTypeEmoji(schedule.careType));
        LinearLayout.LayoutParams emojiParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        emojiParams.rightMargin = (int) (12 * getResources().getDisplayMetrics().density); // 12dp
        emojiView.setLayoutParams(emojiParams);
        row.addView(emojiView);

        // Vertical inner layout
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams innerParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        inner.setLayoutParams(innerParams);

        // Care type name + frequency
        TextView nameView = new TextView(this);
        nameView.setText(getCareTypeName(schedule.careType) + " • " + getString(R.string.every_x_days, schedule.frequencyDays));
        nameView.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        nameView.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        inner.addView(nameView);

        // Source label
        TextView sourceView = new TextView(this);
        sourceView.setText(schedule.isCustom ? getString(R.string.customized_by_you) : getString(R.string.suggested_by_ai));
        sourceView.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
        sourceView.setTextColor(ContextCompat.getColor(this, schedule.isCustom ? R.color.md_theme_primary : R.color.text_secondary));
        inner.addView(sourceView);

        row.addView(inner);

        // Set click listener to open frequency picker
        row.setOnClickListener(v -> showFrequencyPicker(schedule));

        return row;
    }

    private String getCareTypeEmoji(String careType) {
        switch (careType) {
            case "water":
                return "💧";
            case "fertilize":
                return "🌱";
            case "repot":
                return "🪴";
            default:
                return "✓";
        }
    }

    private String getCareTypeName(String careType) {
        switch (careType) {
            case "water":
                return "Water";
            case "fertilize":
                return "Fertilize";
            case "repot":
                return "Repot";
            default:
                return careType;
        }
    }

    private void showFrequencyPicker(CareSchedule schedule) {
        // Inflate dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_frequency_picker, null);
        TextView currentFrequencyLabel = dialogView.findViewById(R.id.current_frequency_label);
        TextView scheduleSourceLabel = dialogView.findViewById(R.id.schedule_source_label);
        com.google.android.material.textfield.TextInputEditText frequencyNumber = dialogView.findViewById(R.id.frequency_number);
        AutoCompleteTextView frequencyUnit = dialogView.findViewById(R.id.frequency_unit);

        // Set current values
        currentFrequencyLabel.setText("Current: Every " + schedule.frequencyDays + " days");
        scheduleSourceLabel.setText(schedule.isCustom ? getString(R.string.customized_by_you) : getString(R.string.suggested_by_ai));
        scheduleSourceLabel.setTextColor(ContextCompat.getColor(this, schedule.isCustom ? R.color.md_theme_primary : R.color.text_secondary));

        // Pre-fill with current frequency converted to appropriate unit
        int[] converted = convertDaysToUnit(schedule.frequencyDays);
        frequencyNumber.setText(String.valueOf(converted[0]));

        // Set up unit dropdown
        String[] units = new String[]{"days", "weeks", "months"};
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, units);
        frequencyUnit.setAdapter(unitAdapter);
        frequencyUnit.setText(units[converted[1]], false);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Adjust " + getCareTypeName(schedule.careType) + " Schedule")
                .setView(dialogView)
                .setPositiveButton("Save", (d, which) -> {
                    String numberText = frequencyNumber.getText() != null ? frequencyNumber.getText().toString().trim() : "";
                    String unit = frequencyUnit.getText().toString();

                    try {
                        int number = Integer.parseInt(numberText);
                        if (number < 1 || number > 365) {
                            Toast.makeText(this, "Please enter a number between 1 and 365", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        int newFrequencyDays = convertUnitToDays(number, unit);

                        // Update schedule
                        viewModel.updateScheduleFrequency(schedule.id, newFrequencyDays, new com.leafiq.app.data.repository.PlantRepository.RepositoryCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                runOnUiThread(() -> {
                                    Toast.makeText(PlantDetailActivity.this, "Schedule updated", Toast.LENGTH_SHORT).show();
                                });
                            }

                            @Override
                            public void onError(Exception e) {
                                runOnUiThread(() -> {
                                    Toast.makeText(PlantDetailActivity.this, "Failed to update schedule", Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
    }

    /**
     * Converts days to the most appropriate unit (days, weeks, or months).
     * Returns [number, unitIndex] where unitIndex: 0=days, 1=weeks, 2=months
     */
    private int[] convertDaysToUnit(int days) {
        if (days % 30 == 0 && days >= 30) {
            return new int[]{days / 30, 2}; // months
        } else if (days % 7 == 0 && days >= 7) {
            return new int[]{days / 7, 1}; // weeks
        } else {
            return new int[]{days, 0}; // days
        }
    }

    private int convertUnitToDays(int number, String unit) {
        switch (unit) {
            case "weeks":
                return number * 7;
            case "months":
                return number * 30;
            default:
                return number;
        }
    }
}
