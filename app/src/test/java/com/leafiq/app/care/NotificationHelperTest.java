package com.leafiq.app.care;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.service.notification.StatusBarNotification;

import com.leafiq.app.data.entity.CareSchedule;
import com.leafiq.app.data.entity.Plant;

import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;

/**
 * Unit tests for NotificationHelper utility methods.
 * Tests getCareEmoji, getCareVerb, getPlantDisplayName, and DueScheduleInfo.
 */
public class NotificationHelperTest {

    // ==================== getCareEmoji tests ====================

    @Test
    public void getCareEmoji_water_returnsDroplet() throws Exception {
        assertThat(invokeGetCareEmoji("water")).isEqualTo("\uD83D\uDCA7"); // 💧
    }

    @Test
    public void getCareEmoji_fertilize_returnsSeedling() throws Exception {
        assertThat(invokeGetCareEmoji("fertilize")).isEqualTo("\uD83C\uDF31"); // 🌱
    }

    @Test
    public void getCareEmoji_repot_returnsPottedPlant() throws Exception {
        assertThat(invokeGetCareEmoji("repot")).isEqualTo("\uD83E\uDEB4"); // 🪴
    }

    @Test
    public void getCareEmoji_unknown_returnsHerb() throws Exception {
        assertThat(invokeGetCareEmoji("prune")).isEqualTo("\uD83C\uDF3F"); // 🌿
    }

    // ==================== getCareVerb tests ====================

    @Test
    public void getCareVerb_water_returnsWater() throws Exception {
        assertThat(invokeGetCareVerb("water")).isEqualTo("Water");
    }

    @Test
    public void getCareVerb_fertilize_returnsFertilize() throws Exception {
        assertThat(invokeGetCareVerb("fertilize")).isEqualTo("Fertilize");
    }

    @Test
    public void getCareVerb_repot_returnsRepot() throws Exception {
        assertThat(invokeGetCareVerb("repot")).isEqualTo("Repot");
    }

    @Test
    public void getCareVerb_unknown_returnsCareFor() throws Exception {
        assertThat(invokeGetCareVerb("mist")).isEqualTo("Care for");
    }

    // ==================== getPlantDisplayName tests ====================

    @Test
    public void getPlantDisplayName_withNickname_returnsNickname() throws Exception {
        Plant plant = new Plant();
        plant.nickname = "My Fern";
        plant.commonName = "Boston Fern";

        assertThat(invokeGetPlantDisplayName(plant)).isEqualTo("My Fern");
    }

    @Test
    public void getPlantDisplayName_noNickname_returnsCommonName() throws Exception {
        Plant plant = new Plant();
        plant.nickname = null;
        plant.commonName = "Boston Fern";

        assertThat(invokeGetPlantDisplayName(plant)).isEqualTo("Boston Fern");
    }

    @Test
    public void getPlantDisplayName_emptyNickname_returnsCommonName() throws Exception {
        Plant plant = new Plant();
        plant.nickname = "";
        plant.commonName = "Pothos";

        assertThat(invokeGetPlantDisplayName(plant)).isEqualTo("Pothos");
    }

    @Test
    public void getPlantDisplayName_noNicknameNoCommonName_returnsDefault() throws Exception {
        Plant plant = new Plant();
        plant.nickname = null;
        plant.commonName = null;

        assertThat(invokeGetPlantDisplayName(plant)).isEqualTo("Your plant");
    }

    @Test
    public void getPlantDisplayName_emptyNicknameEmptyCommonName_returnsDefault() throws Exception {
        Plant plant = new Plant();
        plant.nickname = "";
        plant.commonName = "";

        assertThat(invokeGetPlantDisplayName(plant)).isEqualTo("Your plant");
    }

    // ==================== DueScheduleInfo tests ====================

    @Test
    public void dueScheduleInfo_holdsScheduleAndPlant() {
        CareSchedule schedule = new CareSchedule();
        schedule.id = "s1";
        schedule.careType = "water";

        Plant plant = new Plant();
        plant.id = "p1";
        plant.commonName = "Pothos";

        NotificationHelper.DueScheduleInfo info = new NotificationHelper.DueScheduleInfo(schedule, plant);

        assertThat(info.schedule).isEqualTo(schedule);
        assertThat(info.plant).isEqualTo(plant);
        assertThat(info.schedule.careType).isEqualTo("water");
        assertThat(info.plant.commonName).isEqualTo("Pothos");
    }

    // ==================== Constants tests ====================

    @Test
    public void channelId_isCorrect() {
        assertThat(NotificationHelper.CHANNEL_ID).isEqualTo("plant_care_reminders");
    }

    @Test
    public void channelName_isCorrect() {
        assertThat(NotificationHelper.CHANNEL_NAME).isEqualTo("Plant Care Reminders");
    }

    @Test
    public void summaryNotificationId_isCorrect() {
        assertThat(NotificationHelper.SUMMARY_NOTIFICATION_ID).isEqualTo(9999);
    }

    @Test
    public void groupKey_isCorrect() {
        assertThat(NotificationHelper.GROUP_KEY).isEqualTo("com.leafiq.app.CARE_REMINDERS");
    }

    // ==================== dismissNotification tests ====================

    @Test
    public void dismissNotification_cancelsChildNotification() {
        // Create mocks
        Context mockContext = mock(Context.class);
        NotificationManager mockNM = mock(NotificationManager.class);

        // Setup context to return NotificationManager
        when(mockContext.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(mockNM);

        // Mock getActiveNotifications to return empty (no children remaining)
        when(mockNM.getActiveNotifications()).thenReturn(new StatusBarNotification[0]);

        String scheduleId = "test-schedule-123";
        int expectedNotificationId = scheduleId.hashCode();

        // Call dismissNotification
        NotificationHelper.dismissNotification(mockContext, scheduleId);

        // Verify cancel was called with the correct ID
        verify(mockNM).cancel(expectedNotificationId);
    }

    // ==================== checkAndUpdateSummary tests ====================

    @Test
    public void checkAndUpdateSummary_noChildrenDismissesSummary() {
        // Create mocks
        Context mockContext = mock(Context.class);
        NotificationManager mockNM = mock(NotificationManager.class);

        // Mock getActiveNotifications returning only the summary (ID 9999)
        StatusBarNotification summaryNotification = createMockNotification(
                NotificationHelper.SUMMARY_NOTIFICATION_ID,
                NotificationHelper.GROUP_KEY
        );
        when(mockNM.getActiveNotifications()).thenReturn(new StatusBarNotification[]{summaryNotification});

        // Call checkAndUpdateSummary
        NotificationHelper.checkAndUpdateSummary(mockContext, mockNM);

        // Verify summary was cancelled
        verify(mockNM).cancel(NotificationHelper.SUMMARY_NOTIFICATION_ID);
    }

    @Test
    public void checkAndUpdateSummary_withChildrenDoesNotDismissSummary() {
        // Create mocks
        Context mockContext = mock(Context.class);
        NotificationManager mockNM = mock(NotificationManager.class);

        // Mock getActiveNotifications returning summary + 2 children
        StatusBarNotification summaryNotification = createMockNotification(
                NotificationHelper.SUMMARY_NOTIFICATION_ID,
                NotificationHelper.GROUP_KEY
        );
        StatusBarNotification child1 = createMockNotification(100, NotificationHelper.GROUP_KEY);
        StatusBarNotification child2 = createMockNotification(200, NotificationHelper.GROUP_KEY);

        when(mockNM.getActiveNotifications()).thenReturn(
                new StatusBarNotification[]{summaryNotification, child1, child2}
        );

        // Note: We can't test the actual notification building in unit tests (requires Android framework)
        // But we can verify that cancel() is NOT called when children remain
        // The actual notification building will be tested in instrumented tests or manually

        // Call checkAndUpdateSummary (will attempt to update notification, may throw due to Android mocking)
        try {
            NotificationHelper.checkAndUpdateSummary(mockContext, mockNM);
        } catch (Exception e) {
            // Expected in unit test environment when trying to build notification
            // The important thing is that cancel() was not called before the exception
        }

        // Verify summary was NOT cancelled (children remaining)
        verify(mockNM, never()).cancel(NotificationHelper.SUMMARY_NOTIFICATION_ID);
    }

    @Test
    public void checkAndUpdateSummary_emptyNotificationsDismissesSummary() {
        // Create mocks
        Context mockContext = mock(Context.class);
        NotificationManager mockNM = mock(NotificationManager.class);

        // Mock getActiveNotifications returning empty array
        when(mockNM.getActiveNotifications()).thenReturn(new StatusBarNotification[0]);

        // Call checkAndUpdateSummary
        NotificationHelper.checkAndUpdateSummary(mockContext, mockNM);

        // Verify summary was cancelled
        verify(mockNM).cancel(NotificationHelper.SUMMARY_NOTIFICATION_ID);
    }

    // ==================== dismissAllNotifications tests ====================

    @Test
    public void dismissAllNotifications_cancelsAllGroupedNotifications() {
        // Create mocks
        Context mockContext = mock(Context.class);
        NotificationManager mockNM = mock(NotificationManager.class);

        // Setup context to return NotificationManager
        when(mockContext.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(mockNM);

        // Mock getActiveNotifications with 3 notifications in group
        StatusBarNotification summary = createMockNotification(9999, NotificationHelper.GROUP_KEY);
        StatusBarNotification child1 = createMockNotification(100, NotificationHelper.GROUP_KEY);
        StatusBarNotification child2 = createMockNotification(200, NotificationHelper.GROUP_KEY);

        when(mockNM.getActiveNotifications()).thenReturn(
                new StatusBarNotification[]{summary, child1, child2}
        );

        // Call dismissAllNotifications
        NotificationHelper.dismissAllNotifications(mockContext);

        // Verify cancel was called for all 3 notifications
        verify(mockNM).cancel(9999);
        verify(mockNM).cancel(100);
        verify(mockNM).cancel(200);
    }

    // ==================== Reflection helpers ====================

    private String invokeGetCareEmoji(String careType) throws Exception {
        Method method = NotificationHelper.class.getDeclaredMethod("getCareEmoji", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, careType);
    }

    private String invokeGetCareVerb(String careType) throws Exception {
        Method method = NotificationHelper.class.getDeclaredMethod("getCareVerb", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, careType);
    }

    private String invokeGetPlantDisplayName(Plant plant) throws Exception {
        Method method = NotificationHelper.class.getDeclaredMethod("getPlantDisplayName", Plant.class);
        method.setAccessible(true);
        return (String) method.invoke(null, plant);
    }

    /**
     * Creates a mock StatusBarNotification with the given ID and group.
     */
    private StatusBarNotification createMockNotification(int id, String group) {
        StatusBarNotification sbn = mock(StatusBarNotification.class);
        Notification notification = mock(Notification.class);

        when(sbn.getId()).thenReturn(id);
        when(sbn.getNotification()).thenReturn(notification);
        when(notification.getGroup()).thenReturn(group);

        return sbn;
    }
}
