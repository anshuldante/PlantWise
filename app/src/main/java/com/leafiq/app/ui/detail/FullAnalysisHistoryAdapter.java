package com.leafiq.app.ui.detail;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.leafiq.app.R;
import com.leafiq.app.data.entity.Analysis;
import com.leafiq.app.util.HealthUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for full-screen analysis history with month grouping.
 * Displays TYPE_HEADER for month labels and TYPE_ITEM for analysis entries.
 *
 * Supports:
 * - Month grouping with headers
 * - Health trend arrows (compare with previous analysis)
 * - PARTIAL/FAILED dimming at 0.82f alpha
 * - Click listener for navigation to AnalysisDetailActivity
 */
public class FullAnalysisHistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    static final int TYPE_HEADER = 0;
    static final int TYPE_ITEM = 1;

    /**
     * Click listener for analysis history entries.
     */
    public interface OnAnalysisClickListener {
        void onAnalysisClick(Analysis analysis);
    }

    /**
     * Wrapper item for RecyclerView display.
     * Contains either a month header or an analysis item.
     */
    static class HistoryItem {
        int type;
        String monthLabel;      // for TYPE_HEADER
        Analysis analysis;      // for TYPE_ITEM
        int trendDirection;     // -1=down, 0=same, 1=up, -2=no previous
    }

    private final List<HistoryItem> items = new ArrayList<>();
    private OnAnalysisClickListener clickListener;

    public FullAnalysisHistoryAdapter() {
    }

    public void setOnAnalysisClickListener(OnAnalysisClickListener listener) {
        this.clickListener = listener;
    }

    /**
     * Set analyses and build flattened list with month headers.
     *
     * @param analyses List of analyses sorted by createdAt DESC (newest first)
     */
    public void setAnalyses(List<Analysis> analyses) {
        items.clear();

        if (analyses == null || analyses.isEmpty()) {
            notifyDataSetChanged();
            return;
        }

        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        String currentMonth = null;

        for (int i = 0; i < analyses.size(); i++) {
            Analysis analysis = analyses.get(i);

            // Check if we need a new month header
            String month = monthFormat.format(new Date(analysis.createdAt));
            if (!month.equals(currentMonth)) {
                currentMonth = month;
                HistoryItem header = new HistoryItem();
                header.type = TYPE_HEADER;
                header.monthLabel = month;
                items.add(header);
            }

            // Add analysis item with trend computation
            HistoryItem item = new HistoryItem();
            item.type = TYPE_ITEM;
            item.analysis = analysis;

            // Compute trend direction by comparing with next (older) analysis
            if (i + 1 < analyses.size()) {
                Analysis olderAnalysis = analyses.get(i + 1);
                int currentScore = analysis.healthScore;
                int olderScore = olderAnalysis.healthScore;

                if (currentScore > olderScore) {
                    item.trendDirection = 1;  // up (improved)
                } else if (currentScore < olderScore) {
                    item.trendDirection = -1; // down (declined)
                } else {
                    item.trendDirection = 0;  // same
                }
            } else {
                // First (oldest) analysis - no previous to compare
                item.trendDirection = -2;
            }

            items.add(item);
        }

        notifyDataSetChanged();
    }

    /**
     * Get the Analysis at the given adapter position (for swipe-to-delete).
     * Returns null if position is a header.
     */
    public Analysis getSwipeablePosition(int adapterPosition) {
        if (adapterPosition < 0 || adapterPosition >= items.size()) {
            return null;
        }
        HistoryItem item = items.get(adapterPosition);
        return item.type == TYPE_ITEM ? item.analysis : null;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_month_header, parent, false);
            return new MonthHeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_analysis_history_full, parent, false);
            return new AnalysisItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        HistoryItem item = items.get(position);

        if (holder instanceof MonthHeaderViewHolder) {
            ((MonthHeaderViewHolder) holder).bind(item.monthLabel);
        } else if (holder instanceof AnalysisItemViewHolder) {
            AnalysisItemViewHolder itemHolder = (AnalysisItemViewHolder) holder;
            itemHolder.bind(item.analysis, item.trendDirection);

            // Set click listener
            itemHolder.itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onAnalysisClick(item.analysis);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * ViewHolder for month header rows.
     */
    static class MonthHeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView monthHeader;

        MonthHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            monthHeader = itemView.findViewById(R.id.month_header);
        }

        void bind(String monthLabel) {
            monthHeader.setText(monthLabel);
        }
    }

    /**
     * ViewHolder for analysis item rows.
     */
    static class AnalysisItemViewHolder extends RecyclerView.ViewHolder {
        private final TextView healthScoreCircle;
        private final TextView summary;
        private final TextView date;
        private final TextView trendArrow;
        private final Context context;

        AnalysisItemViewHolder(@NonNull View itemView) {
            super(itemView);
            healthScoreCircle = itemView.findViewById(R.id.health_score_circle);
            summary = itemView.findViewById(R.id.analysis_summary);
            date = itemView.findViewById(R.id.analysis_date);
            trendArrow = itemView.findViewById(R.id.trend_arrow);
            context = itemView.getContext();
        }

        void bind(Analysis analysis, int trendDirection) {
            // Health score with colored background
            healthScoreCircle.setText(String.valueOf(analysis.healthScore));
            int colorRes = HealthUtils.getHealthColorRes(analysis.healthScore);
            GradientDrawable background = (GradientDrawable) healthScoreCircle.getBackground();
            background.setColor(ContextCompat.getColor(context, colorRes));

            // Summary text
            String summaryText = analysis.summary != null && !analysis.summary.isEmpty()
                    ? analysis.summary : "No summary available";
            summary.setText(summaryText);

            // Date formatting
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            date.setText(dateFormat.format(new Date(analysis.createdAt)));

            // Trend arrow
            if (trendDirection == 1) {
                // Improved - green up arrow
                trendArrow.setText("\u2191");
                trendArrow.setTextColor(ContextCompat.getColor(context, R.color.health_good));
                trendArrow.setVisibility(View.VISIBLE);
            } else if (trendDirection == -1) {
                // Declined - red down arrow
                trendArrow.setText("\u2193");
                trendArrow.setTextColor(ContextCompat.getColor(context, R.color.health_bad));
                trendArrow.setVisibility(View.VISIBLE);
            } else if (trendDirection == 0) {
                // Same - gray right arrow
                trendArrow.setText("\u2192");
                trendArrow.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
                trendArrow.setVisibility(View.VISIBLE);
            } else {
                // No previous (-2) - hide arrow
                trendArrow.setVisibility(View.GONE);
            }

            // PARTIAL/FAILED dimming
            boolean isDegraded = "PARTIAL".equals(analysis.parseStatus)
                    || "FAILED".equals(analysis.parseStatus);
            itemView.setAlpha(isDegraded ? 0.82f : 1.0f);
        }
    }
}
