package com.leafiq.app.data.db;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.leafiq.app.data.entity.CareSchedule;
import com.leafiq.app.data.entity.Plant;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class CareScheduleDaoTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase database;
    private PlantDao plantDao;
    private CareScheduleDao careScheduleDao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        plantDao = database.plantDao();
        careScheduleDao = database.careScheduleDao();

        // Insert plants for foreign key constraints
        insertPlant("plant-1", "Pothos");
        insertPlant("plant-2", "Fern");
    }

    @After
    public void tearDown() {
        database.close();
    }

    // ==================== Insert tests ====================

    @Test
    public void insertSchedule_canBeRetrievedById() {
        CareSchedule schedule = createSchedule("s1", "plant-1", "water", 7);
        careScheduleDao.insertSchedule(schedule);

        CareSchedule retrieved = careScheduleDao.getScheduleById("s1");
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.id).isEqualTo("s1");
        assertThat(retrieved.plantId).isEqualTo("plant-1");
        assertThat(retrieved.careType).isEqualTo("water");
        assertThat(retrieved.frequencyDays).isEqualTo(7);
    }

    @Test
    public void insertSchedule_preservesAllFields() {
        CareSchedule schedule = createSchedule("s1", "plant-1", "fertilize", 30);
        schedule.isCustom = true;
        schedule.isEnabled = false;
        schedule.snoozeCount = 3;
        schedule.notes = "Use NPK 10-10-10";
        schedule.nextDue = 5000000L;
        careScheduleDao.insertSchedule(schedule);

        CareSchedule retrieved = careScheduleDao.getScheduleById("s1");
        assertThat(retrieved.isCustom).isTrue();
        assertThat(retrieved.isEnabled).isFalse();
        assertThat(retrieved.snoozeCount).isEqualTo(3);
        assertThat(retrieved.notes).isEqualTo("Use NPK 10-10-10");
        assertThat(retrieved.nextDue).isEqualTo(5000000L);
    }

    @Test
    public void insertSchedule_replaceOnConflict() {
        CareSchedule schedule1 = createSchedule("s1", "plant-1", "water", 7);
        schedule1.notes = "Original";
        careScheduleDao.insertSchedule(schedule1);

        CareSchedule schedule2 = createSchedule("s1", "plant-1", "water", 3);
        schedule2.notes = "Updated";
        careScheduleDao.insertSchedule(schedule2);

        CareSchedule retrieved = careScheduleDao.getScheduleById("s1");
        assertThat(retrieved.frequencyDays).isEqualTo(3);
        assertThat(retrieved.notes).isEqualTo("Updated");
    }

    // ==================== getSchedulesByPlantIdSync tests ====================

    @Test
    public void getSchedulesByPlantIdSync_returnsSchedulesForPlant() {
        careScheduleDao.insertSchedule(createSchedule("s1", "plant-1", "water", 7));
        careScheduleDao.insertSchedule(createSchedule("s2", "plant-1", "fertilize", 30));
        careScheduleDao.insertSchedule(createSchedule("s3", "plant-2", "water", 5));

        List<CareSchedule> plant1Schedules = careScheduleDao.getSchedulesByPlantIdSync("plant-1");
        assertThat(plant1Schedules).hasSize(2);

        List<CareSchedule> plant2Schedules = careScheduleDao.getSchedulesByPlantIdSync("plant-2");
        assertThat(plant2Schedules).hasSize(1);
    }

    @Test
    public void getSchedulesByPlantIdSync_emptyForNonexistentPlant() {
        careScheduleDao.insertSchedule(createSchedule("s1", "plant-1", "water", 7));

        List<CareSchedule> schedules = careScheduleDao.getSchedulesByPlantIdSync("nonexistent");
        assertThat(schedules).isEmpty();
    }

    // ==================== getDueSchedules tests ====================

    @Test
    public void getDueSchedules_returnsOnlyDueAndEnabled() {
        CareSchedule due = createSchedule("s1", "plant-1", "water", 7);
        due.nextDue = 1000L;
        due.isEnabled = true;

        CareSchedule notDue = createSchedule("s2", "plant-1", "fertilize", 30);
        notDue.nextDue = 99999999L;
        notDue.isEnabled = true;

        CareSchedule dueButDisabled = createSchedule("s3", "plant-2", "water", 7);
        dueButDisabled.nextDue = 1000L;
        dueButDisabled.isEnabled = false;

        careScheduleDao.insertSchedule(due);
        careScheduleDao.insertSchedule(notDue);
        careScheduleDao.insertSchedule(dueButDisabled);

        List<CareSchedule> dueSchedules = careScheduleDao.getDueSchedules(5000L);
        assertThat(dueSchedules).hasSize(1);
        assertThat(dueSchedules.get(0).id).isEqualTo("s1");
    }

    @Test
    public void getDueSchedules_includesSchedulesExactlyAtTimestamp() {
        CareSchedule exactlyDue = createSchedule("s1", "plant-1", "water", 7);
        exactlyDue.nextDue = 5000L;
        exactlyDue.isEnabled = true;
        careScheduleDao.insertSchedule(exactlyDue);

        List<CareSchedule> dueSchedules = careScheduleDao.getDueSchedules(5000L);
        assertThat(dueSchedules).hasSize(1);
    }

    @Test
    public void getDueSchedules_emptyWhenNoneDue() {
        CareSchedule future = createSchedule("s1", "plant-1", "water", 7);
        future.nextDue = 99999999L;
        future.isEnabled = true;
        careScheduleDao.insertSchedule(future);

        List<CareSchedule> dueSchedules = careScheduleDao.getDueSchedules(5000L);
        assertThat(dueSchedules).isEmpty();
    }

    // ==================== getEnabledSchedulesForPlant tests ====================

    @Test
    public void getAllEnabledSchedules_returnsOnlyEnabled() throws Exception {
        CareSchedule enabled = createSchedule("s1", "plant-1", "water", 7);
        enabled.isEnabled = true;

        CareSchedule disabled = createSchedule("s2", "plant-1", "fertilize", 30);
        disabled.isEnabled = false;

        careScheduleDao.insertSchedule(enabled);
        careScheduleDao.insertSchedule(disabled);

        List<CareSchedule> allEnabled = careScheduleDao.getAllEnabledSchedules();
        assertThat(allEnabled).hasSize(1);
        assertThat(allEnabled.get(0).id).isEqualTo("s1");
    }

    // ==================== Update tests ====================

    @Test
    public void updateSchedule_persistsChanges() {
        CareSchedule schedule = createSchedule("s1", "plant-1", "water", 7);
        careScheduleDao.insertSchedule(schedule);

        schedule.frequencyDays = 3;
        schedule.isCustom = true;
        schedule.snoozeCount = 2;
        careScheduleDao.updateSchedule(schedule);

        CareSchedule updated = careScheduleDao.getScheduleById("s1");
        assertThat(updated.frequencyDays).isEqualTo(3);
        assertThat(updated.isCustom).isTrue();
        assertThat(updated.snoozeCount).isEqualTo(2);
    }

    // ==================== Delete tests ====================

    @Test
    public void deleteSchedule_removesFromDatabase() {
        CareSchedule schedule = createSchedule("s1", "plant-1", "water", 7);
        careScheduleDao.insertSchedule(schedule);

        careScheduleDao.deleteSchedule(schedule);

        CareSchedule deleted = careScheduleDao.getScheduleById("s1");
        assertThat(deleted).isNull();
    }

    @Test
    public void deleteSchedulesForPlant_removesAllForPlant() {
        careScheduleDao.insertSchedule(createSchedule("s1", "plant-1", "water", 7));
        careScheduleDao.insertSchedule(createSchedule("s2", "plant-1", "fertilize", 30));
        careScheduleDao.insertSchedule(createSchedule("s3", "plant-2", "water", 5));

        careScheduleDao.deleteSchedulesForPlant("plant-1");

        List<CareSchedule> plant1 = careScheduleDao.getSchedulesByPlantIdSync("plant-1");
        assertThat(plant1).isEmpty();

        List<CareSchedule> plant2 = careScheduleDao.getSchedulesByPlantIdSync("plant-2");
        assertThat(plant2).hasSize(1);
    }

    // ==================== Foreign key CASCADE tests ====================

    @Test
    public void deletePlant_cascadeDeletesSchedules() {
        careScheduleDao.insertSchedule(createSchedule("s1", "plant-1", "water", 7));
        careScheduleDao.insertSchedule(createSchedule("s2", "plant-1", "fertilize", 30));

        Plant plant = plantDao.getPlantByIdSync("plant-1");
        plantDao.deletePlant(plant);

        List<CareSchedule> schedules = careScheduleDao.getSchedulesByPlantIdSync("plant-1");
        assertThat(schedules).isEmpty();
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
}
