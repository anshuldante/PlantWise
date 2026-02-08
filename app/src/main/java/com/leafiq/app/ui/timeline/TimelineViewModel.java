package com.leafiq.app.ui.timeline;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.leafiq.app.LeafIQApplication;
import com.leafiq.app.data.model.AnalysisWithPlant;
import com.leafiq.app.data.repository.PlantRepository;
import com.leafiq.app.util.HealthUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * ViewModel for the Timeline screen.
 * Manages filtering and date grouping of all analyses across all plants.
 */
public class TimelineViewModel extends AndroidViewModel {

    /**
     * Health filter options for timeline display.
     */
    public enum HealthFilter {
        ALL,
        HEALTHY,
        NEEDS_ATTENTION,
        CRITICAL
    }

    /**
     * Base class for timeline items (headers and entries).
     */
    public static abstract class TimelineItem {
        /**
         * Date section header.
         */
        public static class Header extends TimelineItem {
            public final String label;

            public Header(String label) {
                this.label = label;
            }
        }

        /**
         * Analysis entry with expansion state.
         */
        public static class Entry extends TimelineItem {
            public final AnalysisWithPlant data;
            public final boolean isExpanded;

            public Entry(AnalysisWithPlant data, boolean isExpanded) {
                this.data = data;
                this.isExpanded = isExpanded;
            }
        }
    }

    private final PlantRepository repository;
    private final MutableLiveData<HealthFilter> currentFilter;
    private final MediatorLiveData<List<TimelineItem>> timelineItems;

    public TimelineViewModel(@NonNull Application application) {
        super(application);
        this.repository = ((LeafIQApplication) application).getPlantRepository();
        this.currentFilter = new MutableLiveData<>(HealthFilter.ALL);
        this.timelineItems = new MediatorLiveData<>();

        // Combine raw data with filter to produce filtered + grouped list
        LiveData<List<AnalysisWithPlant>> rawData = repository.getAllAnalysesWithPlant();

        timelineItems.addSource(rawData, analyses -> {
            timelineItems.setValue(
                groupByDateWithFilter(analyses, currentFilter.getValue())
            );
        });

        timelineItems.addSource(currentFilter, filter -> {
            timelineItems.setValue(
                groupByDateWithFilter(rawData.getValue(), filter)
            );
        });
    }

    /**
     * Get the timeline items (headers + entries) as LiveData.
     */
    public LiveData<List<TimelineItem>> getTimelineItems() {
        return timelineItems;
    }

    /**
     * Set the health filter.
     */
    public void setFilter(HealthFilter filter) {
        currentFilter.setValue(filter);
    }

    /**
     * Toggle expansion state for an entry at the given position.
     */
    public void toggleExpansion(int position) {
        List<TimelineItem> currentItems = timelineItems.getValue();
        if (currentItems == null || position < 0 || position >= currentItems.size()) {
            return;
        }

        TimelineItem item = currentItems.get(position);
        if (!(item instanceof TimelineItem.Entry)) {
            return;
        }

        // Create new list with toggled expansion
        List<TimelineItem> newItems = new ArrayList<>(currentItems);
        TimelineItem.Entry entry = (TimelineItem.Entry) item;
        newItems.set(position, new TimelineItem.Entry(entry.data, !entry.isExpanded));
        timelineItems.setValue(newItems);
    }

    /**
     * Filter and group analyses by date.
     */
    private List<TimelineItem> groupByDateWithFilter(
            List<AnalysisWithPlant> analyses,
            HealthFilter filter
    ) {
        if (analyses == null) {
            return new ArrayList<>();
        }

        List<TimelineItem> items = new ArrayList<>();
        String lastDateLabel = null;

        for (AnalysisWithPlant analysis : analyses) {
            // Apply health filter
            if (!matchesFilter(analysis, filter)) {
                continue;
            }

            // Get date label for this analysis
            String dateLabel = getDateLabel(analysis.analysis.createdAt);

            // Add header if date changed
            if (!dateLabel.equals(lastDateLabel)) {
                items.add(new TimelineItem.Header(dateLabel));
                lastDateLabel = dateLabel;
            }

            // Add entry (not expanded by default)
            items.add(new TimelineItem.Entry(analysis, false));
        }

        return items;
    }

    /**
     * Check if analysis matches the current filter.
     */
    private boolean matchesFilter(AnalysisWithPlant analysis, HealthFilter filter) {
        if (filter == HealthFilter.ALL) {
            return true;
        }

        int healthScore = analysis.analysis.healthScore;

        switch (filter) {
            case HEALTHY:
                return healthScore >= 7;
            case NEEDS_ATTENTION:
                return healthScore >= 4 && healthScore < 7;
            case CRITICAL:
                return healthScore < 4;
            default:
                return true;
        }
    }

    /**
     * Get date label for grouping (Today, Yesterday, date).
     */
    private String getDateLabel(long timestamp) {
        Calendar analysisDate = Calendar.getInstance();
        analysisDate.setTimeInMillis(timestamp);

        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        if (isSameDay(analysisDate, today)) {
            return "Today";
        } else if (isSameDay(analysisDate, yesterday)) {
            return "Yesterday";
        } else {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d", Locale.US);
            return dateFormat.format(new Date(timestamp));
        }
    }

    /**
     * Check if two calendars represent the same day.
     */
    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
}
