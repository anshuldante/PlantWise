package com.leafiq.app.ui.detail;

import com.leafiq.app.R;
import com.leafiq.app.data.entity.CareCompletion;
import com.leafiq.app.data.entity.CareSchedule;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for FullCareHistoryAdapter month grouping and status computation.
 * Tests the extracted buildHistoryItems() static method directly.
 */
@RunWith(JUnit4.class)
public class FullCareHistoryAdapterTest {

    /**
     * Helper to create test CareCompletion objects.
     */
    private CareCompletion createCompletion(String id, String scheduleId, long completedAt, String source) {
        CareCompletion c = new CareCompletion();
        c.id = id;
        c.scheduleId = scheduleId;
        c.completedAt = completedAt;
        c.source = source;
        return c;
    }

    /**
     * Helper to create test CareSchedule objects.
     */
    private CareSchedule createSchedule(String id, String careType, int frequencyDays) {
        CareSchedule s = new CareSchedule();
        s.id = id;
        s.plantId = "test_plant";
        s.careType = careType;
        s.frequencyDays = frequencyDays;
        // Set nextDue to a known future date for status computation
        s.nextDue = getTimestamp(2026, Calendar.MARCH, 1);
        return s;
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
        List<FullCareHistoryAdapter.HistoryItem> items =
            FullCareHistoryAdapter.buildHistoryItems(new ArrayList<>(), new HashMap<>());
        assertEquals(0, items.size());
    }

    @Test
    public void testSingleCompletionProducesHeaderAndItem() {
        List<CareCompletion> completions = new ArrayList<>();
        completions.add(createCompletion("c1", "s1", getTimestamp(2026, Calendar.FEBRUARY, 10), "done"));

        Map<String, CareSchedule> scheduleMap = new HashMap<>();
        scheduleMap.put("s1", createSchedule("s1", "water", 7));

        List<FullCareHistoryAdapter.HistoryItem> items =
            FullCareHistoryAdapter.buildHistoryItems(completions, scheduleMap);

        // Should produce: 1 header + 1 item = 2 total
        assertEquals(2, items.size());
        assertEquals(FullCareHistoryAdapter.TYPE_HEADER, items.get(0).type);
        assertEquals(FullCareHistoryAdapter.TYPE_ITEM, items.get(1).type);
    }

    @Test
    public void testCorrectMonthGrouping() {
        List<CareCompletion> completions = new ArrayList<>();
        completions.add(createCompletion("c1", "s1", getTimestamp(2026, Calendar.FEBRUARY, 10), "done"));
        completions.add(createCompletion("c2", "s1", getTimestamp(2026, Calendar.FEBRUARY, 5), "done"));
        completions.add(createCompletion("c3", "s1", getTimestamp(2026, Calendar.JANUARY, 25), "done"));

        Map<String, CareSchedule> scheduleMap = new HashMap<>();
        scheduleMap.put("s1", createSchedule("s1", "water", 7));

        List<FullCareHistoryAdapter.HistoryItem> items =
            FullCareHistoryAdapter.buildHistoryItems(completions, scheduleMap);

        // Should produce: header(Feb) + 2 items + header(Jan) + 1 item = 5 total
        assertEquals(5, items.size());
        assertEquals(FullCareHistoryAdapter.TYPE_HEADER, items.get(0).type); // Feb header
        assertEquals(FullCareHistoryAdapter.TYPE_ITEM, items.get(1).type);   // Feb item
        assertEquals(FullCareHistoryAdapter.TYPE_ITEM, items.get(2).type);   // Feb item
        assertEquals(FullCareHistoryAdapter.TYPE_HEADER, items.get(3).type); // Jan header
        assertEquals(FullCareHistoryAdapter.TYPE_ITEM, items.get(4).type);   // Jan item
    }

    // ==================== Care Type Resolution Tests ====================

    @Test
    public void testCareTypeResolvedFromScheduleMap() {
        List<CareCompletion> completions = new ArrayList<>();
        completions.add(createCompletion("c1", "s1", getTimestamp(2026, Calendar.FEBRUARY, 10), "done"));
        completions.add(createCompletion("c2", "s2", getTimestamp(2026, Calendar.FEBRUARY, 8), "done"));
        completions.add(createCompletion("c3", "s3", getTimestamp(2026, Calendar.FEBRUARY, 5), "done"));

        Map<String, CareSchedule> scheduleMap = new HashMap<>();
        scheduleMap.put("s1", createSchedule("s1", "water", 7));
        scheduleMap.put("s2", createSchedule("s2", "fertilize", 14));
        scheduleMap.put("s3", createSchedule("s3", "repot", 90));

        List<FullCareHistoryAdapter.HistoryItem> items =
            FullCareHistoryAdapter.buildHistoryItems(completions, scheduleMap);

        // Verify care types are resolved
        assertEquals("c1", items.get(1).completion.id);
        assertEquals("water", items.get(1).careType);

        assertEquals("c2", items.get(2).completion.id);
        assertEquals("fertilize", items.get(2).careType);

        assertEquals("c3", items.get(3).completion.id);
        assertEquals("repot", items.get(3).careType);
    }

    @Test
    public void testUnknownScheduleIdDefaultsCareType() {
        List<CareCompletion> completions = new ArrayList<>();
        completions.add(createCompletion("c1", "unknown_schedule", getTimestamp(2026, Calendar.FEBRUARY, 10), "done"));

        Map<String, CareSchedule> scheduleMap = new HashMap<>();
        // No schedule for "unknown_schedule"

        List<FullCareHistoryAdapter.HistoryItem> items =
            FullCareHistoryAdapter.buildHistoryItems(completions, scheduleMap);

        // Should produce: 1 header + 1 item = 2 total
        assertEquals(2, items.size());

        // Verify completion exists with default care type
        assertEquals("c1", items.get(1).completion.id);
        assertEquals("unknown", items.get(1).careType);
    }

    // ==================== Status Computation Tests ====================

    @Test
    public void testOnTimeStatusWhenCompletedNearDue() {
        // Schedule with nextDue = March 1, frequencyDays = 7
        // Most recent completion (index 0) should be due at: March 1 - 7 days = Feb 22
        // If completed on Feb 22 or Feb 23 (within 1 day tolerance), status = "On time"

        CareSchedule schedule = createSchedule("s1", "water", 7);
        schedule.nextDue = getTimestamp(2026, Calendar.MARCH, 1);

        List<CareCompletion> completions = new ArrayList<>();
        // Completed on Feb 22 (exactly on estimated due date)
        completions.add(createCompletion("c1", "s1", getTimestamp(2026, Calendar.FEBRUARY, 22), "done"));

        Map<String, CareSchedule> scheduleMap = new HashMap<>();
        scheduleMap.put("s1", schedule);

        List<FullCareHistoryAdapter.HistoryItem> items =
            FullCareHistoryAdapter.buildHistoryItems(completions, scheduleMap);

        // Should produce: 1 header + 1 item = 2 total
        assertEquals(2, items.size());

        // Verify status is "On time"
        assertEquals("c1", items.get(1).completion.id);
        assertEquals("On time", items.get(1).statusLabel);
        assertEquals(R.color.health_good, items.get(1).statusColor);
    }

    @Test
    public void testLateStatusWhenCompletedWellAfterDue() {
        // Schedule with nextDue = March 1, frequencyDays = 7
        // Most recent completion (index 0) should be due at: March 1 - 7 days = Feb 22
        // If completed on Feb 25 (3 days after due), status = "Late"

        CareSchedule schedule = createSchedule("s1", "water", 7);
        schedule.nextDue = getTimestamp(2026, Calendar.MARCH, 1);

        List<CareCompletion> completions = new ArrayList<>();
        // Completed on Feb 25 (3 days after estimated due date of Feb 22)
        completions.add(createCompletion("c1", "s1", getTimestamp(2026, Calendar.FEBRUARY, 25), "done"));

        Map<String, CareSchedule> scheduleMap = new HashMap<>();
        scheduleMap.put("s1", schedule);

        List<FullCareHistoryAdapter.HistoryItem> items =
            FullCareHistoryAdapter.buildHistoryItems(completions, scheduleMap);

        // Should produce: 1 header + 1 item = 2 total
        assertEquals(2, items.size());

        // Verify status is "Late"
        assertEquals("c1", items.get(1).completion.id);
        assertEquals("Late", items.get(1).statusLabel);
        assertEquals(R.color.health_warning, items.get(1).statusColor);
    }
}
