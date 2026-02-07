package com.leafiq.app.ui.library;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.leafiq.app.R;
import com.leafiq.app.data.entity.Plant;
import com.leafiq.app.ui.detail.PlantDetailActivity;

public class LibraryFragment extends Fragment implements PlantCardAdapter.OnPlantClickListener {

    private LibraryViewModel viewModel;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private PlantCardAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_plants);
        emptyState = view.findViewById(R.id.empty_state);

        setupRecyclerView();

        viewModel = new ViewModelProvider(this).get(LibraryViewModel.class);
        viewModel.getAllPlants().observe(getViewLifecycleOwner(), plants -> {
            if (plants == null || plants.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyState.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyState.setVisibility(View.GONE);
                adapter.submitList(plants);
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new PlantCardAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onPlantClick(Plant plant) {
        Intent intent = new Intent(requireContext(), PlantDetailActivity.class);
        intent.putExtra(PlantDetailActivity.EXTRA_PLANT_ID, plant.id);
        startActivity(intent);
    }
}
