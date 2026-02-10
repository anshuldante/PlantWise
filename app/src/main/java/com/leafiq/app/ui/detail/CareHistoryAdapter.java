package com.leafiq.app.ui.detail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.leafiq.app.R;
import com.leafiq.app.data.entity.CareCompletion;
import com.leafiq.app.util.DateFormatter;

import java.util.HashMap;
import java.util.Map;

/**
 * RecyclerView adapter for care completion history.
 * Displays completion records with care type emoji, timestamp, and source.
 */
public class CareHistoryAdapter extends ListAdapter<CareCompletion, CareHistoryAdapter.ViewHolder> {

    private Map<String, String> careTypeMap = new HashMap<>();

    public CareHistoryAdapter() {
        super(new DiffUtil.ItemCallback<CareCompletion>() {
            @Override
            public boolean areItemsTheSame(@NonNull CareCompletion oldItem, @NonNull CareCompletion newItem) {
                return oldItem.id.equals(newItem.id);
            }

            @Override
            public boolean areContentsTheSame(@NonNull CareCompletion oldItem, @NonNull CareCompletion newItem) {
                return oldItem.completedAt == newItem.completedAt
                        && oldItem.source.equals(newItem.source);
            }
        });
    }

    /**
     * Sets the care type map for displaying care type emojis and names.
     * Map format: scheduleId -> careType
     */
    public void setCareTypeMap(Map<String, String> careTypeMap) {
        this.careTypeMap = careTypeMap;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_care_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CareCompletion completion = getItem(position);
        holder.bind(completion, careTypeMap);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView careTypeIcon;
        private final TextView careCompletionText;
        private final TextView careCompletionTime;
        private final TextView careCompletionSource;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            careTypeIcon = itemView.findViewById(R.id.care_type_icon);
            careCompletionText = itemView.findViewById(R.id.care_completion_text);
            careCompletionTime = itemView.findViewById(R.id.care_completion_time);
            careCompletionSource = itemView.findViewById(R.id.care_completion_source);
        }

        public void bind(CareCompletion completion, Map<String, String> careTypeMap) {
            String careType = careTypeMap.get(completion.scheduleId);
            if (careType == null) {
                careType = "unknown";
            }

            // Set emoji
            careTypeIcon.setText(getCareTypeEmoji(careType));

            // Set completion text
            careCompletionText.setText(getCareCompletionText(careType));

            // Set timestamp
            careCompletionTime.setText(DateFormatter.getRelativeTime(itemView.getContext(), completion.completedAt));

            // Set source
            String source = getSourceLabel(completion.source);
            careCompletionSource.setText(source);
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

        private String getCareCompletionText(String careType) {
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

        private String getSourceLabel(String source) {
            if (source == null) {
                return "";
            }
            switch (source) {
                case "notification_action":
                    return itemView.getContext().getString(R.string.via_notification);
                case "in_app":
                    return itemView.getContext().getString(R.string.in_app);
                case "snooze":
                    return "snoozed";
                default:
                    return source;
            }
        }
    }
}
