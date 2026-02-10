package com.leafiq.app.ui.timeline;

import static com.google.common.truth.Truth.assertThat;

import com.leafiq.app.data.entity.Analysis;
import com.leafiq.app.data.model.AnalysisWithPlant;
import com.leafiq.app.ui.timeline.TimelineViewModel.TimelineItem;

import org.junit.Test;

public class TimelineItemTest {

    // ==================== Header tests ====================

    @Test
    public void header_storesLabel() {
        TimelineItem.Header header = new TimelineItem.Header("Today");
        assertThat(header.label).isEqualTo("Today");
    }

    @Test
    public void header_withYesterdayLabel() {
        TimelineItem.Header header = new TimelineItem.Header("Yesterday");
        assertThat(header.label).isEqualTo("Yesterday");
    }

    @Test
    public void header_withDateLabel() {
        TimelineItem.Header header = new TimelineItem.Header("Monday, February 3");
        assertThat(header.label).isEqualTo("Monday, February 3");
    }

    @Test
    public void header_isInstanceOfTimelineItem() {
        TimelineItem item = new TimelineItem.Header("Today");
        assertThat(item).isInstanceOf(TimelineItem.Header.class);
    }

    // ==================== Entry tests ====================

    @Test
    public void entry_storesDataAndExpansionState() {
        AnalysisWithPlant data = createTestAnalysisWithPlant("a1", "p1", 8);
        TimelineItem.Entry entry = new TimelineItem.Entry(data, false);

        assertThat(entry.data).isEqualTo(data);
        assertThat(entry.isExpanded).isFalse();
    }

    @Test
    public void entry_expanded_isTrue() {
        AnalysisWithPlant data = createTestAnalysisWithPlant("a1", "p1", 7);
        TimelineItem.Entry entry = new TimelineItem.Entry(data, true);

        assertThat(entry.isExpanded).isTrue();
    }

    @Test
    public void entry_isInstanceOfTimelineItem() {
        AnalysisWithPlant data = createTestAnalysisWithPlant("a1", "p1", 5);
        TimelineItem item = new TimelineItem.Entry(data, false);

        assertThat(item).isInstanceOf(TimelineItem.Entry.class);
    }

    @Test
    public void entry_dataFieldsAccessible() {
        AnalysisWithPlant data = createTestAnalysisWithPlant("a1", "p1", 9);
        data.plantCommonName = "Monstera";
        data.plantNickname = "Monty";

        TimelineItem.Entry entry = new TimelineItem.Entry(data, false);

        assertThat(entry.data.plantCommonName).isEqualTo("Monstera");
        assertThat(entry.data.plantNickname).isEqualTo("Monty");
        assertThat(entry.data.analysis.healthScore).isEqualTo(9);
    }

    @Test
    public void entry_toggledCopy_hasOppositeExpansion() {
        AnalysisWithPlant data = createTestAnalysisWithPlant("a1", "p1", 8);
        TimelineItem.Entry original = new TimelineItem.Entry(data, false);
        TimelineItem.Entry toggled = new TimelineItem.Entry(original.data, !original.isExpanded);

        assertThat(original.isExpanded).isFalse();
        assertThat(toggled.isExpanded).isTrue();
        assertThat(toggled.data).isSameInstanceAs(original.data);
    }

    @Test
    public void headerAndEntry_areDifferentTypes() {
        TimelineItem header = new TimelineItem.Header("Today");
        TimelineItem entry = new TimelineItem.Entry(
                createTestAnalysisWithPlant("a1", "p1", 7), false);

        assertThat(header).isInstanceOf(TimelineItem.Header.class);
        assertThat(entry).isInstanceOf(TimelineItem.Entry.class);
        assertThat(header).isNotInstanceOf(TimelineItem.Entry.class);
        assertThat(entry).isNotInstanceOf(TimelineItem.Header.class);
    }

    // ==================== HealthFilter enum tests ====================

    @Test
    public void healthFilter_hasFourValues() {
        TimelineViewModel.HealthFilter[] values = TimelineViewModel.HealthFilter.values();
        assertThat(values).hasLength(4);
    }

    @Test
    public void healthFilter_containsExpectedValues() {
        assertThat(TimelineViewModel.HealthFilter.valueOf("ALL")).isNotNull();
        assertThat(TimelineViewModel.HealthFilter.valueOf("HEALTHY")).isNotNull();
        assertThat(TimelineViewModel.HealthFilter.valueOf("NEEDS_ATTENTION")).isNotNull();
        assertThat(TimelineViewModel.HealthFilter.valueOf("CRITICAL")).isNotNull();
    }

    // ==================== Helpers ====================

    private AnalysisWithPlant createTestAnalysisWithPlant(String analysisId, String plantId, int healthScore) {
        AnalysisWithPlant awp = new AnalysisWithPlant();
        awp.analysis.id = analysisId;
        awp.analysis.plantId = plantId;
        awp.analysis.healthScore = healthScore;
        awp.analysis.createdAt = System.currentTimeMillis();
        return awp;
    }
}
