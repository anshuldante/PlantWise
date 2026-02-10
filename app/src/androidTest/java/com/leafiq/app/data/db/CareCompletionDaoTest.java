package com.leafiq.app.data.db;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.leafiq.app.data.entity.CareCompletion;
import com.leafiq.app.data.entity.CareSchedule;
import com.leafiq.app.data.entity.Plant;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class CareCompletionDaoTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase database;
    private PlantDao plantDao;
    private CareScheduleDao careScheduleDao;
    private CareCompletionDao careCompletionDao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        plantDao = database.plantDao();
        careScheduleDao = database.careScheduleDao();
        careCompletionDao = database.careCompletionDao();

        // Set up plant and schedule for FK constraints
        insertPlant("plant-1", "Pothos");
        insertSchedule("sched-1", "plant-1", "water", 7);
        insertSchedule("sched-2", "plant-1", "fertilize", 30);
    }

    @After
    public void tearDown() {
        database.close();
    }

    // ==================== Insert tests ====================

    @Test
    public void insertCompletion_canBeRetrieved() {
        CareCompletion completion = createCompletion("c1", "sched-1", 1000000L, "in_app");
        careCompletionDao.insertCompletion(completion);

        CareCompletion last = careCompletionDao.getLastCompletionForSchedule("sched-1");
        assertThat(last).isNotNull();
        assertThat(last.id).isEqualTo("c1");
        assertThat(last.scheduleId).isEqualTo("sched-1");
        assertThat(last.completedAt).isEqualTo(1000000L);
        assertThat(last.source).isEqualTo("in_app");
    }

    // ==================== getLastCompletionForSchedule tests ====================

    @Test
    public void getLastCompletion_returnsMostRecent() {
        careCompletionDao.insertCompletion(createCompletion("c1", "sched-1", 1000000L, "in_app"));
        careCompletionDao.insertCompletion(createCompletion("c2", "sched-1", 3000000L, "notification_action"));
        careCompletionDao.insertCompletion(createCompletion("c3", "sched-1", 2000000L, "in_app"));

        CareCompletion last = careCompletionDao.getLastCompletionForSchedule("sched-1");
        assertThat(last.id).isEqualTo("c2"); // Most recent by completedAt DESC
    }

    @Test
    public void getLastCompletion_returnsNullWhenNone() {
        CareCompletion last = careCompletionDao.getLastCompletionForSchedule("sched-1");
        assertThat(last).isNull();
    }

    @Test
    public void getLastCompletion_onlyReturnsForRequestedSchedule() {
        careCompletionDao.insertCompletion(createCompletion("c1", "sched-1", 1000000L, "in_app"));
        careCompletionDao.insertCompletion(createCompletion("c2", "sched-2", 2000000L, "in_app"));

        CareCompletion last = careCompletionDao.getLastCompletionForSchedule("sched-1");
        assertThat(last.id).isEqualTo("c1");
    }

    // ==================== getRecentCompletionsForPlant tests ====================

    @Test
    public void getRecentCompletionsForPlant_joinsWithSchedule() throws Exception {
        careCompletionDao.insertCompletion(createCompletion("c1", "sched-1", 1000000L, "in_app"));
        careCompletionDao.insertCompletion(createCompletion("c2", "sched-2", 2000000L, "notification_action"));

        List<CareCompletion> completions = LiveDataTestUtil.getValue(
                careCompletionDao.getRecentCompletionsForPlant("plant-1", 10));

        assertThat(completions).hasSize(2);
        // Should be ordered by completedAt DESC
        assertThat(completions.get(0).id).isEqualTo("c2");
        assertThat(completions.get(1).id).isEqualTo("c1");
    }

    @Test
    public void getRecentCompletionsForPlant_respectsLimit() throws Exception {
        careCompletionDao.insertCompletion(createCompletion("c1", "sched-1", 1000000L, "in_app"));
        careCompletionDao.insertCompletion(createCompletion("c2", "sched-1", 2000000L, "in_app"));
        careCompletionDao.insertCompletion(createCompletion("c3", "sched-1", 3000000L, "in_app"));

        List<CareCompletion> completions = LiveDataTestUtil.getValue(
                careCompletionDao.getRecentCompletionsForPlant("plant-1", 2));

        assertThat(completions).hasSize(2);
        // Most recent first
        assertThat(completions.get(0).id).isEqualTo("c3");
        assertThat(completions.get(1).id).isEqualTo("c2");
    }

    @Test
    public void getRecentCompletionsForPlant_emptyForNonexistentPlant() throws Exception {
        careCompletionDao.insertCompletion(createCompletion("c1", "sched-1", 1000000L, "in_app"));

        List<CareCompletion> completions = LiveDataTestUtil.getValue(
                careCompletionDao.getRecentCompletionsForPlant("nonexistent", 10));

        assertThat(completions).isEmpty();
    }

    // ==================== deleteCompletionsForSchedule tests ====================

    @Test
    public void deleteCompletionsForSchedule_removesAllForSchedule() {
        careCompletionDao.insertCompletion(createCompletion("c1", "sched-1", 1000000L, "in_app"));
        careCompletionDao.insertCompletion(createCompletion("c2", "sched-1", 2000000L, "in_app"));
        careCompletionDao.insertCompletion(createCompletion("c3", "sched-2", 3000000L, "in_app"));

        careCompletionDao.deleteCompletionsForSchedule("sched-1");

        CareCompletion last1 = careCompletionDao.getLastCompletionForSchedule("sched-1");
        assertThat(last1).isNull();

        // sched-2 completion should still exist
        CareCompletion last2 = careCompletionDao.getLastCompletionForSchedule("sched-2");
        assertThat(last2).isNotNull();
        assertThat(last2.id).isEqualTo("c3");
    }

    // ==================== CASCADE tests ====================

    @Test
    public void deleteSchedule_cascadeDeletesCompletions() {
        careCompletionDao.insertCompletion(createCompletion("c1", "sched-1", 1000000L, "in_app"));
        careCompletionDao.insertCompletion(createCompletion("c2", "sched-1", 2000000L, "in_app"));

        CareSchedule schedule = careScheduleDao.getScheduleById("sched-1");
        careScheduleDao.deleteSchedule(schedule);

        CareCompletion last = careCompletionDao.getLastCompletionForSchedule("sched-1");
        assertThat(last).isNull();
    }

    @Test
    public void deletePlant_cascadeDeletesScheduleAndCompletions() {
        careCompletionDao.insertCompletion(createCompletion("c1", "sched-1", 1000000L, "in_app"));

        Plant plant = plantDao.getPlantByIdSync("plant-1");
        plantDao.deletePlant(plant);

        // Schedule should be gone
        CareSchedule schedule = careScheduleDao.getScheduleById("sched-1");
        assertThat(schedule).isNull();

        // Completion should also be gone via cascade
        CareCompletion last = careCompletionDao.getLastCompletionForSchedule("sched-1");
        assertThat(last).isNull();
    }

    // ==================== Helpers ====================

    private void insertPlant(String id, String name) {
        Plant plant = new Plant();
        plant.id = id;
        plant.commonName = name;
        plant.createdAt = System.currentTimeMillis();
        plant.updatedAt = System.currentTimeMillis();
        plantDao.insertPlant(plant);
    }

    private void insertSchedule(String id, String plantId, String careType, int frequencyDays) {
        CareSchedule schedule = new CareSchedule();
        schedule.id = id;
        schedule.plantId = plantId;
        schedule.careType = careType;
        schedule.frequencyDays = frequencyDays;
        schedule.nextDue = System.currentTimeMillis();
        schedule.isCustom = false;
        schedule.isEnabled = true;
        schedule.snoozeCount = 0;
        schedule.notes = "";
        careScheduleDao.insertSchedule(schedule);
    }

    private CareCompletion createCompletion(String id, String scheduleId, long completedAt, String source) {
        CareCompletion completion = new CareCompletion();
        completion.id = id;
        completion.scheduleId = scheduleId;
        completion.completedAt = completedAt;
        completion.source = source;
        return completion;
    }
}
