package com.leafiq.app.ui.library;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.leafiq.app.R;
import com.leafiq.app.data.entity.Plant;
import com.bumptech.glide.Glide;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PlantCardAdapter extends ListAdapter<Plant, PlantCardAdapter.PlantViewHolder> {

    private final OnPlantClickListener clickListener;

    public interface OnPlantClickListener {
        void onPlantClick(Plant plant);
    }

    public PlantCardAdapter(OnPlantClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
    }

    private static final DiffUtil.ItemCallback<Plant> DIFF_CALLBACK =
        new DiffUtil.ItemCallback<Plant>() {
            @Override
            public boolean areItemsTheSame(@NonNull Plant oldItem, @NonNull Plant newItem) {
                return oldItem.id.equals(newItem.id);
            }

            @Override
            public boolean areContentsTheSame(@NonNull Plant oldItem, @NonNull Plant newItem) {
                return oldItem.commonName.equals(newItem.commonName)
                    && oldItem.latestHealthScore == newItem.latestHealthScore
                    && oldItem.updatedAt == newItem.updatedAt;
            }
        };

    @NonNull
    @Override
    public PlantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_plant_card, parent, false);
        return new PlantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlantViewHolder holder, int position) {
        Plant plant = getItem(position);
        holder.bind(plant, clickListener);
    }

    static class PlantViewHolder extends RecyclerView.ViewHolder {
        private final ImageView thumbnail;
        private final TextView name;
        private final TextView scientificName;
        private final TextView lastAnalyzed;
        private final TextView healthScore;

        PlantViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.plant_thumbnail);
            name = itemView.findViewById(R.id.plant_name);
            scientificName = itemView.findViewById(R.id.plant_scientific_name);
            lastAnalyzed = itemView.findViewById(R.id.last_analyzed);
            healthScore = itemView.findViewById(R.id.health_score_badge);
        }

        void bind(Plant plant, OnPlantClickListener clickListener) {
            // Load thumbnail
            if (plant.thumbnailPath != null && !plant.thumbnailPath.isEmpty()) {
                Glide.with(itemView.getContext())
                    .load(new File(plant.thumbnailPath))
                    .centerCrop()
                    .into(thumbnail);
            } else {
                thumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            // Set name
            String displayName = plant.nickname != null && !plant.nickname.isEmpty()
                ? plant.nickname : plant.commonName;
            name.setText(displayName != null ? displayName : "Unknown Plant");

            // Set scientific name
            scientificName.setText(plant.scientificName != null ? plant.scientificName : "");

            // Set last analyzed time
            lastAnalyzed.setText(getRelativeTimeString(plant.updatedAt));

            // Set health score and color
            healthScore.setText(String.valueOf(plant.latestHealthScore));
            int color = getHealthColor(plant.latestHealthScore);
            healthScore.getBackground().setTint(ContextCompat.getColor(itemView.getContext(), color));

            // Click listener
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onPlantClick(plant);
                }
            });
        }

        private int getHealthColor(int score) {
            if (score >= 7) {
                return R.color.health_good;
            } else if (score >= 4) {
                return R.color.health_warning;
            } else {
                return R.color.health_bad;
            }
        }

        private String getRelativeTimeString(long timestamp) {
            long diff = System.currentTimeMillis() - timestamp;
            long days = TimeUnit.MILLISECONDS.toDays(diff);

            if (days == 0) {
                return "Last analyzed: Today";
            } else if (days == 1) {
                return "Last analyzed: Yesterday";
            } else if (days < 7) {
                return "Last analyzed: " + days + " days ago";
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
                return "Last analyzed: " + sdf.format(new Date(timestamp));
            }
        }
    }
}
