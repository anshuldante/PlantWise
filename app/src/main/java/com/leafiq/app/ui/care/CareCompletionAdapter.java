package com.leafiq.app.ui.care;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.leafiq.app.R;
import com.leafiq.app.util.DateFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for care completion items in overview screen.
 * Shows simple row: emoji, "Watered Plant Name", timestamp.
 */
public class CareCompletionAdapter extends RecyclerView.Adapter<CareCompletionAdapter.ViewHolder> {

    private List<CareOverviewViewModel.CareCompletionItem> items = new ArrayList<>();
    private final Context context;

    public CareCompletionAdapter(Context context) {
        this.context = context;
    }

    public void setItems(List<CareOverviewViewModel.CareCompletionItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_care_completion_overview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CareOverviewViewModel.CareCompletionItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView emoji;
        private final TextView text;
        private final TextView timestamp;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            emoji = itemView.findViewById(R.id.completion_emoji);
            text = itemView.findViewById(R.id.completion_text);
            timestamp = itemView.findViewById(R.id.completion_timestamp);
        }

        void bind(CareOverviewViewModel.CareCompletionItem item) {
            // Care emoji
            emoji.setText(getCareEmoji(item.careType));

            // Completion text: "Watered Fern"
            String verb = getPastTenseVerb(item.careType);
            text.setText(verb + " " + item.plantDisplayName);

            // Relative timestamp
            timestamp.setText(DateFormatter.getRelativeTime(context, item.completedAt));
        }

        private String getCareEmoji(String careType) {
            switch (careType) {
                case "water":
                    return "💧";
                case "fertilize":
                    return "🌱";
                case "repot":
                    return "🪴";
                default:
                    return "🌿";
            }
        }

        private String getPastTenseVerb(String careType) {
            switch (careType) {
                case "water":
                    return "Watered";
                case "fertilize":
                    return "Fertilized";
                case "repot":
                    return "Repotted";
                default:
                    return "Cared for";
            }
        }
    }
}
