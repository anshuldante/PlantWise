package com.leafiq.app.ui.care;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.leafiq.app.R;
import com.leafiq.app.data.repository.PlantRepository;

/**
 * Care Overview screen showing today's tasks, upcoming 7-day tasks, and recent completions.
 * Central hub for plant care management with calm, non-judgmental UX.
 */
public class CareOverviewActivity extends AppCompatActivity {

    private CareOverviewViewModel viewModel;

    private RecyclerView todayRecycler;
    private RecyclerView upcomingRecycler;
    private RecyclerView recentRecycler;

    private TextView todayEmpty;
    private TextView upcomingEmpty;
    private TextView recentEmpty;

    private CareTaskAdapter todayAdapter;
    private CareTaskAdapter upcomingAdapter;
    private CareCompletionAdapter recentAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_care_overview);

        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(CareOverviewViewModel.class);

        // Initialize views
        todayRecycler = findViewById(R.id.today_tasks_recycler);
        upcomingRecycler = findViewById(R.id.upcoming_tasks_recycler);
        recentRecycler = findViewById(R.id.recent_completions_recycler);

        todayEmpty = findViewById(R.id.today_empty);
        upcomingEmpty = findViewById(R.id.upcoming_empty);
        recentEmpty = findViewById(R.id.recent_empty);

        // Setup RecyclerViews
        setupRecyclerViews();

        // Observe LiveData
        observeViewModel();
    }

    private void setupRecyclerViews() {
        // Today tasks
        todayRecycler.setLayoutManager(new LinearLayoutManager(this));
        todayAdapter = new CareTaskAdapter(this, new CareTaskAdapter.OnTaskActionListener() {
            @Override
            public void onDoneClicked(CareOverviewViewModel.CareTaskItem item) {
                handleDone(item);
            }

            @Override
            public void onSnoozeClicked(CareOverviewViewModel.CareTaskItem item) {
                handleSnooze(item);
            }
        });
        todayRecycler.setAdapter(todayAdapter);

        // Upcoming tasks
        upcomingRecycler.setLayoutManager(new LinearLayoutManager(this));
        upcomingAdapter = new CareTaskAdapter(this, new CareTaskAdapter.OnTaskActionListener() {
            @Override
            public void onDoneClicked(CareOverviewViewModel.CareTaskItem item) {
                handleDone(item);
            }

            @Override
            public void onSnoozeClicked(CareOverviewViewModel.CareTaskItem item) {
                handleSnooze(item);
            }
        });
        upcomingRecycler.setAdapter(upcomingAdapter);

        // Recent completions
        recentRecycler.setLayoutManager(new LinearLayoutManager(this));
        recentAdapter = new CareCompletionAdapter(this);
        recentRecycler.setAdapter(recentAdapter);
    }

    private void observeViewModel() {
        // Today tasks
        viewModel.getTodayTasks().observe(this, items -> {
            todayAdapter.setItems(items);
            if (items == null || items.isEmpty()) {
                todayRecycler.setVisibility(View.GONE);
                todayEmpty.setVisibility(View.VISIBLE);
            } else {
                todayRecycler.setVisibility(View.VISIBLE);
                todayEmpty.setVisibility(View.GONE);
            }
        });

        // Upcoming tasks
        viewModel.getUpcomingTasks().observe(this, items -> {
            upcomingAdapter.setItems(items);
            if (items == null || items.isEmpty()) {
                upcomingRecycler.setVisibility(View.GONE);
                upcomingEmpty.setVisibility(View.VISIBLE);
            } else {
                upcomingRecycler.setVisibility(View.VISIBLE);
                upcomingEmpty.setVisibility(View.GONE);
            }
        });

        // Recent completions
        viewModel.getRecentCompletions().observe(this, items -> {
            recentAdapter.setItems(items);
            if (items == null || items.isEmpty()) {
                recentRecycler.setVisibility(View.GONE);
                recentEmpty.setVisibility(View.VISIBLE);
            } else {
                recentRecycler.setVisibility(View.VISIBLE);
                recentEmpty.setVisibility(View.GONE);
            }
        });
    }

    private void handleDone(CareOverviewViewModel.CareTaskItem item) {
        viewModel.markComplete(item.schedule.id, new PlantRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    // Get display name
                    String displayName = getPlantDisplayName(item.plant);
                    String verb = getPastTenseVerb(item.schedule.careType);

                    // Show toast: "Watered Fern"
                    String message = verb + " " + displayName;
                    Toast.makeText(CareOverviewActivity.this, message, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(CareOverviewActivity.this, "Error marking complete", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void handleSnooze(CareOverviewViewModel.CareTaskItem item) {
        // Show snooze dialog with three options
        String[] options = {
                getString(R.string.snooze_6_hours),
                getString(R.string.snooze_tomorrow),
                getString(R.string.snooze_next_due)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_snooze)
                .setItems(options, (dialog, which) -> {
                    viewModel.snooze(item.schedule.id, which, new PlantRepository.RepositoryCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            runOnUiThread(() -> {
                                Toast.makeText(CareOverviewActivity.this, "Snoozed", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            runOnUiThread(() -> {
                                Toast.makeText(CareOverviewActivity.this, "Error snoozing", Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private String getPlantDisplayName(com.leafiq.app.data.entity.Plant plant) {
        if (plant.nickname != null && !plant.nickname.isEmpty()) {
            return plant.nickname;
        }
        if (plant.commonName != null && !plant.commonName.isEmpty()) {
            return plant.commonName;
        }
        return "Your plant";
    }

    private String getPastTenseVerb(String careType) {
        switch (careType) {
            case "water":
                return "Watered";
            case "fertilize":
                return "Fertilized";
            case "repot":
                return "Repotted";
            default:
                return "Cared for";
        }
    }
}
