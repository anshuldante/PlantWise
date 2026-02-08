package com.leafiq.app.ui.timeline;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.leafiq.app.R;
import com.leafiq.app.ui.camera.CameraActivity;

import java.util.List;

/**
 * Fragment for the Timeline screen.
 * Displays all analyses across all plants with filtering and date grouping.
 */
public class TimelineFragment extends Fragment {

    private TimelineViewModel viewModel;
    private TimelineAdapter adapter;
    private RecyclerView recyclerView;
    private View emptyState;
    private View loadingState;
    private ChipGroup filterChips;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_timeline, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        recyclerView = view.findViewById(R.id.timeline_recycler);
        emptyState = view.findViewById(R.id.empty_state);
        loadingState = view.findViewById(R.id.loading_state);
        filterChips = view.findViewById(R.id.filter_chips);

        // Setup ViewModel
        viewModel = new ViewModelProvider(this).get(TimelineViewModel.class);

        // Setup RecyclerView with adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TimelineAdapter(requireContext(), new TimelineAdapter.OnTimelineItemClickListener() {
            @Override
            public void onItemClick(int position) {
                viewModel.toggleExpansion(position);
            }

            @Override
            public void onViewFullAnalysis(com.leafiq.app.data.model.AnalysisWithPlant data) {
                Intent intent = new Intent(requireContext(), AnalysisDetailActivity.class);
                intent.putExtra(AnalysisDetailActivity.EXTRA_ANALYSIS_ID, data.analysis.id);
                intent.putExtra(AnalysisDetailActivity.EXTRA_PLANT_ID, data.analysis.plantId);
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);

        // Setup filter chips
        setupFilterChips();

        // Setup empty state button
        MaterialButton emptyStateButton = view.findViewById(R.id.empty_state_button);
        emptyStateButton.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), CameraActivity.class));
        });

        // Observe timeline items for state management
        viewModel.getTimelineItems().observe(getViewLifecycleOwner(), this::updateUiState);
    }

    /**
     * Setup filter chip listeners.
     */
    private void setupFilterChips() {
        filterChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return; // Prevent deselecting all chips
            }

            int checkedId = checkedIds.get(0);
            TimelineViewModel.HealthFilter filter;

            if (checkedId == R.id.chip_all) {
                filter = TimelineViewModel.HealthFilter.ALL;
            } else if (checkedId == R.id.chip_healthy) {
                filter = TimelineViewModel.HealthFilter.HEALTHY;
            } else if (checkedId == R.id.chip_needs_attention) {
                filter = TimelineViewModel.HealthFilter.NEEDS_ATTENTION;
            } else if (checkedId == R.id.chip_critical) {
                filter = TimelineViewModel.HealthFilter.CRITICAL;
            } else {
                filter = TimelineViewModel.HealthFilter.ALL;
            }

            viewModel.setFilter(filter);
        });
    }

    /**
     * Update UI state based on timeline items.
     */
    private void updateUiState(List<TimelineViewModel.TimelineItem> items) {
        if (items == null) {
            // Loading state
            loadingState.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
        } else if (items.isEmpty()) {
            // Empty state
            loadingState.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            // Content state
            loadingState.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            // Update adapter with new items
            adapter.submitList(items);
        }
    }
}
