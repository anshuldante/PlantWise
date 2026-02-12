package com.leafiq.app.ui.analysis;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import com.leafiq.app.data.entity.CareItem;
import com.leafiq.app.data.model.PlantAnalysisResult;

import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Unit tests for AnalysisViewModel private helper methods.
 * Uses reflection to access private methods since they contain significant
 * business logic worth testing independently (parseFrequencyDays, buildCareItems).
 */
public class AnalysisViewModelTest {

    // ==================== parseFrequencyDays tests ====================

    // Null/empty input -> 14 (ultimate fallback changed from 7)
    @Test
    public void parseFrequencyDays_null_returnsDefault14() throws Exception {
        assertThat(invokeParseFrequencyDays(null)).isEqualTo(14);
    }

    @Test
    public void parseFrequencyDays_emptyString_returnsDefault14() throws Exception {
        assertThat(invokeParseFrequencyDays("")).isEqualTo(14);
    }

    // Keyword fallbacks (longest first to fix biweekly bug)
    @Test
    public void parseFrequencyDays_daily_returns1() throws Exception {
        assertThat(invokeParseFrequencyDays("daily")).isEqualTo(1);
    }

    @Test
    public void parseFrequencyDays_weekly_returns7() throws Exception {
        assertThat(invokeParseFrequencyDays("weekly")).isEqualTo(7);
    }

    @Test
    public void parseFrequencyDays_biweekly_returns14_BUG15_FIXED() throws Exception {
        // BUG-15 FIX: biweekly must be checked BEFORE weekly to avoid substring collision
        assertThat(invokeParseFrequencyDays("biweekly")).isEqualTo(14);
    }

    @Test
    public void parseFrequencyDays_biWeeklyHyphen_returns14() throws Exception {
        assertThat(invokeParseFrequencyDays("bi-weekly")).isEqualTo(14);
    }

    @Test
    public void parseFrequencyDays_fortnightly_returns14() throws Exception {
        assertThat(invokeParseFrequencyDays("fortnightly")).isEqualTo(14);
    }

    @Test
    public void parseFrequencyDays_monthly_returns30() throws Exception {
        assertThat(invokeParseFrequencyDays("monthly")).isEqualTo(30);
    }

    @Test
    public void parseFrequencyDays_bimonthly_returns60() throws Exception {
        assertThat(invokeParseFrequencyDays("bimonthly")).isEqualTo(60);
    }

    @Test
    public void parseFrequencyDays_biMonthlyHyphen_returns60() throws Exception {
        assertThat(invokeParseFrequencyDays("bi-monthly")).isEqualTo(60);
    }

    @Test
    public void parseFrequencyDays_yearly_returns90_capped() throws Exception {
        // Yearly capped at 90 days per user decision
        assertThat(invokeParseFrequencyDays("yearly")).isEqualTo(90);
    }

    @Test
    public void parseFrequencyDays_annually_returns90_capped() throws Exception {
        assertThat(invokeParseFrequencyDays("annually")).isEqualTo(90);
    }

    // Numeric extraction with units
    @Test
    public void parseFrequencyDays_every10days_returns10() throws Exception {
        assertThat(invokeParseFrequencyDays("every 10 days")).isEqualTo(10);
    }

    @Test
    public void parseFrequencyDays_every2weeks_returns14() throws Exception {
        assertThat(invokeParseFrequencyDays("every 2 weeks")).isEqualTo(14);
    }

    @Test
    public void parseFrequencyDays_every3months_returns90_capped() throws Exception {
        // 3 months = 90 days, which is already at the cap
        assertThat(invokeParseFrequencyDays("every 3 months")).isEqualTo(90);
    }

    @Test
    public void parseFrequencyDays_every5months_returns90_capped() throws Exception {
        // 5 months = 150 days, but capped at 90
        assertThat(invokeParseFrequencyDays("every 5 months")).isEqualTo(90);
    }

    // Range numeric extraction (higher bound)
    @Test
    public void parseFrequencyDays_every23weeks_returns21_higherBound() throws Exception {
        // "every 2-3 weeks" -> 3 * 7 = 21 (higher bound)
        assertThat(invokeParseFrequencyDays("every 2-3 weeks")).isEqualTo(21);
    }

    @Test
    public void parseFrequencyDays_every710days_returns10_higherBound() throws Exception {
        // "every 7-10 days" -> 10 (higher bound)
        assertThat(invokeParseFrequencyDays("every 7-10 days")).isEqualTo(10);
    }

    // Special phrases
    @Test
    public void parseFrequencyDays_twiceAWeek_returns4() throws Exception {
        // Twice a week = every 3.5 days, round to 4
        assertThat(invokeParseFrequencyDays("twice a week")).isEqualTo(4);
    }

    @Test
    public void parseFrequencyDays_twiceAMonth_returns15() throws Exception {
        // Twice a month = every 15 days
        assertThat(invokeParseFrequencyDays("twice a month")).isEqualTo(15);
    }

    // Condition-based detection
    @Test
    public void parseFrequencyDays_asNeeded_returns14_conditionBased() throws Exception {
        assertThat(invokeParseFrequencyDays("as needed")).isEqualTo(14);
    }

    @Test
    public void parseFrequencyDays_whenSoilIsDry_returns14_conditionBased() throws Exception {
        assertThat(invokeParseFrequencyDays("when soil is dry")).isEqualTo(14);
    }

    @Test
    public void parseFrequencyDays_waterWhenTopInchDry_returns14_conditionBased() throws Exception {
        assertThat(invokeParseFrequencyDays("Water when top inch of soil is dry")).isEqualTo(14);
    }

    // Seasonal qualifier (first frequency found)
    @Test
    public void parseFrequencyDays_monthlyInSummerWeeklyInWinter_returns30_firstFrequency() throws Exception {
        // Keyword scan hits "monthly" first -> 30
        assertThat(invokeParseFrequencyDays("monthly in summer, weekly in winter")).isEqualTo(30);
    }

    // Bounds enforcement
    @Test
    public void parseFrequencyDays_every1day_returns1_minFloor() throws Exception {
        assertThat(invokeParseFrequencyDays("every 1 day")).isEqualTo(1);
    }

    @Test
    public void parseFrequencyDays_every200days_returns90_maxCap() throws Exception {
        // 200 days capped at 90
        assertThat(invokeParseFrequencyDays("every 200 days")).isEqualTo(90);
    }

    @Test
    public void parseFrequencyDays_bareNumber14_assumesDays() throws Exception {
        assertThat(invokeParseFrequencyDays("14")).isEqualTo(14);
    }

    @Test
    public void parseFrequencyDays_bareNumber0_returns1_minFloor() throws Exception {
        // Math.max(1, 0) ensures at least 1 day
        assertThat(invokeParseFrequencyDays("0")).isEqualTo(1);
    }

    // Case insensitivity
    @Test
    public void parseFrequencyDays_caseInsensitive() throws Exception {
        assertThat(invokeParseFrequencyDays("DAILY")).isEqualTo(1);
        assertThat(invokeParseFrequencyDays("BiWeekly")).isEqualTo(14);
        assertThat(invokeParseFrequencyDays("MONTHLY")).isEqualTo(30);
    }

    // Gibberish fallback
    @Test
    public void parseFrequencyDays_gibberish_returnsDefault14() throws Exception {
        assertThat(invokeParseFrequencyDays("gibberish text")).isEqualTo(14);
    }

    // ==================== buildCareItems tests ====================

    @Test
    public void buildCareItems_fullCarePlan_createsAllItems() throws Exception {
        PlantAnalysisResult.CarePlan carePlan = new PlantAnalysisResult.CarePlan();

        // Watering
        carePlan.watering = new PlantAnalysisResult.CarePlan.Watering();
        carePlan.watering.frequency = "weekly";
        carePlan.watering.amount = "1 cup";
        carePlan.watering.notes = "Let soil dry between waterings";

        // Fertilizer
        carePlan.fertilizer = new PlantAnalysisResult.CarePlan.Fertilizer();
        carePlan.fertilizer.frequency = "monthly";
        carePlan.fertilizer.type = "NPK 10-10-10";

        // Pruning
        carePlan.pruning = new PlantAnalysisResult.CarePlan.Pruning();
        carePlan.pruning.needed = true;
        carePlan.pruning.instructions = "Remove dead leaves";

        // Repotting
        carePlan.repotting = new PlantAnalysisResult.CarePlan.Repotting();
        carePlan.repotting.needed = true;
        carePlan.repotting.signs = "Roots visible";

        List<CareItem> items = invokeBuildCareItems("plant-1", carePlan, 1000000L);

        assertThat(items).hasSize(4);

        // Verify water item
        CareItem water = findByType(items, "water");
        assertThat(water).isNotNull();
        assertThat(water.frequencyDays).isEqualTo(7);
        assertThat(water.notes).isEqualTo("1 cup - Let soil dry between waterings");
        assertThat(water.plantId).isEqualTo("plant-1");

        // Verify fertilize item
        CareItem fertilize = findByType(items, "fertilize");
        assertThat(fertilize).isNotNull();
        assertThat(fertilize.frequencyDays).isEqualTo(30);
        assertThat(fertilize.notes).isEqualTo("NPK 10-10-10");

        // Verify prune item
        CareItem prune = findByType(items, "prune");
        assertThat(prune).isNotNull();
        assertThat(prune.frequencyDays).isEqualTo(30); // Default for pruning

        // Verify repot item
        CareItem repot = findByType(items, "repot");
        assertThat(repot).isNotNull();
        assertThat(repot.frequencyDays).isEqualTo(365); // Default for repotting
        assertThat(repot.notes).isEqualTo("Roots visible");
    }

    @Test
    public void buildCareItems_wateringOnly_createsSingleItem() throws Exception {
        PlantAnalysisResult.CarePlan carePlan = new PlantAnalysisResult.CarePlan();
        carePlan.watering = new PlantAnalysisResult.CarePlan.Watering();
        carePlan.watering.frequency = "daily";
        carePlan.watering.amount = "Half cup";

        List<CareItem> items = invokeBuildCareItems("plant-1", carePlan, 1000000L);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).type).isEqualTo("water");
        assertThat(items.get(0).frequencyDays).isEqualTo(1);
    }

    @Test
    public void buildCareItems_pruningNotNeeded_skipsPruning() throws Exception {
        PlantAnalysisResult.CarePlan carePlan = new PlantAnalysisResult.CarePlan();
        carePlan.pruning = new PlantAnalysisResult.CarePlan.Pruning();
        carePlan.pruning.needed = false;

        List<CareItem> items = invokeBuildCareItems("plant-1", carePlan, 1000000L);

        assertThat(items).isEmpty();
    }

    @Test
    public void buildCareItems_repottingNotNeeded_skipsRepotting() throws Exception {
        PlantAnalysisResult.CarePlan carePlan = new PlantAnalysisResult.CarePlan();
        carePlan.repotting = new PlantAnalysisResult.CarePlan.Repotting();
        carePlan.repotting.needed = false;

        List<CareItem> items = invokeBuildCareItems("plant-1", carePlan, 1000000L);

        assertThat(items).isEmpty();
    }

    @Test
    public void buildCareItems_nullWateringFrequency_skipsWatering() throws Exception {
        PlantAnalysisResult.CarePlan carePlan = new PlantAnalysisResult.CarePlan();
        carePlan.watering = new PlantAnalysisResult.CarePlan.Watering();
        carePlan.watering.frequency = null;
        carePlan.watering.amount = "Some amount";

        List<CareItem> items = invokeBuildCareItems("plant-1", carePlan, 1000000L);

        assertThat(items).isEmpty();
    }

    @Test
    public void buildCareItems_setsCorrectTimestamps() throws Exception {
        long now = 1000000L;
        PlantAnalysisResult.CarePlan carePlan = new PlantAnalysisResult.CarePlan();
        carePlan.watering = new PlantAnalysisResult.CarePlan.Watering();
        carePlan.watering.frequency = "weekly";
        carePlan.watering.amount = "1 cup";

        List<CareItem> items = invokeBuildCareItems("plant-1", carePlan, now);

        CareItem water = items.get(0);
        assertThat(water.lastDone).isEqualTo(now);
        assertThat(water.nextDue).isEqualTo(now + (7 * 86400000L));
    }

    @Test
    public void buildCareItems_wateringWithNullNotes_onlyUsesAmount() throws Exception {
        PlantAnalysisResult.CarePlan carePlan = new PlantAnalysisResult.CarePlan();
        carePlan.watering = new PlantAnalysisResult.CarePlan.Watering();
        carePlan.watering.frequency = "weekly";
        carePlan.watering.amount = "1 cup";
        carePlan.watering.notes = null;

        List<CareItem> items = invokeBuildCareItems("plant-1", carePlan, 1000000L);

        assertThat(items.get(0).notes).isEqualTo("1 cup");
    }

    @Test
    public void buildCareItems_emptyCarePlan_returnsEmptyList() throws Exception {
        PlantAnalysisResult.CarePlan carePlan = new PlantAnalysisResult.CarePlan();

        List<CareItem> items = invokeBuildCareItems("plant-1", carePlan, 1000000L);

        assertThat(items).isEmpty();
    }

    @Test
    public void buildCareItems_eachItemHasUniqueId() throws Exception {
        PlantAnalysisResult.CarePlan carePlan = new PlantAnalysisResult.CarePlan();
        carePlan.watering = new PlantAnalysisResult.CarePlan.Watering();
        carePlan.watering.frequency = "weekly";
        carePlan.watering.amount = "1 cup";
        carePlan.fertilizer = new PlantAnalysisResult.CarePlan.Fertilizer();
        carePlan.fertilizer.frequency = "monthly";
        carePlan.fertilizer.type = "NPK";

        List<CareItem> items = invokeBuildCareItems("plant-1", carePlan, 1000000L);

        assertThat(items).hasSize(2);
        assertThat(items.get(0).id).isNotEmpty();
        assertThat(items.get(1).id).isNotEmpty();
        assertThat(items.get(0).id).isNotEqualTo(items.get(1).id);
    }

    // ==================== Reflection helpers ====================

    /**
     * Creates a mock AnalysisViewModel that can execute private methods.
     * Mockito uses Objenesis internally to bypass constructors.
     */
    private AnalysisViewModel createInstance() {
        return mock(AnalysisViewModel.class, Mockito.withSettings().stubOnly());
    }

    /**
     * Invokes the private parseFrequencyDays method via reflection.
     */
    private int invokeParseFrequencyDays(String frequency) throws Exception {
        Method method = AnalysisViewModel.class.getDeclaredMethod("parseFrequencyDays", String.class);
        method.setAccessible(true);
        AnalysisViewModel instance = createInstance();
        try {
            return (int) method.invoke(instance, frequency);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    /**
     * Invokes the private buildCareItems method via reflection.
     */
    @SuppressWarnings("unchecked")
    private List<CareItem> invokeBuildCareItems(String plantId, PlantAnalysisResult.CarePlan carePlan, long now) throws Exception {
        Method method = AnalysisViewModel.class.getDeclaredMethod("buildCareItems", String.class, PlantAnalysisResult.CarePlan.class, long.class);
        method.setAccessible(true);
        AnalysisViewModel instance = createInstance();
        try {
            return (List<CareItem>) method.invoke(instance, plantId, carePlan, now);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    private CareItem findByType(List<CareItem> items, String type) {
        for (CareItem item : items) {
            if (type.equals(item.type)) {
                return item;
            }
        }
        return null;
    }

    // ==================== New field tests (Plans 03 and 05) ====================

    /**
     * Tests for quickDiagnosis and qualityOverridden fields added in plans 12-03 and 12-05.
     * These verify the public API exists and is callable.
     * State preservation is tested via integration tests since stubOnly() mocks don't preserve fields.
     */

    @Test
    public void quickDiagnosis_apiExists() throws Exception {
        // Verify setQuickDiagnosis and isQuickDiagnosis methods exist via reflection
        AnalysisViewModel.class.getDeclaredMethod("setQuickDiagnosis", boolean.class);
        AnalysisViewModel.class.getDeclaredMethod("isQuickDiagnosis");

        // Methods exist and are callable
        AnalysisViewModel instance = createInstance();
        instance.setQuickDiagnosis(true);
        instance.setQuickDiagnosis(false);
        instance.isQuickDiagnosis();
    }

    @Test
    public void qualityOverridden_apiExists() throws Exception {
        // Verify setQualityOverridden method exists via reflection
        AnalysisViewModel.class.getDeclaredMethod("setQualityOverridden", boolean.class);

        // Method exists and is callable
        AnalysisViewModel instance = createInstance();
        instance.setQualityOverridden(true);
        instance.setQualityOverridden(false);
    }
}
