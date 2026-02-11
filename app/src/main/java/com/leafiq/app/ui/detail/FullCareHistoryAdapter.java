package com.leafiq.app.ui.detail;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.leafiq.app.R;
import com.leafiq.app.data.entity.CareCompletion;
import com.leafiq.app.data.entity.CareSchedule;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * RecyclerView adapter for full-screen care history with month grouping.
 * Displays TYPE_HEADER for month labels and TYPE_ITEM for care completion entries.
 *
 * Supports:
 * - Month grouping with headers
 * - Care type emojis (water, fertilize, repot)
 * - Status computation (On time/Late based on estimated due dates)
 * - No click listener (informational only per user decision)
 */
public class FullCareHistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    static final int TYPE_HEADER = 0;
    static final int TYPE_ITEM = 1;

    /**
     * Wrapper item for RecyclerView display.
     * Contains either a month header or a care completion item.
     */
    static class HistoryItem {
        int type;
        String monthLabel;          // for TYPE_HEADER
        CareCompletion completion;  // for TYPE_ITEM
        String careType;            // resolved from schedule
        String statusLabel;         // "On time" or "Late"
        int statusColor;            // color resource ID
    }

    private final List<HistoryItem> items = new ArrayList<>();

    public FullCareHistoryAdapter() {
    }

    /**
     * Set completions and build flattened list with month headers.
     *
     * @param completions List of completions sorted by completedAt DESC (newest first)
     * @param scheduleMap Map of scheduleId -> CareSchedule for status computation
     */
    public void setData(List<CareCompletion> completions, Map<String, CareSchedule> scheduleMap) {
        items.clear();

        if (completions == null || completions.isEmpty()) {
            notifyDataSetChanged();
            return;
        }

        // Group completions by scheduleId for status computation
        Map<String, List<CareCompletion>> completionsBySchedule = new HashMap<>();
        for (CareCompletion completion : completions) {
            if (!completionsBySchedule.containsKey(completion.scheduleId)) {
                completionsBySchedule.put(completion.scheduleId, new ArrayList<>());
            }
            completionsBySchedule.get(completion.scheduleId).add(completion);
        }

        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        String currentMonth = null;

        for (CareCompletion completion : completions) {
            // Check if we need a new month header
            String month = monthFormat.format(new Date(completion.completedAt));
            if (!month.equals(currentMonth)) {
                currentMonth = month;
                HistoryItem header = new HistoryItem();
                header.type = TYPE_HEADER;
                header.monthLabel = month;
                items.add(header);
            }

            // Add care completion item with status computation
            HistoryItem item = new HistoryItem();
            item.type = TYPE_ITEM;
            item.completion = completion;

            // Resolve care type from schedule
            CareSchedule schedule = scheduleMap.get(completion.scheduleId);
            if (schedule != null) {
                item.careType = schedule.careType;

                // Compute status: On time or Late
                // Walk backward from schedule.nextDue to estimate when this completion was due
                List<CareCompletion> scheduleCompletions = completionsBySchedule.get(completion.scheduleId);
                if (scheduleCompletions != null && !scheduleCompletions.isEmpty()) {
                    // Find position of this completion in the schedule's completion list
                    int positionInSchedule = scheduleCompletions.indexOf(completion);

                    // Estimate due date by walking backward from nextDue
                    // Most recent completion (index 0) was due at: nextDue - frequencyDays
                    // Second most recent (index 1) was due at: nextDue - (2 * frequencyDays)
                    // etc.
                    long frequencyMillis = schedule.frequencyDays * 24L * 60 * 60 * 1000;
                    long estimatedDue = schedule.nextDue - ((positionInSchedule + 1) * frequencyMillis);

                    // Check if completed within 1 day tolerance
                    long oneDayMillis = 24L * 60 * 60 * 1000;
                    if (completion.completedAt <= estimatedDue + oneDayMillis) {
                        item.statusLabel = "On time";
                        item.statusColor = R.color.health_good;
                    } else {
                        item.statusLabel = "Late";
                        item.statusColor = R.color.health_warning;
                    }
                } else {
                    // Fallback: assume on time if we can't compute
                    item.statusLabel = "On time";
                    item.statusColor = R.color.health_good;
                }
            } else {
                item.careType = "unknown";
                item.statusLabel = "On time";
                item.statusColor = R.color.health_good;
            }

            items.add(item);
        }

        notifyDataSetChanged();
    }

    /**
     * Get the CareCompletion at the given adapter position (for swipe-to-delete).
     * Returns null if position is a header.
     */
    public CareCompletion getCompletionAtPosition(int adapterPosition) {
        if (adapterPosition < 0 || adapterPosition >= items.size()) {
            return null;
        }
        HistoryItem item = items.get(adapterPosition);
        return item.type == TYPE_ITEM ? item.completion : null;
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
                    .inflate(R.layout.item_care_history_full, parent, false);
            return new CareItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        HistoryItem item = items.get(position);

        if (holder instanceof MonthHeaderViewHolder) {
            ((MonthHeaderViewHolder) holder).bind(item.monthLabel);
        } else if (holder instanceof CareItemViewHolder) {
            CareItemViewHolder itemHolder = (CareItemViewHolder) holder;
            itemHolder.bind(item.completion, item.careType, item.statusLabel, item.statusColor);
            // No click listener per user decision (tap does nothing)
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
     * ViewHolder for care completion item rows.
     */
    static class CareItemViewHolder extends RecyclerView.ViewHolder {
        private final TextView careTypeEmoji;
        private final TextView careActionName;
        private final TextView careDate;
        private final TextView statusBadge;
        private final Context context;

        CareItemViewHolder(@NonNull View itemView) {
            super(itemView);
            careTypeEmoji = itemView.findViewById(R.id.care_type_emoji);
            careActionName = itemView.findViewById(R.id.care_action_name);
            careDate = itemView.findViewById(R.id.care_date);
            statusBadge = itemView.findViewById(R.id.status_badge);
            context = itemView.getContext();
        }

        void bind(CareCompletion completion, String careType, String statusLabel, int statusColor) {
            // Set emoji
            careTypeEmoji.setText(getCareTypeEmoji(careType));

            // Set action name
            careActionName.setText(getCareActionName(careType));

            // Set date
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            careDate.setText(dateFormat.format(new Date(completion.completedAt)));

            // Set status badge
            statusBadge.setText(statusLabel);
            statusBadge.setTextColor(ContextCompat.getColor(context, statusColor));
        }

        private String getCareTypeEmoji(String careType) {
            switch (careType) {
                case "water":
                    return "💧";
                case "fertilize":
                    return "🌱";
                case "repot":
                    return "\uD83E\uDEB4";  // 🪴 (Unicode surrogate pair for Java)
                default:
                    return "✓";
            }
        }

        private String getCareActionName(String careType) {
            switch (careType) {
                case "water":
                    return "Watered";
                case "fertilize":
                    return "Fertilized";
                case "repot":
                    return "Repotted";
                default:
                    return "Completed";
            }
        }
    }
}
