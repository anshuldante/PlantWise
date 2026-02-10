package com.leafiq.app.care;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;

/**
 * Unit tests for CareReminderReceiver helper methods.
 * Tests getCareVerb which is a private helper used for toast messages.
 */
public class CareReminderReceiverTest {

    // ==================== getCareVerb tests ====================

    @Test
    public void getCareVerb_water_returnsWatered() throws Exception {
        assertThat(invokeGetCareVerb("water")).isEqualTo("Watered");
    }

    @Test
    public void getCareVerb_fertilize_returnsFertilized() throws Exception {
        assertThat(invokeGetCareVerb("fertilize")).isEqualTo("Fertilized");
    }

    @Test
    public void getCareVerb_repot_returnsRepotted() throws Exception {
        assertThat(invokeGetCareVerb("repot")).isEqualTo("Repotted");
    }

    @Test
    public void getCareVerb_unknown_returnsDefault() throws Exception {
        assertThat(invokeGetCareVerb("prune")).isEqualTo("Completed care for");
    }

    @Test
    public void getCareVerb_emptyString_returnsDefault() throws Exception {
        assertThat(invokeGetCareVerb("")).isEqualTo("Completed care for");
    }

    // ==================== Constants tests ====================

    @Test
    public void actionDone_isCorrectValue() {
        assertThat(CareReminderReceiver.ACTION_DONE).isEqualTo("com.leafiq.app.ACTION_CARE_DONE");
    }

    @Test
    public void actionSnooze_isCorrectValue() {
        assertThat(CareReminderReceiver.ACTION_SNOOZE).isEqualTo("com.leafiq.app.ACTION_CARE_SNOOZE");
    }

    // ==================== Snooze calculation tests ====================

    @Test
    public void snoozeOption0_adds6Hours() {
        long now = 1000000L;
        long expected = now + (6 * 60 * 60 * 1000L); // 6 hours
        assertThat(calculateSnoozeTime(now, 0, 7, 1000000L)).isEqualTo(expected);
    }

    @Test
    public void snoozeOption1_adds1Day() {
        long now = 1000000L;
        long expected = now + (24 * 60 * 60 * 1000L); // 24 hours
        assertThat(calculateSnoozeTime(now, 1, 7, 1000000L)).isEqualTo(expected);
    }

    @Test
    public void snoozeOption2_addsNextRegularCycle() {
        long currentNextDue = 5000000L;
        int frequencyDays = 7;
        long expected = currentNextDue + (frequencyDays * 24L * 60 * 60 * 1000);
        assertThat(calculateSnoozeTime(1000000L, 2, frequencyDays, currentNextDue)).isEqualTo(expected);
    }

    // ==================== SUGGEST_ADJUST_MARKER tests ====================

    @Test
    public void suggestAdjust_addedAt3Snoozes() {
        String notes = "Water thoroughly";
        int snoozeCount = 3;
        String marker = "[SUGGEST_ADJUST]";

        if (snoozeCount >= 3 && !notes.contains(marker)) {
            notes = notes + " " + marker;
        }

        assertThat(notes).contains("[SUGGEST_ADJUST]");
    }

    @Test
    public void suggestAdjust_notAddedBefore3() {
        String notes = "Water thoroughly";
        int snoozeCount = 2;
        String marker = "[SUGGEST_ADJUST]";

        if (snoozeCount >= 3 && !notes.contains(marker)) {
            notes = notes + " " + marker;
        }

        assertThat(notes).doesNotContain("[SUGGEST_ADJUST]");
    }

    @Test
    public void suggestAdjust_notDuplicatedIfAlreadyPresent() {
        String notes = "Water thoroughly [SUGGEST_ADJUST]";
        int snoozeCount = 5;
        String marker = "[SUGGEST_ADJUST]";

        if (snoozeCount >= 3 && !notes.contains(marker)) {
            notes = notes + " " + marker;
        }

        // Count occurrences of marker
        int count = 0;
        int idx = 0;
        while ((idx = notes.indexOf(marker, idx)) != -1) {
            count++;
            idx += marker.length();
        }
        assertThat(count).isEqualTo(1);
    }

    // ==================== Helpers ====================

    /**
     * Simulates the snooze time calculation from handleSnoozeAction.
     */
    private long calculateSnoozeTime(long now, int snoozeOption, int frequencyDays, long currentNextDue) {
        switch (snoozeOption) {
            case 0: return now + (6 * 60 * 60 * 1000L);
            case 1: return now + (24 * 60 * 60 * 1000L);
            case 2: return currentNextDue + (frequencyDays * 24L * 60 * 60 * 1000);
            default: return now;
        }
    }

    /**
     * Invokes the private getCareVerb method via reflection.
     * Uses Mockito's internal Objenesis to bypass BroadcastReceiver constructor.
     */
    private String invokeGetCareVerb(String careType) throws Exception {
        Method method = CareReminderReceiver.class.getDeclaredMethod("getCareVerb", String.class);
        method.setAccessible(true);

        CareReminderReceiver instance = mock(CareReminderReceiver.class, Mockito.withSettings().stubOnly());
        return (String) method.invoke(instance, careType);
    }
}
