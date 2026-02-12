package com.leafiq.app.ui.care;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import com.leafiq.app.data.entity.CareSchedule;
import com.leafiq.app.data.entity.Plant;

import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit tests for CareOverviewViewModel POJOs and logic.
 * Tests CareTaskItem, CareCompletionItem construction and snooze time calculations.
 */
public class CareOverviewViewModelTest {

    // ==================== CareTaskItem tests ====================

    @Test
    public void careTaskItem_holdsScheduleAndPlant() {
        CareSchedule schedule = createSchedule("s1", "plant-1", "water", 7);
        Plant plant = createPlant("plant-1", "Pothos", "My Plant");

        CareOverviewViewModel.CareTaskItem item = new CareOverviewViewModel.CareTaskItem(schedule, plant);

        assertThat(item.schedule).isEqualTo(schedule);
        assertThat(item.plant).isEqualTo(plant);
        assertThat(item.schedule.careType).isEqualTo("water");
        assertThat(item.plant.commonName).isEqualTo("Pothos");
    }

    // ==================== CareCompletionItem tests ====================

    @Test
    public void careCompletionItem_holdsAllFields() {
        CareOverviewViewModel.CareCompletionItem item = new CareOverviewViewModel.CareCompletionItem(
                "sched-1", "water", "Watered My Pothos", "2 days ago", "plant-1", 1000000L);

        assertThat(item.scheduleId).isEqualTo("sched-1");
        assertThat(item.careType).isEqualTo("water");
        assertThat(item.displayText).isEqualTo("Watered My Pothos");
        assertThat(item.relativeTime).isEqualTo("2 days ago");
        assertThat(item.plantId).isEqualTo("plant-1");
        assertThat(item.completedAt).isEqualTo(1000000L);
    }

    // ==================== Snooze calculation tests ====================

    @Test
    public void snoozeOption0_6hours_calculatesCorrectly() {
        long now = System.currentTimeMillis();
        long expected = now + (6 * 60 * 60 * 1000L);

        long result = calculateSnoozeNextDue(now, 0, null);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void snoozeOption1_tomorrow_calculatesCorrectly() {
        long now = System.currentTimeMillis();
        long expected = now + (24 * 60 * 60 * 1000L);

        long result = calculateSnoozeNextDue(now, 1, null);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void snoozeOption2_nextDueWindow_addsFrequency() {
        CareSchedule schedule = createSchedule("s1", "p1", "water", 7);
        schedule.nextDue = 5000000L;

        long expected = schedule.nextDue + (schedule.frequencyDays * 24L * 60 * 60 * 1000);
        long result = calculateSnoozeNextDue(System.currentTimeMillis(), 2, schedule);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void snooze_incrementsSnoozeCount() {
        CareSchedule schedule = createSchedule("s1", "p1", "water", 7);
        assertThat(schedule.snoozeCount).isEqualTo(0);

        schedule.snoozeCount++;
        assertThat(schedule.snoozeCount).isEqualTo(1);

        schedule.snoozeCount++;
        assertThat(schedule.snoozeCount).isEqualTo(2);
    }

    // ==================== Date filtering logic tests ====================

    @Test
    public void todayFilter_scheduleDueBeforeEndOfToday_isToday() {
        long now = System.currentTimeMillis();
        long endOfToday = getEndOfToday();

        CareSchedule dueNow = createSchedule("s1", "p1", "water", 7);
        dueNow.nextDue = now - 1000; // Due in the past (overdue)

        assertThat(dueNow.nextDue <= endOfToday).isTrue();
    }

    @Test
    public void todayFilter_scheduleDueTomorrow_isNotToday() {
        long endOfToday = getEndOfToday();
        long tomorrow = endOfToday + 1000;

        CareSchedule dueTomorrow = createSchedule("s1", "p1", "water", 7);
        dueTomorrow.nextDue = tomorrow;

        assertThat(dueTomorrow.nextDue <= endOfToday).isFalse();
    }

    @Test
    public void upcomingFilter_scheduleDueIn3Days_isUpcoming() {
        long endOfToday = getEndOfToday();
        long sevenDaysFromNow = endOfToday + (7 * 24 * 60 * 60 * 1000L);
        long threeDaysFromNow = endOfToday + (3 * 24 * 60 * 60 * 1000L);

        CareSchedule dueIn3Days = createSchedule("s1", "p1", "water", 7);
        dueIn3Days.nextDue = threeDaysFromNow;

        boolean isUpcoming = dueIn3Days.nextDue > endOfToday && dueIn3Days.nextDue <= sevenDaysFromNow;
        assertThat(isUpcoming).isTrue();
    }

    @Test
    public void upcomingFilter_scheduleDueIn10Days_isNotUpcoming() {
        long endOfToday = getEndOfToday();
        long sevenDaysFromNow = endOfToday + (7 * 24 * 60 * 60 * 1000L);
        long tenDaysFromNow = endOfToday + (10 * 24 * 60 * 60 * 1000L);

        CareSchedule dueIn10Days = createSchedule("s1", "p1", "water", 7);
        dueIn10Days.nextDue = tenDaysFromNow;

        boolean isUpcoming = dueIn10Days.nextDue > endOfToday && dueIn10Days.nextDue <= sevenDaysFromNow;
        assertThat(isUpcoming).isFalse();
    }

    // ==================== Past tense verb tests ====================

    @Test
    public void getPastTenseVerb_water_returnsWatered() throws Exception {
        assertThat(invokeGetPastTenseVerb("water")).isEqualTo("Watered");
    }

    @Test
    public void getPastTenseVerb_fertilize_returnsFertilized() throws Exception {
        assertThat(invokeGetPastTenseVerb("fertilize")).isEqualTo("Fertilized");
    }

    @Test
    public void getPastTenseVerb_repot_returnsRepotted() throws Exception {
        assertThat(invokeGetPastTenseVerb("repot")).isEqualTo("Repotted");
    }

    @Test
    public void getPastTenseVerb_prune_returnsPruned() throws Exception {
        assertThat(invokeGetPastTenseVerb("prune")).isEqualTo("Pruned");
    }

    @Test
    public void getPastTenseVerb_unknown_returnsDefault() throws Exception {
        assertThat(invokeGetPastTenseVerb("mist")).isEqualTo("Cared for");
    }

    // ==================== Helpers ====================

    /**
     * Simulates snooze calculation from CareOverviewViewModel.snooze().
     */
    private long calculateSnoozeNextDue(long now, int snoozeOption, CareSchedule schedule) {
        switch (snoozeOption) {
            case 0: return now + (6 * 60 * 60 * 1000L);
            case 1: return now + (24 * 60 * 60 * 1000L);
            case 2: return schedule.nextDue + (schedule.frequencyDays * 24L * 60 * 60 * 1000);
            default: return now;
        }
    }

    /**
     * Invokes the private getPastTenseVerb method via reflection.
     */
    private String invokeGetPastTenseVerb(String careType) throws Exception {
        java.lang.reflect.Method method = CareOverviewViewModel.class.getDeclaredMethod("getPastTenseVerb", String.class);
        method.setAccessible(true);

        // Create a mock ViewModel to call the method on
        // Note: getPastTenseVerb is an instance method, but we can create a minimal mock
        // Since the method doesn't use any instance state, we can use reflection to invoke it
        // We need to pass null for 'this' if it's actually private static, or create a minimal instance

        // After checking the implementation, getPastTenseVerb is private instance method
        // We'll invoke it on a null-like context using Mockito stub
        org.mockito.Mockito.mock(CareOverviewViewModel.class, org.mockito.Mockito.withSettings().stubOnly());

        // Actually, let's just invoke directly with null since it doesn't use instance fields
        // But we need an instance. Let's create a test double by invoking on a mocked instance.
        CareOverviewViewModel mockViewModel = org.mockito.Mockito.mock(CareOverviewViewModel.class, org.mockito.Mockito.withSettings().stubOnly());
        return (String) method.invoke(mockViewModel, careType);
    }

    private long getEndOfToday() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
        cal.set(java.util.Calendar.MINUTE, 59);
        cal.set(java.util.Calendar.SECOND, 59);
        cal.set(java.util.Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }

    private CareSchedule createSchedule(String id, String plantId, String careType, int frequencyDays) {
        CareSchedule schedule = new CareSchedule();
        schedule.id = id;
        schedule.plantId = plantId;
        schedule.careType = careType;
        schedule.frequencyDays = frequencyDays;
        schedule.nextDue = System.currentTimeMillis() + (frequencyDays * 24L * 60 * 60 * 1000);
        schedule.isCustom = false;
        schedule.isEnabled = true;
        schedule.snoozeCount = 0;
        schedule.notes = "";
        return schedule;
    }

    private Plant createPlant(String id, String commonName, String nickname) {
        Plant plant = new Plant();
        plant.id = id;
        plant.commonName = commonName;
        plant.nickname = nickname;
        plant.createdAt = System.currentTimeMillis();
        plant.updatedAt = System.currentTimeMillis();
        return plant;
    }
}
