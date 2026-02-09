package com.leafiq.app.ui.care;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.button.MaterialButton;
import com.leafiq.app.R;
import com.leafiq.app.util.DateFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for care task items (today's and upcoming tasks).
 * Displays plant thumbnail, name, care type with emoji, due date, and Done/Snooze buttons.
 */
public class CareTaskAdapter extends RecyclerView.Adapter<CareTaskAdapter.ViewHolder> {

    private List<CareOverviewViewModel.CareTaskItem> items = new ArrayList<>();
    private final OnTaskActionListener listener;
    private final Context context;

    public interface OnTaskActionListener {
        void onDoneClicked(CareOverviewViewModel.CareTaskItem item);
        void onSnoozeClicked(CareOverviewViewModel.CareTaskItem item);
    }

    public CareTaskAdapter(Context context, OnTaskActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<CareOverviewViewModel.CareTaskItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_care_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CareOverviewViewModel.CareTaskItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView thumbnail;
        private final TextView plantName;
        private final TextView careType;
        private final TextView dueDate;
        private final MaterialButton doneBtn;
        private final MaterialButton snoozeBtn;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.task_plant_thumbnail);
            plantName = itemView.findViewById(R.id.task_plant_name);
            careType = itemView.findViewById(R.id.task_care_type);
            dueDate = itemView.findViewById(R.id.task_due_date);
            doneBtn = itemView.findViewById(R.id.task_done_btn);
            snoozeBtn = itemView.findViewById(R.id.task_snooze_btn);
        }

        void bind(CareOverviewViewModel.CareTaskItem item) {
            // Plant display name (nickname > commonName > "Your plant")
            String displayName = getPlantDisplayName(item.plant);
            plantName.setText(displayName);

            // Care type with emoji
            String emoji = getCareEmoji(item.schedule.careType);
            String verb = getCareVerb(item.schedule.careType);
            careType.setText(emoji + " " + verb);

            // Due date (relative time)
            String dueDateText = DateFormatter.getRelativeTime(context, item.schedule.nextDue);
            dueDate.setText(dueDateText);

            // Plant thumbnail
            if (item.plant.thumbnailPath != null && !item.plant.thumbnailPath.isEmpty()) {
                Glide.with(context)
                        .load(item.plant.thumbnailPath)
                        .transform(new CircleCrop())
                        .placeholder(R.drawable.ic_plant_placeholder)
                        .into(thumbnail);
            } else {
                Glide.with(context)
                        .load(R.drawable.ic_plant_placeholder)
                        .transform(new CircleCrop())
                        .into(thumbnail);
            }

            // Done button
            doneBtn.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDoneClicked(item);
                }
            });

            // Snooze button
            snoozeBtn.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSnoozeClicked(item);
                }
            });
        }

        private String getPlantDisplayName(com.leafiq.app.data.entity.Plant plant) {
            if (plant.nickname != null && !plant.nickname.isEmpty()) {
                return plant.nickname;
            }
            if (plant.commonName != null && !plant.commonName.isEmpty()) {
                return plant.commonName;
            }
            return "Your plant";
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

        private String getCareVerb(String careType) {
            switch (careType) {
                case "water":
                    return "Watering";
                case "fertilize":
                    return "Fertilizing";
                case "repot":
                    return "Repotting";
                default:
                    return "Care";
            }
        }
    }
}
