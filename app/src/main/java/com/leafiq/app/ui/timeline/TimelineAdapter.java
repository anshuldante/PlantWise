package com.leafiq.app.ui.timeline;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.leafiq.app.R;
import com.leafiq.app.data.model.AnalysisWithPlant;
import com.leafiq.app.data.model.PlantAnalysisResult;
import com.leafiq.app.util.DateFormatter;
import com.leafiq.app.util.HealthUtils;
import com.leafiq.app.util.JsonParser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for timeline RecyclerView with expandable entries and date headers.
 * Supports two view types:
 * - HEADER: Date section labels (Today, Yesterday, date)
 * - ENTRY: Expandable analysis entries with collapsed/expanded states
 */
public class TimelineAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ENTRY = 1;

    private List<TimelineViewModel.TimelineItem> items = new ArrayList<>();
    private OnTimelineItemClickListener listener;
    private Context context;

    /**
     * Listener interface for timeline item interactions.
     */
    public interface OnTimelineItemClickListener {
        void onItemClick(int position);
        void onViewFullAnalysis(AnalysisWithPlant data);
    }

    public TimelineAdapter(Context context, OnTimelineItemClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        TimelineViewModel.TimelineItem item = items.get(position);
        if (item instanceof TimelineViewModel.TimelineItem.Header) {
            return VIEW_TYPE_HEADER;
        } else {
            return VIEW_TYPE_ENTRY;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_timeline_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_timeline_entry, parent, false);
            return new EntryViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TimelineViewModel.TimelineItem item = items.get(position);
        if (holder instanceof HeaderViewHolder) {
            TimelineViewModel.TimelineItem.Header header = (TimelineViewModel.TimelineItem.Header) item;
            ((HeaderViewHolder) holder).bind(header.label);
        } else if (holder instanceof EntryViewHolder) {
            TimelineViewModel.TimelineItem.Entry entry = (TimelineViewModel.TimelineItem.Entry) item;
            ((EntryViewHolder) holder).bind(entry, listener);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Submit new list of timeline items with diff calculation.
     */
    public void submitList(List<TimelineViewModel.TimelineItem> newItems) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new TimelineDiffCallback(items, newItems));
        items.clear();
        items.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
    }

    /**
     * ViewHolder for date section headers.
     */
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private TextView headerText;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            headerText = itemView.findViewById(R.id.header_text);
        }

        public void bind(String label) {
            headerText.setText(label);
        }
    }

    /**
     * ViewHolder for expandable entry rows.
     */
    class EntryViewHolder extends RecyclerView.ViewHolder {
        // Collapsed views
        private ShapeableImageView plantIcon;
        private TextView plantName;
        private TextView healthBadge;
        private TextView timestamp;
        private ImageView chevron;

        // Expanded views
        private LinearLayout expandedContent;
        private ShapeableImageView entryPhoto;
        private TextView healthLabel;
        private TextView diagnosis;
        private LinearLayout careHighlights;
        private TextView careHighlight1;
        private TextView careHighlight2;
        private MaterialButton btnViewFull;

        public EntryViewHolder(@NonNull View itemView) {
            super(itemView);

            // Collapsed views
            plantIcon = itemView.findViewById(R.id.plant_icon);
            plantName = itemView.findViewById(R.id.entry_plant_name);
            healthBadge = itemView.findViewById(R.id.entry_health_badge);
            timestamp = itemView.findViewById(R.id.entry_timestamp);
            chevron = itemView.findViewById(R.id.entry_chevron);

            // Expanded views
            expandedContent = itemView.findViewById(R.id.expanded_content);
            entryPhoto = itemView.findViewById(R.id.entry_photo);
            healthLabel = itemView.findViewById(R.id.entry_health_label);
            diagnosis = itemView.findViewById(R.id.entry_diagnosis);
            careHighlights = itemView.findViewById(R.id.care_highlights);
            careHighlight1 = itemView.findViewById(R.id.care_highlight_1);
            careHighlight2 = itemView.findViewById(R.id.care_highlight_2);
            btnViewFull = itemView.findViewById(R.id.btn_view_full);
        }

        public void bind(TimelineViewModel.TimelineItem.Entry entry, OnTimelineItemClickListener listener) {
            AnalysisWithPlant data = entry.data;

            // Collapsed content
            bindCollapsedContent(data, entry.isExpanded);

            // Expanded content
            if (entry.isExpanded) {
                expandedContent.setVisibility(View.VISIBLE);
                bindExpandedContent(data);
            } else {
                expandedContent.setVisibility(View.GONE);
            }

            // Click listener on root to toggle expansion
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(getAdapterPosition());
                }
            });

            // View full analysis button
            btnViewFull.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewFullAnalysis(data);
                }
            });
        }

        private void bindCollapsedContent(AnalysisWithPlant data, boolean isExpanded) {
            // Plant icon - circular crop
            if (data.plantThumbnailPath != null && !data.plantThumbnailPath.isEmpty()) {
                Glide.with(context)
                        .load(new File(data.plantThumbnailPath))
                        .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                        .placeholder(R.drawable.ic_plant_placeholder)
                        .into(plantIcon);
            } else {
                plantIcon.setImageResource(R.drawable.ic_plant_placeholder);
            }

            // Plant name - nickname takes precedence
            String displayName;
            if (data.plantNickname != null && !data.plantNickname.isEmpty()) {
                displayName = data.plantNickname;
            } else if (data.plantCommonName != null && !data.plantCommonName.isEmpty()) {
                displayName = data.plantCommonName;
            } else {
                displayName = "Unknown Plant";
            }
            plantName.setText(displayName);

            // Health badge - text label with color (no numeric score)
            String healthLabelText = HealthUtils.getHealthLabel(data.analysis.healthScore);
            int healthColorRes = HealthUtils.getHealthColorRes(data.analysis.healthScore);
            healthBadge.setText(healthLabelText);

            // Set badge background color using GradientDrawable
            GradientDrawable badgeBackground = new GradientDrawable();
            badgeBackground.setShape(GradientDrawable.OVAL);
            badgeBackground.setColor(ContextCompat.getColor(context, healthColorRes));
            badgeBackground.setCornerRadius(100); // Large radius for pill shape
            healthBadge.setBackground(badgeBackground);

            // Timestamp - relative time
            String relativeTime = DateFormatter.getRelativeTime(context, data.analysis.createdAt);
            timestamp.setText(relativeTime);

            // Chevron rotation
            float rotation = isExpanded ? 180f : 0f;
            chevron.setRotation(rotation);
        }

        private void bindExpandedContent(AnalysisWithPlant data) {
            // Photo thumbnail
            if (data.analysis.photoPath != null && !data.analysis.photoPath.isEmpty()) {
                Glide.with(context)
                        .load(new File(data.analysis.photoPath))
                        .centerCrop()
                        .placeholder(R.drawable.ic_plant_placeholder)
                        .into(entryPhoto);
            } else {
                entryPhoto.setImageResource(R.drawable.ic_plant_placeholder);
            }

            // Health label with color
            String healthLabelText = HealthUtils.getHealthLabel(data.analysis.healthScore);
            int healthColorRes = HealthUtils.getHealthColorRes(data.analysis.healthScore);
            healthLabel.setText(healthLabelText);
            healthLabel.setTextColor(ContextCompat.getColor(context, healthColorRes));

            // Diagnosis - truncate if needed
            String diagnosisText = data.analysis.summary;
            if (diagnosisText == null || diagnosisText.isEmpty()) {
                diagnosisText = "No diagnosis available";
            } else if (diagnosisText.length() > 100) {
                diagnosisText = diagnosisText.substring(0, 100) + "...";
            }
            diagnosis.setText(diagnosisText);

            // Care highlights - parse from rawResponse
            bindCareHighlights(data);
        }

        private void bindCareHighlights(AnalysisWithPlant data) {
            List<String> highlights = extractCareHighlights(data);

            if (highlights.isEmpty()) {
                careHighlights.setVisibility(View.GONE);
            } else {
                careHighlights.setVisibility(View.VISIBLE);

                // Show first highlight
                if (highlights.size() >= 1) {
                    careHighlight1.setText("• " + highlights.get(0));
                    careHighlight1.setVisibility(View.VISIBLE);
                } else {
                    careHighlight1.setVisibility(View.GONE);
                }

                // Show second highlight
                if (highlights.size() >= 2) {
                    careHighlight2.setText("• " + highlights.get(1));
                    careHighlight2.setVisibility(View.VISIBLE);
                } else {
                    careHighlight2.setVisibility(View.GONE);
                }
            }
        }

        /**
         * Extract up to 2 critical care highlights from analysis.
         * Priority: immediateActions > care plan adjustments.
         */
        private List<String> extractCareHighlights(AnalysisWithPlant data) {
            List<String> highlights = new ArrayList<>();

            if (data.analysis.rawResponse == null || data.analysis.rawResponse.isEmpty()) {
                return highlights;
            }

            try {
                PlantAnalysisResult parsed = JsonParser.parsePlantAnalysis(data.analysis.rawResponse);

                // Priority 1: Immediate actions (urgent or soon)
                if (parsed.immediateActions != null) {
                    for (PlantAnalysisResult.ImmediateAction action : parsed.immediateActions) {
                        if (highlights.size() >= 2) break;

                        if ("urgent".equals(action.priority) || "soon".equals(action.priority)) {
                            if (action.action != null && !action.action.isEmpty()) {
                                highlights.add(action.action);
                            }
                        }
                    }
                }

                // Priority 2: Care plan adjustments (if not at max)
                if (highlights.size() < 2 && parsed.carePlan != null) {
                    // Light adjustment
                    if (highlights.size() < 2 && parsed.carePlan.light != null
                            && parsed.carePlan.light.adjustment != null
                            && !parsed.carePlan.light.adjustment.isEmpty()) {
                        highlights.add(parsed.carePlan.light.adjustment);
                    }

                    // Watering notes
                    if (highlights.size() < 2 && parsed.carePlan.watering != null
                            && parsed.carePlan.watering.notes != null
                            && !parsed.carePlan.watering.notes.isEmpty()) {
                        highlights.add(parsed.carePlan.watering.notes);
                    }
                }

            } catch (Exception e) {
                // Parsing failed - no highlights
            }

            return highlights;
        }
    }

    /**
     * DiffUtil callback for efficient list updates.
     */
    static class TimelineDiffCallback extends DiffUtil.Callback {
        private List<TimelineViewModel.TimelineItem> oldList;
        private List<TimelineViewModel.TimelineItem> newList;

        public TimelineDiffCallback(List<TimelineViewModel.TimelineItem> oldList,
                                   List<TimelineViewModel.TimelineItem> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            TimelineViewModel.TimelineItem oldItem = oldList.get(oldItemPosition);
            TimelineViewModel.TimelineItem newItem = newList.get(newItemPosition);

            // Both must be same type
            if (oldItem.getClass() != newItem.getClass()) {
                return false;
            }

            // Headers: same if same label
            if (oldItem instanceof TimelineViewModel.TimelineItem.Header) {
                TimelineViewModel.TimelineItem.Header oldHeader = (TimelineViewModel.TimelineItem.Header) oldItem;
                TimelineViewModel.TimelineItem.Header newHeader = (TimelineViewModel.TimelineItem.Header) newItem;
                return oldHeader.label.equals(newHeader.label);
            }

            // Entries: same if same analysis ID
            if (oldItem instanceof TimelineViewModel.TimelineItem.Entry) {
                TimelineViewModel.TimelineItem.Entry oldEntry = (TimelineViewModel.TimelineItem.Entry) oldItem;
                TimelineViewModel.TimelineItem.Entry newEntry = (TimelineViewModel.TimelineItem.Entry) newItem;
                return oldEntry.data.analysis.id.equals(newEntry.data.analysis.id);
            }

            return false;
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            TimelineViewModel.TimelineItem oldItem = oldList.get(oldItemPosition);
            TimelineViewModel.TimelineItem newItem = newList.get(newItemPosition);

            // Headers: contents same if label same (already checked in areItemsTheSame)
            if (oldItem instanceof TimelineViewModel.TimelineItem.Header) {
                return true;
            }

            // Entries: contents same if expansion state and health score match
            if (oldItem instanceof TimelineViewModel.TimelineItem.Entry) {
                TimelineViewModel.TimelineItem.Entry oldEntry = (TimelineViewModel.TimelineItem.Entry) oldItem;
                TimelineViewModel.TimelineItem.Entry newEntry = (TimelineViewModel.TimelineItem.Entry) newItem;
                return oldEntry.isExpanded == newEntry.isExpanded
                        && oldEntry.data.analysis.healthScore == newEntry.data.analysis.healthScore;
            }

            return false;
        }
    }
}
