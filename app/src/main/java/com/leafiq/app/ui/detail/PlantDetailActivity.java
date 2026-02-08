package com.leafiq.app.ui.detail;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
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
import com.leafiq.app.data.entity.Plant;
import com.leafiq.app.ui.analysis.AnalysisActivity;
import com.leafiq.app.ui.camera.CameraActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
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
    private TextView historyLabel;
    private RecyclerView analysisHistory;
    private FloatingActionButton fabReanalyze;
    private MaterialButton deleteButton;
    private TextInputEditText nicknameInput;
    private AutoCompleteTextView locationInput;

    private String plantId;
    private Plant currentPlant;
    private AnalysisHistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    }

    private void initViews() {
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        plantImage = findViewById(R.id.plant_image);
        scientificName = findViewById(R.id.scientific_name);
        healthScore = findViewById(R.id.health_score);
        summaryCard = findViewById(R.id.summary_card);
        latestSummary = findViewById(R.id.latest_summary);
        lastAnalyzedDate = findViewById(R.id.last_analyzed_date);
        historyLabel = findViewById(R.id.history_label);
        analysisHistory = findViewById(R.id.analysis_history);
        fabReanalyze = findViewById(R.id.fab_reanalyze);
        deleteButton = findViewById(R.id.btn_delete);
        nicknameInput = findViewById(R.id.nickname_input);
        locationInput = findViewById(R.id.location_input);

        setupInputListeners();
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
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new AnalysisHistoryAdapter();
        analysisHistory.setLayoutManager(new LinearLayoutManager(this));
        analysisHistory.setAdapter(adapter);
    }

    private void observeData() {
        viewModel.getPlant(plantId).observe(this, this::displayPlant);
        viewModel.getAnalyses(plantId).observe(this, analyses -> {
            if (analyses != null && !analyses.isEmpty()) {
                historyLabel.setVisibility(View.VISIBLE);
                adapter.submitList(analyses);

                // Show latest analysis summary
                Analysis latest = analyses.get(0);
                if (latest.summary != null && !latest.summary.isEmpty()) {
                    summaryCard.setVisibility(View.VISIBLE);
                    latestSummary.setText(latest.summary);

                    SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
                    lastAnalyzedDate.setText("Analyzed on " + sdf.format(new Date(latest.createdAt)));
                }
            }
        });
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
            Glide.with(this)
                .load(new File(plant.thumbnailPath))
                .centerCrop()
                .into(plantImage);
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
}
