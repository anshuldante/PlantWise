package com.leafiq.app.ui.detail;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.leafiq.app.R;
import com.leafiq.app.data.entity.Analysis;
import com.leafiq.app.data.repository.PlantRepository;
import com.leafiq.app.ui.timeline.AnalysisDetailActivity;
import com.leafiq.app.util.WindowInsetsHelper;

/**
 * Full-screen analysis history list for a plant.
 * Shows all analyses chronologically grouped by month with swipe-to-delete.
 *
 * Features:
 * - Month-grouped entries with headers
 * - Health trend arrows (green up, red down, gray neutral)
 * - PARTIAL/FAILED dimming at 0.82f alpha
 * - Swipe-to-delete with confirmation dialog
 * - Tap to navigate to AnalysisDetailActivity
 * - Empty state when no analyses exist
 */
public class AnalysisHistoryActivity extends AppCompatActivity {

    public static final String EXTRA_PLANT_ID = "extra_plant_id";

    private String plantId;
    private RecyclerView recyclerView;
    private TextView emptyState;
    private FullAnalysisHistoryAdapter adapter;
    private PlantDetailViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis_history);

        plantId = getIntent().getStringExtra(EXTRA_PLANT_ID);
        if (plantId == null) {
            finish();
            return;
        }

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Analysis History");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize views
        recyclerView = findViewById(R.id.recycler_view);
        emptyState = findViewById(R.id.empty_state);

        // Apply edge-to-edge bottom insets
        WindowInsetsHelper.applyBottomInsets(recyclerView);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FullAnalysisHistoryAdapter();
        recyclerView.setAdapter(adapter);

        // Set up click listener for navigation to AnalysisDetailActivity
        adapter.setOnAnalysisClickListener(analysis -> {
            Intent intent = new Intent(this, AnalysisDetailActivity.class);
            intent.putExtra(AnalysisDetailActivity.EXTRA_ANALYSIS_ID, analysis.id);
            intent.putExtra(AnalysisDetailActivity.EXTRA_PLANT_ID, plantId);
            startActivity(intent);
        });

        // Set up swipe-to-delete
        setupSwipeToDelete();

        // Set up ViewModel and observe data
        viewModel = new ViewModelProvider(this).get(PlantDetailViewModel.class);
        viewModel.getAnalyses(plantId).observe(this, analyses -> {
            if (analyses == null || analyses.isEmpty()) {
                // Show empty state
                recyclerView.setVisibility(View.GONE);
                emptyState.setVisibility(View.VISIBLE);
            } else {
                // Show list
                recyclerView.setVisibility(View.VISIBLE);
                emptyState.setVisibility(View.GONE);
                adapter.setAnalyses(analyses);
            }
        });
    }

    /**
     * Set up ItemTouchHelper for swipe-to-delete with confirmation.
     */
    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT
        ) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Analysis analysis = adapter.getSwipeablePosition(position);

                // If it's a header, restore and return
                if (analysis == null) {
                    adapter.notifyItemChanged(position);
                    return;
                }

                // Show confirmation dialog
                showDeleteConfirmation(analysis, position);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    /**
     * Show confirmation dialog for deleting an analysis.
     *
     * @param analysis Analysis to delete
     * @param position Adapter position for restoring if cancelled
     */
    private void showDeleteConfirmation(Analysis analysis, int position) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Delete this analysis?")
                .setMessage("This action cannot be undone.")
                .setPositiveButton("Delete", (dialogInterface, which) -> {
                    // Delete the analysis
                    viewModel.deleteAnalysis(analysis.id, new PlantRepository.RepositoryCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            // LiveData observation will automatically update the list
                            Toast.makeText(AnalysisHistoryActivity.this,
                                    "Analysis deleted", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(Exception e) {
                            // Restore item on error
                            adapter.notifyItemChanged(position);
                            Toast.makeText(AnalysisHistoryActivity.this,
                                    "Failed to delete analysis", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", (dialogInterface, which) -> {
                    // Restore the swiped item
                    adapter.notifyItemChanged(position);
                })
                .setOnCancelListener(dialogInterface -> {
                    // Restore item if dismissed without action
                    adapter.notifyItemChanged(position);
                })
                .create();

        dialog.show();
    }
}
