package com.leafiq.app.ui.detail;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.leafiq.app.R;
import com.leafiq.app.data.entity.CareCompletion;
import com.leafiq.app.data.entity.CareSchedule;
import com.leafiq.app.data.repository.PlantRepository;
import com.leafiq.app.util.WindowInsetsHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Full-screen care history list for a plant.
 * Shows all care completions chronologically with month grouping and swipe-to-delete.
 */
public class CareHistoryActivity extends AppCompatActivity {

    public static final String EXTRA_PLANT_ID = "extra_plant_id";

    private String plantId;
    private PlantDetailViewModel viewModel;
    private FullCareHistoryAdapter adapter;
    private RecyclerView recyclerView;
    private TextView emptyState;

    private List<CareCompletion> completions;
    private List<CareSchedule> schedules;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_care_history);

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
            getSupportActionBar().setTitle("Care History");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Set up RecyclerView
        recyclerView = findViewById(R.id.recycler_view);
        emptyState = findViewById(R.id.empty_state);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FullCareHistoryAdapter();
        recyclerView.setAdapter(adapter);

        // Apply window insets
        WindowInsetsHelper.applyBottomInsets(recyclerView);

        // Set up swipe-to-delete
        setupSwipeToDelete();

        // Set up ViewModel and observe data
        viewModel = new ViewModelProvider(this).get(PlantDetailViewModel.class);
        observeData();
    }

    private void observeData() {
        // Observe completions
        viewModel.getAllCompletionsForPlant(plantId).observe(this, completionList -> {
            this.completions = completionList;
            updateAdapter();
            updateEmptyState();
        });

        // Observe schedules
        viewModel.getSchedulesForPlant(plantId).observe(this, scheduleList -> {
            this.schedules = scheduleList;
            updateAdapter();
        });
    }

    private void updateAdapter() {
        if (completions != null && schedules != null) {
            // Build schedule map: scheduleId -> CareSchedule
            Map<String, CareSchedule> scheduleMap = new HashMap<>();
            for (CareSchedule schedule : schedules) {
                scheduleMap.put(schedule.id, schedule);
            }

            // Update adapter with completions and schedule map
            adapter.setData(completions, scheduleMap);
        }
    }

    private void updateEmptyState() {
        if (completions != null && completions.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT
        ) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder viewHolder,
                                @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();

                // Get completion at position (returns null for headers)
                CareCompletion completion = adapter.getCompletionAtPosition(position);

                if (completion == null) {
                    // Header swiped - restore immediately
                    adapter.notifyItemChanged(position);
                    return;
                }

                // Show confirmation dialog
                showDeleteConfirmation(completion, position);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void showDeleteConfirmation(CareCompletion completion, int position) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Delete this care entry?")
                .setMessage("This will update the care history and schedule.")
                .setPositiveButton("Delete", (d, which) -> {
                    // Delete the completion
                    viewModel.deleteCareCompletion(
                            completion.id,
                            completion.scheduleId,
                            new PlantRepository.RepositoryCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    // LiveData will auto-update the list
                                    // Schedule recalculation already happened in ViewModel
                                }

                                @Override
                                public void onError(Exception e) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(CareHistoryActivity.this,
                                                "Error deleting care entry: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                        // Restore item
                                        adapter.notifyItemChanged(position);
                                    });
                                }
                            }
                    );
                })
                .setNegativeButton("Cancel", (d, which) -> {
                    // Restore item
                    adapter.notifyItemChanged(position);
                })
                .setOnCancelListener(d -> {
                    // Restore item on back press
                    adapter.notifyItemChanged(position);
                })
                .create();

        dialog.show();
    }
}
