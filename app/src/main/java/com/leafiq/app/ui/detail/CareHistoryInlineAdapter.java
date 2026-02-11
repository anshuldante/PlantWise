package com.leafiq.app.ui.detail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.leafiq.app.R;
import com.leafiq.app.data.entity.CareCompletion;
import com.leafiq.app.data.entity.CareSchedule;
import com.leafiq.app.util.DateFormatter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Inline adapter for care history on plant detail page.
 * Displays compact care completion entries with emoji, action, date, and status.
 */
public class CareHistoryInlineAdapter extends ListAdapter<CareCompletion, CareHistoryInlineAdapter.ViewHolder> {

    private Map<String, String> careTypeMap = new HashMap<>();
    private Map<String, CareSchedule> scheduleMap = new HashMap<>();

    public CareHistoryInlineAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<CareCompletion> DIFF_CALLBACK =
        new DiffUtil.ItemCallback<CareCompletion>() {
            @Override
            public boolean areItemsTheSame(@NonNull CareCompletion oldItem, @NonNull CareCompletion newItem) {
                return oldItem.id.equals(newItem.id);
            }

            @Override
            public boolean areContentsTheSame(@NonNull CareCompletion oldItem, @NonNull CareCompletion newItem) {
                return oldItem.completedAt == newItem.completedAt
                    && oldItem.source.equals(newItem.source);
            }
        };

    /**
     * Set the care type map for emoji/name display.
     * Map format: scheduleId -> careType
     */
    public void setCareTypeMap(Map<String, String> careTypeMap) {
        this.careTypeMap = careTypeMap;
    }

    /**
     * Set the schedule map for timing computation.
     * Map format: scheduleId -> CareSchedule
     */
    public void setScheduleMap(Map<String, CareSchedule> scheduleMap) {
        this.scheduleMap = scheduleMap;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_care_history_inline, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CareCompletion completion = getItem(position);
        CareSchedule schedule = scheduleMap.get(completion.scheduleId);
        holder.bind(completion, careTypeMap, schedule);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView careEmoji;
        private final TextView careActionText;
        private final TextView careDateText;
        private final TextView careStatusLabel;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            careEmoji = itemView.findViewById(R.id.care_emoji);
            careActionText = itemView.findViewById(R.id.care_action_text);
            careDateText = itemView.findViewById(R.id.care_date_text);
            careStatusLabel = itemView.findViewById(R.id.care_status_label);
        }

        void bind(CareCompletion completion, Map<String, String> careTypeMap, CareSchedule schedule) {
            String careType = careTypeMap.get(completion.scheduleId);
            if (careType == null) {
                careType = "unknown";
            }

            // Emoji
            careEmoji.setText(getCareTypeEmoji(careType));

            // Action text
            careActionText.setText(getCareActionText(careType));

            // Date
            careDateText.setText(DateFormatter.getRelativeTime(itemView.getContext(), completion.completedAt));

            // Status computation (On time, Late, Skipped)
            if (schedule != null) {
                String status = computeStatus(completion, schedule);
                careStatusLabel.setText(status);

                // Color based on status
                int colorRes;
                if (status.equals("On time")) {
                    colorRes = R.color.health_good;
                } else if (status.equals("Late")) {
                    colorRes = R.color.health_warning;
                } else {
                    colorRes = R.color.text_secondary;
                }
                careStatusLabel.setTextColor(ContextCompat.getColor(itemView.getContext(), colorRes));
            } else {
                careStatusLabel.setVisibility(View.GONE);
            }
        }

        private String getCareTypeEmoji(String careType) {
            switch (careType) {
                case "water":
                    return "💧";
                case "fertilize":
                    return "🌱";
                case "repot":
                    return "🪴";
                default:
                    return "✓";
            }
        }

        private String getCareActionText(String careType) {
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

        /**
         * Compute status based on completion timing vs schedule nextDue.
         * On time: completed within 1 day of due date
         * Late: completed > 1 day after due date
         */
        private String computeStatus(CareCompletion completion, CareSchedule schedule) {
            long completedAt = completion.completedAt;

            // Reconstruct the expected due date at time of completion
            // This is approximate - ideally we'd store expectedDueAt in the completion
            // For now, use a simple heuristic: if completed within 1 day of a frequency cycle, it's on time
            long frequencyMs = schedule.frequencyDays * 24L * 60 * 60 * 1000;
            long timeSinceCompletion = System.currentTimeMillis() - completedAt;
            long cyclesSinceCompletion = timeSinceCompletion / frequencyMs;

            // Reconstruct what the due date was at completion time
            long reconstructedDueAt = schedule.nextDue - (cyclesSinceCompletion * frequencyMs);

            long timeDiff = completedAt - reconstructedDueAt;
            long daysDiff = TimeUnit.MILLISECONDS.toDays(Math.abs(timeDiff));

            if (daysDiff <= 1) {
                return "On time";
            } else if (timeDiff > 0) {
                return "Late";
            } else {
                return "Early";
            }
        }
    }
}
