package com.leafiq.app.ui.library;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.leafiq.app.R;
import com.leafiq.app.data.entity.Plant;
import com.leafiq.app.ui.detail.PlantDetailActivity;
import com.leafiq.app.util.WindowInsetsHelper;

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

        // Apply bottom insets for edge-to-edge
        int existingPadding = (int) (8 * getResources().getDisplayMetrics().density); // 8dp from XML
        WindowInsetsHelper.applyBottomInsetsWithPadding(recyclerView, existingPadding);

        // Add swipe-to-delete
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.START | ItemTouchHelper.END) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder viewHolder,
                                @NonNull RecyclerView.ViewHolder target) {
                return false; // No drag support
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Plant plant = adapter.getCurrentList().get(position);
                showDeleteConfirmation(plant, position);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void showDeleteConfirmation(Plant plant, int position) {
        String displayName = plant.nickname != null && !plant.nickname.isEmpty()
                ? plant.nickname
                : (plant.commonName != null && !plant.commonName.isEmpty()
                        ? plant.commonName
                        : "this plant");

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Plant")
                .setMessage("Delete " + displayName + "? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    viewModel.deletePlant(plant, new com.leafiq.app.data.repository.PlantRepository.RepositoryCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), "Plant deleted", Toast.LENGTH_SHORT).show()
                            );
                        }

                        @Override
                        public void onError(Exception e) {
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Failed to delete plant", Toast.LENGTH_SHORT).show();
                                adapter.notifyItemChanged(position);
                            });
                        }
                    });
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    adapter.notifyItemChanged(position);
                })
                .setOnCancelListener(dialog -> {
                    adapter.notifyItemChanged(position);
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onPlantClick(Plant plant) {
        Intent intent = new Intent(requireContext(), PlantDetailActivity.class);
        intent.putExtra(PlantDetailActivity.EXTRA_PLANT_ID, plant.id);
        startActivity(intent);
    }
}
