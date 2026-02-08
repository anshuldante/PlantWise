package com.leafiq.app.ui.detail;

import android.text.format.DateUtils;
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
import com.leafiq.app.data.entity.Analysis;
import com.leafiq.app.util.HealthUtils;
import com.bumptech.glide.Glide;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class AnalysisHistoryAdapter extends ListAdapter<Analysis, AnalysisHistoryAdapter.AnalysisViewHolder> {

    /**
     * Click listener for analysis history entries.
     */
    public interface OnAnalysisClickListener {
        void onAnalysisClick(Analysis analysis);
    }

    private final OnAnalysisClickListener clickListener;

    public AnalysisHistoryAdapter(OnAnalysisClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
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
        Analysis analysis = getItem(position);
        holder.bind(analysis);
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onAnalysisClick(analysis);
            }
        });
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
            date.setText(getRelativeTimeString(analysis.createdAt));

            // Health label with color (e.g., "Healthy", "Needs Attention", "Critical")
            String healthLabel = HealthUtils.getHealthLabel(analysis.healthScore);
            score.setText(healthLabel);
            int colorRes = HealthUtils.getHealthColorRes(analysis.healthScore);
            score.setTextColor(ContextCompat.getColor(itemView.getContext(), colorRes));

            // Summary
            summary.setText(analysis.summary != null ? analysis.summary : "");
        }

        private String getRelativeTimeString(long timestamp) {
            if (timestamp <= 0) return "Unknown date";
            long now = System.currentTimeMillis();
            long diff = now - timestamp;
            long days = TimeUnit.MILLISECONDS.toDays(diff);

            if (days < 7) {
                return DateUtils.getRelativeTimeSpanString(
                    timestamp, now, DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE).toString();
            }

            int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH;
            if (days > 365) {
                flags |= DateUtils.FORMAT_SHOW_YEAR;
            }
            return DateUtils.formatDateTime(itemView.getContext(), timestamp, flags);
        }
    }
}
