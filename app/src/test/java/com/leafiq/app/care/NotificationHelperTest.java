package com.leafiq.app.care;

import static com.google.common.truth.Truth.assertThat;

import com.leafiq.app.data.entity.CareSchedule;
import com.leafiq.app.data.entity.Plant;

import org.junit.Test;

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
}
