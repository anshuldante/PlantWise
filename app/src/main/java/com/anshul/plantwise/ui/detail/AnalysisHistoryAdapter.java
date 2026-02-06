package com.anshul.plantwise.ui.detail;

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

import com.anshul.plantwise.R;
import com.anshul.plantwise.data.entity.Analysis;
import com.bumptech.glide.Glide;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AnalysisHistoryAdapter extends ListAdapter<Analysis, AnalysisHistoryAdapter.AnalysisViewHolder> {

    public AnalysisHistoryAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<Analysis> DIFF_CALLBACK =
        new DiffUtil.ItemCallback<Analysis>() {
            @Override
            public boolean areItemsTheSame(@NonNull Analysis oldItem, @NonNull Analysis newItem) {
                return oldItem.id.equals(newItem.id);
            }

            @Override
            public boolean areContentsTheSame(@NonNull Analysis oldItem, @NonNull Analysis newItem) {
                return oldItem.healthScore == newItem.healthScore
                    && oldItem.createdAt == newItem.createdAt;
            }
        };

    @NonNull
    @Override
    public AnalysisViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_analysis_history, parent, false);
        return new AnalysisViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AnalysisViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class AnalysisViewHolder extends RecyclerView.ViewHolder {
        private final ImageView thumbnail;
        private final TextView date;
        private final TextView score;
        private final TextView summary;

        AnalysisViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.analysis_thumbnail);
            date = itemView.findViewById(R.id.analysis_date);
            score = itemView.findViewById(R.id.analysis_score);
            summary = itemView.findViewById(R.id.analysis_summary);
        }

        void bind(Analysis analysis) {
            // Load thumbnail
            if (analysis.photoPath != null && !analysis.photoPath.isEmpty()) {
                Glide.with(itemView.getContext())
                    .load(new File(analysis.photoPath))
                    .centerCrop()
                    .into(thumbnail);
            }

            // Format date
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            date.setText(sdf.format(new Date(analysis.createdAt)));

            // Health score with color
            score.setText("Health: " + analysis.healthScore + "/10");
            int colorRes;
            if (analysis.healthScore >= 7) {
                colorRes = R.color.health_good;
            } else if (analysis.healthScore >= 4) {
                colorRes = R.color.health_warning;
            } else {
                colorRes = R.color.health_bad;
            }
            score.setTextColor(ContextCompat.getColor(itemView.getContext(), colorRes));

            // Summary
            summary.setText(analysis.summary != null ? analysis.summary : "");
        }
    }
}
