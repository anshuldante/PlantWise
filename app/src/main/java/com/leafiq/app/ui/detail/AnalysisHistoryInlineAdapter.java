package com.leafiq.app.ui.detail;

import android.graphics.drawable.GradientDrawable;
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

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Inline adapter for analysis history on plant detail page.
 * Displays compact analysis entries with health trend arrows and PARTIAL/FAILED dimming.
 */
public class AnalysisHistoryInlineAdapter extends ListAdapter<Analysis, AnalysisHistoryInlineAdapter.ViewHolder> {

    public interface OnAnalysisClickListener {
        void onAnalysisClick(Analysis analysis);
    }

    private final OnAnalysisClickListener clickListener;
    private List<Analysis> fullAnalysisList;

    public AnalysisHistoryInlineAdapter(OnAnalysisClickListener clickListener) {
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
                    && oldItem.createdAt == newItem.createdAt
                    && oldItem.parseStatus.equals(newItem.parseStatus);
            }
        };

    /**
     * Set the full analysis list for trend computation.
     * Adapter will compare each item with the next item in this list.
     */
    public void setFullAnalysisList(List<Analysis> fullList) {
        this.fullAnalysisList = fullList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_analysis_history_inline, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Analysis analysis = getItem(position);

        // Compute trend based on position in FULL list (not just displayed 3)
        int trendDirection = 0; // 0=none, 1=up, -1=down
        if (fullAnalysisList != null && fullAnalysisList.size() > 0) {
            int fullListIndex = fullAnalysisList.indexOf(analysis);
            if (fullListIndex >= 0 && fullListIndex < fullAnalysisList.size() - 1) {
                Analysis previous = fullAnalysisList.get(fullListIndex + 1);
                if (analysis.healthScore > previous.healthScore) {
                    trendDirection = 1;
                } else if (analysis.healthScore < previous.healthScore) {
                    trendDirection = -1;
                }
            }
        }

        holder.bind(analysis, trendDirection);
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onAnalysisClick(analysis);
            }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView healthScoreCircle;
        private final TextView summaryText;
        private final TextView dateText;
        private final ImageView trendArrow;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            healthScoreCircle = itemView.findViewById(R.id.health_score_circle);
            summaryText = itemView.findViewById(R.id.analysis_summary_text);
            dateText = itemView.findViewById(R.id.analysis_date_text);
            trendArrow = itemView.findViewById(R.id.health_trend_arrow);
        }

        void bind(Analysis analysis, int trendDirection) {
            // Health score circle
            healthScoreCircle.setText(String.valueOf(analysis.healthScore));
            int colorRes = getHealthColorRes(analysis.healthScore);
            ((GradientDrawable) healthScoreCircle.getBackground()).setColor(
                ContextCompat.getColor(itemView.getContext(), colorRes));

            // Summary
            String summary = analysis.summary != null && !analysis.summary.isEmpty()
                ? analysis.summary : "Analysis completed";
            summaryText.setText(summary);

            // Date
            dateText.setText(getRelativeTimeString(analysis.createdAt));

            // Trend arrow
            if (trendDirection == 0) {
                trendArrow.setVisibility(View.GONE);
            } else {
                trendArrow.setVisibility(View.VISIBLE);
                if (trendDirection > 0) {
                    trendArrow.setImageResource(android.R.drawable.arrow_up_float);
                    trendArrow.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.health_good));
                } else {
                    trendArrow.setImageResource(android.R.drawable.arrow_down_float);
                    trendArrow.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.health_bad));
                }
            }

            // Dim PARTIAL/FAILED entries
            if (analysis.parseStatus != null && !analysis.parseStatus.equals("OK")) {
                itemView.setAlpha(0.82f);
            } else {
                itemView.setAlpha(1.0f);
            }
        }

        private int getHealthColorRes(int score) {
            if (score >= 7) {
                return R.color.health_good;
            } else if (score >= 4) {
                return R.color.health_warning;
            } else {
                return R.color.health_bad;
            }
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
