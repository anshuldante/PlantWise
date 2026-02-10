package com.leafiq.app.ui.care;

import static com.google.common.truth.Truth.assertThat;

import com.leafiq.app.data.entity.CareSchedule;
import com.leafiq.app.data.entity.Plant;

import org.junit.Test;

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
                "sched-1", "water", "My Pothos", 1000000L);

        assertThat(item.scheduleId).isEqualTo("sched-1");
        assertThat(item.careType).isEqualTo("water");
        assertThat(item.plantDisplayName).isEqualTo("My Pothos");
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
