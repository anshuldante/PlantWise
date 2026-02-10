package com.leafiq.app.care;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AlarmManager;
import android.content.Context;

import com.leafiq.app.data.entity.CareCompletion;
import com.leafiq.app.data.entity.CareItem;
import com.leafiq.app.data.entity.CareSchedule;
import com.leafiq.app.data.repository.PlantRepository;
import com.leafiq.app.util.KeystoreHelper;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CareScheduleManagerTest {

    private PlantRepository mockRepository;
    private KeystoreHelper mockKeystoreHelper;
    private Context mockContext;
    private CareScheduleManager manager;

    @Before
    public void setUp() {
        mockRepository = mock(PlantRepository.class);
        mockKeystoreHelper = mock(KeystoreHelper.class);
        mockContext = mock(Context.class);

        // Mock context.getApplicationContext() returns itself
        when(mockContext.getApplicationContext()).thenReturn(mockContext);

        // Mock AlarmManager
        AlarmManager mockAlarmManager = mock(AlarmManager.class);
        when(mockContext.getSystemService(Context.ALARM_SERVICE)).thenReturn(mockAlarmManager);

        // Default: reminders not paused, preferred time 8:00 AM
        when(mockKeystoreHelper.areRemindersPaused()).thenReturn(false);
        when(mockKeystoreHelper.getPreferredReminderTime()).thenReturn(new int[]{8, 0});

        // Use spy to stub scheduleNextAlarm/cancelAlarm which use PendingIntent (unavailable in unit tests)
        manager = spy(new CareScheduleManager(mockContext, mockRepository, mockKeystoreHelper));
        doNothing().when(manager).scheduleNextAlarm();
        doNothing().when(manager).cancelAlarm();
    }

    // ==================== createSchedulesFromCareItems tests ====================

    @Test
    public void createSchedules_withNewWaterItem_createsNewSchedule() {
        String plantId = "plant-1";
        CareItem waterItem = createCareItem("water", 7, "1 cup");

        when(mockRepository.getSchedulesByPlantIdSync(plantId)).thenReturn(new ArrayList<>());

        List<CareSchedule> needsPrompt = manager.createSchedulesFromCareItems(plantId, Arrays.asList(waterItem));

        assertThat(needsPrompt).isEmpty();
        // Verify insertSchedule was called
        ArgumentCaptor<CareSchedule> captor = ArgumentCaptor.forClass(CareSchedule.class);
        verify(mockRepository).insertSchedule(captor.capture(), any());
        CareSchedule created = captor.getValue();
        assertThat(created.plantId).isEqualTo(plantId);
        assertThat(created.careType).isEqualTo("water");
        assertThat(created.frequencyDays).isEqualTo(7);
        assertThat(created.isCustom).isFalse();
        assertThat(created.isEnabled).isTrue();
        assertThat(created.snoozeCount).isEqualTo(0);
        assertThat(created.notes).isEqualTo("1 cup");
    }

    @Test
    public void createSchedules_withMultipleValidTypes_createsAllSchedules() {
        String plantId = "plant-1";
        CareItem waterItem = createCareItem("water", 7, "1 cup");
        CareItem fertilizeItem = createCareItem("fertilize", 30, "NPK");
        CareItem repotItem = createCareItem("repot", 365, "Spring");

        when(mockRepository.getSchedulesByPlantIdSync(plantId)).thenReturn(new ArrayList<>());

        List<CareSchedule> needsPrompt = manager.createSchedulesFromCareItems(
                plantId, Arrays.asList(waterItem, fertilizeItem, repotItem));

        assertThat(needsPrompt).isEmpty();
        // Verify 3 inserts
        verify(mockRepository, org.mockito.Mockito.times(3)).insertSchedule(any(), any());
    }

    @Test
    public void createSchedules_withPruneType_skipsIt() {
        String plantId = "plant-1";
        CareItem pruneItem = createCareItem("prune", 30, "Trim dead leaves");

        when(mockRepository.getSchedulesByPlantIdSync(plantId)).thenReturn(new ArrayList<>());

        List<CareSchedule> needsPrompt = manager.createSchedulesFromCareItems(
                plantId, Arrays.asList(pruneItem));

        assertThat(needsPrompt).isEmpty();
        verify(mockRepository, never()).insertSchedule(any(), any());
        verify(mockRepository, never()).updateSchedule(any(), any());
    }

    @Test
    public void createSchedules_withUnknownType_skipsIt() {
        String plantId = "plant-1";
        CareItem unknownItem = createCareItem("mist", 1, "Daily misting");

        when(mockRepository.getSchedulesByPlantIdSync(plantId)).thenReturn(new ArrayList<>());

        List<CareSchedule> needsPrompt = manager.createSchedulesFromCareItems(
                plantId, Arrays.asList(unknownItem));

        assertThat(needsPrompt).isEmpty();
        verify(mockRepository, never()).insertSchedule(any(), any());
    }

    @Test
    public void createSchedules_existingAiDerived_updatesSchedule() {
        String plantId = "plant-1";
        CareItem waterItem = createCareItem("water", 5, "New amount");

        CareSchedule existing = createExistingSchedule("s1", plantId, "water", 7, false);
        when(mockRepository.getSchedulesByPlantIdSync(plantId)).thenReturn(Arrays.asList(existing));
        when(mockRepository.getLastCompletionForScheduleSync("s1")).thenReturn(null);

        List<CareSchedule> needsPrompt = manager.createSchedulesFromCareItems(
                plantId, Arrays.asList(waterItem));

        assertThat(needsPrompt).isEmpty();
        // Verify update was called (not insert)
        verify(mockRepository, never()).insertSchedule(any(), any());
        ArgumentCaptor<CareSchedule> captor = ArgumentCaptor.forClass(CareSchedule.class);
        verify(mockRepository).updateSchedule(captor.capture(), any());
        CareSchedule updated = captor.getValue();
        assertThat(updated.frequencyDays).isEqualTo(5);
        assertThat(updated.notes).isEqualTo("New amount");
    }

    @Test
    public void createSchedules_existingAiDerived_withCompletion_calculatesNextDueFromCompletion() {
        String plantId = "plant-1";
        CareItem waterItem = createCareItem("water", 5, "notes");

        CareSchedule existing = createExistingSchedule("s1", plantId, "water", 7, false);
        when(mockRepository.getSchedulesByPlantIdSync(plantId)).thenReturn(Arrays.asList(existing));

        CareCompletion completion = new CareCompletion();
        completion.id = "c1";
        completion.completedAt = 1000000L;
        when(mockRepository.getLastCompletionForScheduleSync("s1")).thenReturn(completion);

        manager.createSchedulesFromCareItems(plantId, Arrays.asList(waterItem));

        ArgumentCaptor<CareSchedule> captor = ArgumentCaptor.forClass(CareSchedule.class);
        verify(mockRepository).updateSchedule(captor.capture(), any());
        CareSchedule updated = captor.getValue();
        // nextDue = completedAt + (frequencyDays * MILLIS_PER_DAY)
        long expectedNextDue = 1000000L + (5 * 24 * 60 * 60 * 1000L);
        assertThat(updated.nextDue).isEqualTo(expectedNextDue);
    }

    @Test
    public void createSchedules_existingCustom_sameFrequency_noPrompt() {
        String plantId = "plant-1";
        CareItem waterItem = createCareItem("water", 7, "Same freq");

        CareSchedule existing = createExistingSchedule("s1", plantId, "water", 7, true);
        when(mockRepository.getSchedulesByPlantIdSync(plantId)).thenReturn(Arrays.asList(existing));

        List<CareSchedule> needsPrompt = manager.createSchedulesFromCareItems(
                plantId, Arrays.asList(waterItem));

        assertThat(needsPrompt).isEmpty();
        verify(mockRepository, never()).insertSchedule(any(), any());
        verify(mockRepository, never()).updateSchedule(any(), any());
    }

    @Test
    public void createSchedules_existingCustom_differentFrequency_flagsForPrompt() {
        String plantId = "plant-1";
        CareItem waterItem = createCareItem("water", 5, "AI notes");

        CareSchedule existing = createExistingSchedule("s1", plantId, "water", 7, true);
        existing.notes = "Original notes";
        when(mockRepository.getSchedulesByPlantIdSync(plantId)).thenReturn(Arrays.asList(existing));

        List<CareSchedule> needsPrompt = manager.createSchedulesFromCareItems(
                plantId, Arrays.asList(waterItem));

        assertThat(needsPrompt).hasSize(1);
        CareSchedule prompted = needsPrompt.get(0);
        assertThat(prompted.notes).isEqualTo("AI_RECOMMENDED:5|AI notes");
        // Should NOT have called update or insert
        verify(mockRepository, never()).insertSchedule(any(), any());
        verify(mockRepository, never()).updateSchedule(any(), any());
    }

    @Test
    public void createSchedules_existingCustom_differentFrequency_nullNotes_handledGracefully() {
        String plantId = "plant-1";
        CareItem waterItem = createCareItem("water", 5, null);

        CareSchedule existing = createExistingSchedule("s1", plantId, "water", 7, true);
        existing.notes = "Original";
        when(mockRepository.getSchedulesByPlantIdSync(plantId)).thenReturn(Arrays.asList(existing));

        List<CareSchedule> needsPrompt = manager.createSchedulesFromCareItems(
                plantId, Arrays.asList(waterItem));

        assertThat(needsPrompt).hasSize(1);
        assertThat(needsPrompt.get(0).notes).isEqualTo("AI_RECOMMENDED:5|");
    }

    @Test
    public void createSchedules_mixedTypes_onlyProcessesWaterFertilizeRepot() {
        String plantId = "plant-1";
        List<CareItem> items = Arrays.asList(
                createCareItem("water", 7, "water notes"),
                createCareItem("prune", 30, "prune notes"),
                createCareItem("fertilize", 14, "fert notes"),
                createCareItem("mist", 1, "mist notes"),
                createCareItem("repot", 365, "repot notes")
        );

        when(mockRepository.getSchedulesByPlantIdSync(plantId)).thenReturn(new ArrayList<>());

        manager.createSchedulesFromCareItems(plantId, items);

        // Should insert 3 schedules (water, fertilize, repot) and skip 2 (prune, mist)
        verify(mockRepository, org.mockito.Mockito.times(3)).insertSchedule(any(), any());
    }

    @Test
    public void createSchedules_emptyList_doesNothing() {
        String plantId = "plant-1";
        when(mockRepository.getSchedulesByPlantIdSync(plantId)).thenReturn(new ArrayList<>());

        List<CareSchedule> needsPrompt = manager.createSchedulesFromCareItems(
                plantId, new ArrayList<>());

        assertThat(needsPrompt).isEmpty();
        verify(mockRepository, never()).insertSchedule(any(), any());
        verify(mockRepository, never()).updateSchedule(any(), any());
    }

    // ==================== toggleRemindersForPlant tests ====================

    @Test
    public void toggleReminders_disable_updatesAllSchedules() {
        String plantId = "plant-1";
        CareSchedule s1 = createExistingSchedule("s1", plantId, "water", 7, false);
        CareSchedule s2 = createExistingSchedule("s2", plantId, "fertilize", 30, false);
        when(mockRepository.getSchedulesByPlantIdSync(plantId)).thenReturn(Arrays.asList(s1, s2));

        manager.toggleRemindersForPlant(plantId, false);

        verify(mockRepository, org.mockito.Mockito.times(2)).updateSchedule(any(), any());
        assertThat(s1.isEnabled).isFalse();
        assertThat(s2.isEnabled).isFalse();
    }

    @Test
    public void toggleReminders_enable_recalculatesNextDue() {
        String plantId = "plant-1";
        CareSchedule s1 = createExistingSchedule("s1", plantId, "water", 7, false);
        s1.isEnabled = false;
        when(mockRepository.getSchedulesByPlantIdSync(plantId)).thenReturn(Arrays.asList(s1));
        when(mockRepository.getLastCompletionForScheduleSync("s1")).thenReturn(null);

        manager.toggleRemindersForPlant(plantId, true);

        assertThat(s1.isEnabled).isTrue();
        // nextDue should be set to approximately now + 7 days
        long sevenDaysMs = 7 * 24 * 60 * 60 * 1000L;
        assertThat(s1.nextDue).isGreaterThan(System.currentTimeMillis() + sevenDaysMs - 5000);
        verify(mockRepository).updateSchedule(any(), any());
    }

    @Test
    public void toggleReminders_enable_withCompletion_calculatesFromCompletion() {
        String plantId = "plant-1";
        CareSchedule s1 = createExistingSchedule("s1", plantId, "water", 7, false);
        s1.isEnabled = false;
        when(mockRepository.getSchedulesByPlantIdSync(plantId)).thenReturn(Arrays.asList(s1));

        CareCompletion completion = new CareCompletion();
        completion.id = "c1";
        completion.completedAt = 2000000L;
        when(mockRepository.getLastCompletionForScheduleSync("s1")).thenReturn(completion);

        manager.toggleRemindersForPlant(plantId, true);

        long expectedNextDue = 2000000L + (7 * 24 * 60 * 60 * 1000L);
        assertThat(s1.nextDue).isEqualTo(expectedNextDue);
    }

    @Test
    public void toggleReminders_emptySchedules_doesNotCrash() {
        String plantId = "plant-1";
        when(mockRepository.getSchedulesByPlantIdSync(plantId)).thenReturn(new ArrayList<>());

        manager.toggleRemindersForPlant(plantId, true);

        verify(mockRepository, never()).updateSchedule(any(), any());
    }

    // ==================== updateScheduleFrequency tests ====================

    @Test
    public void updateFrequency_marksAsCustom() {
        CareSchedule schedule = createExistingSchedule("s1", "p1", "water", 7, false);
        when(mockRepository.getScheduleByIdSync("s1")).thenReturn(schedule);
        when(mockRepository.getLastCompletionForScheduleSync("s1")).thenReturn(null);

        manager.updateScheduleFrequency("s1", 3);

        assertThat(schedule.frequencyDays).isEqualTo(3);
        assertThat(schedule.isCustom).isTrue();
        verify(mockRepository).updateSchedule(any(), any());
    }

    @Test
    public void updateFrequency_recalculatesNextDueFromCompletion() {
        CareSchedule schedule = createExistingSchedule("s1", "p1", "water", 7, false);
        when(mockRepository.getScheduleByIdSync("s1")).thenReturn(schedule);

        CareCompletion completion = new CareCompletion();
        completion.id = "c1";
        completion.completedAt = 5000000L;
        when(mockRepository.getLastCompletionForScheduleSync("s1")).thenReturn(completion);

        manager.updateScheduleFrequency("s1", 3);

        long expectedNextDue = 5000000L + (3 * 24 * 60 * 60 * 1000L);
        assertThat(schedule.nextDue).isEqualTo(expectedNextDue);
    }

    @Test
    public void updateFrequency_scheduleNotFound_doesNothing() {
        when(mockRepository.getScheduleByIdSync("nonexistent")).thenReturn(null);

        manager.updateScheduleFrequency("nonexistent", 3);

        verify(mockRepository, never()).updateSchedule(any(), any());
    }

    // ==================== alarm scheduling interaction tests ====================

    @Test
    public void createSchedules_callsScheduleNextAlarm() {
        String plantId = "plant-1";
        when(mockRepository.getSchedulesByPlantIdSync(plantId)).thenReturn(new ArrayList<>());

        manager.createSchedulesFromCareItems(plantId, new ArrayList<>());

        // Verify scheduleNextAlarm was called at end of createSchedulesFromCareItems
        verify(manager).scheduleNextAlarm();
    }

    @Test
    public void toggleReminders_callsScheduleNextAlarm() {
        String plantId = "plant-1";
        when(mockRepository.getSchedulesByPlantIdSync(plantId)).thenReturn(new ArrayList<>());

        manager.toggleRemindersForPlant(plantId, true);

        verify(manager).scheduleNextAlarm();
    }

    @Test
    public void updateFrequency_callsScheduleNextAlarm() {
        CareSchedule schedule = createExistingSchedule("s1", "p1", "water", 7, false);
        when(mockRepository.getScheduleByIdSync("s1")).thenReturn(schedule);
        when(mockRepository.getLastCompletionForScheduleSync("s1")).thenReturn(null);

        manager.updateScheduleFrequency("s1", 3);

        verify(manager).scheduleNextAlarm();
    }

    @Test
    public void rescheduleAllAlarms_whenPaused_doesNotCallScheduleNextAlarm() {
        when(mockKeystoreHelper.areRemindersPaused()).thenReturn(true);

        // Need a fresh spy without doNothing stub for this specific test
        CareScheduleManager freshManager = spy(new CareScheduleManager(mockContext, mockRepository, mockKeystoreHelper));
        doNothing().when(freshManager).scheduleNextAlarm();

        freshManager.rescheduleAllAlarms();

        verify(freshManager, never()).scheduleNextAlarm();
    }

    // ==================== Helper methods ====================

    private CareItem createCareItem(String type, int frequencyDays, String notes) {
        CareItem item = new CareItem();
        item.id = "ci-" + type;
        item.type = type;
        item.frequencyDays = frequencyDays;
        item.notes = notes;
        return item;
    }

    private CareSchedule createExistingSchedule(String id, String plantId, String careType,
                                                  int frequencyDays, boolean isCustom) {
        CareSchedule schedule = new CareSchedule();
        schedule.id = id;
        schedule.plantId = plantId;
        schedule.careType = careType;
        schedule.frequencyDays = frequencyDays;
        schedule.isCustom = isCustom;
        schedule.isEnabled = true;
        schedule.snoozeCount = 0;
        schedule.notes = "";
        schedule.nextDue = System.currentTimeMillis();
        return schedule;
    }
}
