package com.leafiq.app.ui.detail;

import com.leafiq.app.data.entity.Analysis;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for FullAnalysisHistoryAdapter month grouping and trend arrow logic.
 * Tests the extracted buildHistoryItems() static method directly.
 */
@RunWith(JUnit4.class)
public class FullAnalysisHistoryAdapterTest {

    /**
     * Helper to create test Analysis objects.
     */
    private Analysis createAnalysis(String id, int healthScore, long createdAt, String parseStatus) {
        Analysis a = new Analysis();
        a.id = id;
        a.plantId = "test_plant";
        a.healthScore = healthScore;
        a.createdAt = createdAt;
        a.parseStatus = parseStatus;
        a.summary = "Test summary";
        return a;
    }

    /**
     * Helper to get timestamp for a specific date.
     */
    private long getTimestamp(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    // ==================== Month Grouping Tests ====================

    @Test
    public void testEmptyListProducesEmptyItems() {
        List<FullAnalysisHistoryAdapter.HistoryItem> items =
            FullAnalysisHistoryAdapter.buildHistoryItems(new ArrayList<>());
        assertEquals(0, items.size());
    }

    @Test
    public void testSingleAnalysisProducesHeaderAndItem() {
        List<Analysis> analyses = new ArrayList<>();
        analyses.add(createAnalysis("a1", 7, getTimestamp(2026, Calendar.FEBRUARY, 10), "OK"));

        List<FullAnalysisHistoryAdapter.HistoryItem> items =
            FullAnalysisHistoryAdapter.buildHistoryItems(analyses);

        // Should produce: 1 header + 1 item = 2 total
        assertEquals(2, items.size());
        assertEquals(FullAnalysisHistoryAdapter.TYPE_HEADER, items.get(0).type);
        assertEquals(FullAnalysisHistoryAdapter.TYPE_ITEM, items.get(1).type);
    }

    @Test
    public void testSameMonthGroupedUnderOneHeader() {
        List<Analysis> analyses = new ArrayList<>();
        analyses.add(createAnalysis("a1", 8, getTimestamp(2026, Calendar.FEBRUARY, 10), "OK"));
        analyses.add(createAnalysis("a2", 7, getTimestamp(2026, Calendar.FEBRUARY, 5), "OK"));
        analyses.add(createAnalysis("a3", 6, getTimestamp(2026, Calendar.FEBRUARY, 1), "OK"));

        List<FullAnalysisHistoryAdapter.HistoryItem> items =
            FullAnalysisHistoryAdapter.buildHistoryItems(analyses);

        // Should produce: 1 header + 3 items = 4 total
        assertEquals(4, items.size());
        assertEquals(FullAnalysisHistoryAdapter.TYPE_HEADER, items.get(0).type);
        assertEquals(FullAnalysisHistoryAdapter.TYPE_ITEM, items.get(1).type);
        assertEquals(FullAnalysisHistoryAdapter.TYPE_ITEM, items.get(2).type);
        assertEquals(FullAnalysisHistoryAdapter.TYPE_ITEM, items.get(3).type);
    }

    @Test
    public void testDifferentMonthsGetSeparateHeaders() {
        List<Analysis> analyses = new ArrayList<>();
        analyses.add(createAnalysis("a1", 8, getTimestamp(2026, Calendar.FEBRUARY, 10), "OK"));
        analyses.add(createAnalysis("a2", 7, getTimestamp(2026, Calendar.FEBRUARY, 5), "OK"));
        analyses.add(createAnalysis("a3", 6, getTimestamp(2026, Calendar.JANUARY, 25), "OK"));
        analyses.add(createAnalysis("a4", 5, getTimestamp(2026, Calendar.JANUARY, 15), "OK"));

        List<FullAnalysisHistoryAdapter.HistoryItem> items =
            FullAnalysisHistoryAdapter.buildHistoryItems(analyses);

        // Should produce: header(Feb) + 2 items + header(Jan) + 2 items = 6 total
        assertEquals(6, items.size());
        assertEquals(FullAnalysisHistoryAdapter.TYPE_HEADER, items.get(0).type); // Feb header
        assertEquals(FullAnalysisHistoryAdapter.TYPE_ITEM, items.get(1).type);   // Feb item
        assertEquals(FullAnalysisHistoryAdapter.TYPE_ITEM, items.get(2).type);   // Feb item
        assertEquals(FullAnalysisHistoryAdapter.TYPE_HEADER, items.get(3).type); // Jan header
        assertEquals(FullAnalysisHistoryAdapter.TYPE_ITEM, items.get(4).type);   // Jan item
        assertEquals(FullAnalysisHistoryAdapter.TYPE_ITEM, items.get(5).type);   // Jan item
    }

    @Test
    public void testMonthHeaderFormat() {
        List<Analysis> analyses = new ArrayList<>();
        analyses.add(createAnalysis("a1", 7, getTimestamp(2026, Calendar.FEBRUARY, 10), "OK"));

        List<FullAnalysisHistoryAdapter.HistoryItem> items =
            FullAnalysisHistoryAdapter.buildHistoryItems(analyses);

        // Verify header contains month label
        assertEquals(2, items.size());
        assertNotNull(items.get(0).monthLabel);
        // Month label should contain "February" and "2026"
        assertEquals(true, items.get(0).monthLabel.contains("2026"));
    }

    // ==================== Trend Arrow Tests ====================

    @Test
    public void testTrendArrowUp() {
        List<Analysis> analyses = new ArrayList<>();
        // Newer analysis (index 0) has higher score than older (index 1)
        analyses.add(createAnalysis("a1", 8, getTimestamp(2026, Calendar.FEBRUARY, 10), "OK"));
        analyses.add(createAnalysis("a2", 6, getTimestamp(2026, Calendar.FEBRUARY, 5), "OK"));

        List<FullAnalysisHistoryAdapter.HistoryItem> items =
            FullAnalysisHistoryAdapter.buildHistoryItems(analyses);

        // item at index 1 is the newer analysis
        assertEquals(FullAnalysisHistoryAdapter.TYPE_ITEM, items.get(1).type);
        assertEquals("a1", items.get(1).analysis.id);
        assertEquals(1, items.get(1).trendDirection); // up
    }

    @Test
    public void testTrendArrowDown() {
        List<Analysis> analyses = new ArrayList<>();
        // Newer analysis (index 0) has lower score than older (index 1)
        analyses.add(createAnalysis("a1", 5, getTimestamp(2026, Calendar.FEBRUARY, 10), "OK"));
        analyses.add(createAnalysis("a2", 8, getTimestamp(2026, Calendar.FEBRUARY, 5), "OK"));

        List<FullAnalysisHistoryAdapter.HistoryItem> items =
            FullAnalysisHistoryAdapter.buildHistoryItems(analyses);

        // item at index 1 is the newer analysis
        assertEquals(FullAnalysisHistoryAdapter.TYPE_ITEM, items.get(1).type);
        assertEquals("a1", items.get(1).analysis.id);
        assertEquals(-1, items.get(1).trendDirection); // down
    }

    @Test
    public void testTrendArrowSame() {
        List<Analysis> analyses = new ArrayList<>();
        // Newer analysis (index 0) has same score as older (index 1)
        analyses.add(createAnalysis("a1", 7, getTimestamp(2026, Calendar.FEBRUARY, 10), "OK"));
        analyses.add(createAnalysis("a2", 7, getTimestamp(2026, Calendar.FEBRUARY, 5), "OK"));

        List<FullAnalysisHistoryAdapter.HistoryItem> items =
            FullAnalysisHistoryAdapter.buildHistoryItems(analyses);

        // item at index 1 is the newer analysis
        assertEquals(FullAnalysisHistoryAdapter.TYPE_ITEM, items.get(1).type);
        assertEquals("a1", items.get(1).analysis.id);
        assertEquals(0, items.get(1).trendDirection); // same
    }

    @Test
    public void testFirstAnalysisNoArrow() {
        List<Analysis> analyses = new ArrayList<>();
        // Only one analysis - should have no trend arrow
        analyses.add(createAnalysis("a1", 7, getTimestamp(2026, Calendar.FEBRUARY, 10), "OK"));

        List<FullAnalysisHistoryAdapter.HistoryItem> items =
            FullAnalysisHistoryAdapter.buildHistoryItems(analyses);

        // item at index 1 is the only analysis
        assertEquals(FullAnalysisHistoryAdapter.TYPE_ITEM, items.get(1).type);
        assertEquals("a1", items.get(1).analysis.id);
        assertEquals(-2, items.get(1).trendDirection); // no previous
    }

    @Test
    public void testTrendDirectionOnlyComparesPreviousAnalysis() {
        List<Analysis> analyses = new ArrayList<>();
        // Three analyses to verify we only compare with immediate previous
        analyses.add(createAnalysis("a1", 8, getTimestamp(2026, Calendar.FEBRUARY, 10), "OK"));
        analyses.add(createAnalysis("a2", 6, getTimestamp(2026, Calendar.FEBRUARY, 5), "OK"));
        analyses.add(createAnalysis("a3", 4, getTimestamp(2026, Calendar.FEBRUARY, 1), "OK"));

        List<FullAnalysisHistoryAdapter.HistoryItem> items =
            FullAnalysisHistoryAdapter.buildHistoryItems(analyses);

        // Should produce: header + 3 items = 4 total
        assertEquals(4, items.size());

        // a1 should compare to a2 (8 vs 6 = up)
        assertEquals("a1", items.get(1).analysis.id);
        assertEquals(1, items.get(1).trendDirection);

        // a2 should compare to a3 (6 vs 4 = up)
        assertEquals("a2", items.get(2).analysis.id);
        assertEquals(1, items.get(2).trendDirection);

        // a3 is oldest - no previous to compare
        assertEquals("a3", items.get(3).analysis.id);
        assertEquals(-2, items.get(3).trendDirection);
    }
}
