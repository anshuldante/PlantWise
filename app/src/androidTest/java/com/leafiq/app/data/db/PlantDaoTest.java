package com.leafiq.app.data.db;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.leafiq.app.data.entity.Plant;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class PlantDaoTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase database;
    private PlantDao plantDao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
            .allowMainThreadQueries()
            .build();
        plantDao = database.plantDao();
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void insertPlant_andGetById_returnsPlant() throws InterruptedException {
        Plant plant = createTestPlant("1", "Monstera", "Monstera deliciosa");
        plantDao.insertPlant(plant);

        Plant retrieved = plantDao.getPlantByIdSync("1");

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.commonName).isEqualTo("Monstera");
        assertThat(retrieved.scientificName).isEqualTo("Monstera deliciosa");
    }

    @Test
    public void insertPlant_withSameId_replacesExisting() {
        Plant plant1 = createTestPlant("1", "Old Name", "Old scientific");
        plantDao.insertPlant(plant1);

        Plant plant2 = createTestPlant("1", "New Name", "New scientific");
        plantDao.insertPlant(plant2);

        Plant retrieved = plantDao.getPlantByIdSync("1");
        assertThat(retrieved.commonName).isEqualTo("New Name");
    }

    @Test
    public void getAllPlants_returnsAllPlants_orderedByUpdatedAt() throws InterruptedException {
        Plant plant1 = createTestPlant("1", "First", "First scientific");
        plant1.updatedAt = 1000L;
        plantDao.insertPlant(plant1);

        Plant plant2 = createTestPlant("2", "Second", "Second scientific");
        plant2.updatedAt = 3000L;
        plantDao.insertPlant(plant2);

        Plant plant3 = createTestPlant("3", "Third", "Third scientific");
        plant3.updatedAt = 2000L;
        plantDao.insertPlant(plant3);

        List<Plant> plants = LiveDataTestUtil.getValue(plantDao.getAllPlants());

        assertThat(plants).hasSize(3);
        // Should be ordered by updatedAt DESC
        assertThat(plants.get(0).commonName).isEqualTo("Second");
        assertThat(plants.get(1).commonName).isEqualTo("Third");
        assertThat(plants.get(2).commonName).isEqualTo("First");
    }

    @Test
    public void deletePlant_removesPlant() {
        Plant plant = createTestPlant("1", "To Delete", "Scientific");
        plantDao.insertPlant(plant);

        plantDao.deletePlant(plant);

        Plant retrieved = plantDao.getPlantByIdSync("1");
        assertThat(retrieved).isNull();
    }

    @Test
    public void updatePlant_updatesFields() {
        Plant plant = createTestPlant("1", "Original", "Scientific");
        plant.latestHealthScore = 5;
        plantDao.insertPlant(plant);

        plant.commonName = "Updated";
        plant.latestHealthScore = 8;
        plantDao.updatePlant(plant);

        Plant retrieved = plantDao.getPlantByIdSync("1");
        assertThat(retrieved.commonName).isEqualTo("Updated");
        assertThat(retrieved.latestHealthScore).isEqualTo(8);
    }

    @Test
    public void getPlantByIdSync_withNonexistentId_returnsNull() {
        Plant result = plantDao.getPlantByIdSync("nonexistent");
        assertThat(result).isNull();
    }

    @Test
    public void insertMultiplePlants_getAllReturnsAll() throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            Plant plant = createTestPlant(
                String.valueOf(i),
                "Plant " + i,
                "Scientific " + i
            );
            plant.updatedAt = System.currentTimeMillis() + i;
            plantDao.insertPlant(plant);
        }

        List<Plant> plants = LiveDataTestUtil.getValue(plantDao.getAllPlants());
        assertThat(plants).hasSize(5);
    }

    // ==================== Phase 5: getDistinctLocations tests ====================

    @Test
    public void getDistinctLocations_returnsUniqueLocations() {
        Plant plant1 = createTestPlant("loc-1", "Plant A", "Sci A");
        plant1.location = "Living Room";
        plantDao.insertPlant(plant1);

        Plant plant2 = createTestPlant("loc-2", "Plant B", "Sci B");
        plant2.location = "Bedroom";
        plantDao.insertPlant(plant2);

        Plant plant3 = createTestPlant("loc-3", "Plant C", "Sci C");
        plant3.location = "Living Room"; // duplicate
        plantDao.insertPlant(plant3);

        List<String> locations = plantDao.getDistinctLocations();

        assertThat(locations).hasSize(2);
        assertThat(locations).contains("Living Room");
        assertThat(locations).contains("Bedroom");
    }

    @Test
    public void getDistinctLocations_excludesNullAndEmpty() {
        Plant plantWithLoc = createTestPlant("ne-1", "Plant A", "Sci A");
        plantWithLoc.location = "Kitchen";
        plantDao.insertPlant(plantWithLoc);

        Plant plantNull = createTestPlant("ne-2", "Plant B", "Sci B");
        plantNull.location = null;
        plantDao.insertPlant(plantNull);

        Plant plantEmpty = createTestPlant("ne-3", "Plant C", "Sci C");
        plantEmpty.location = "";
        plantDao.insertPlant(plantEmpty);

        List<String> locations = plantDao.getDistinctLocations();

        assertThat(locations).hasSize(1);
        assertThat(locations).contains("Kitchen");
    }

    @Test
    public void getDistinctLocations_returnsSortedAlphabetically() {
        Plant plant1 = createTestPlant("sort-1", "Plant A", "Sci A");
        plant1.location = "Patio";
        plantDao.insertPlant(plant1);

        Plant plant2 = createTestPlant("sort-2", "Plant B", "Sci B");
        plant2.location = "Bathroom";
        plantDao.insertPlant(plant2);

        Plant plant3 = createTestPlant("sort-3", "Plant C", "Sci C");
        plant3.location = "Kitchen";
        plantDao.insertPlant(plant3);

        List<String> locations = plantDao.getDistinctLocations();

        assertThat(locations).hasSize(3);
        assertThat(locations.get(0)).isEqualTo("Bathroom");
        assertThat(locations.get(1)).isEqualTo("Kitchen");
        assertThat(locations.get(2)).isEqualTo("Patio");
    }

    @Test
    public void getDistinctLocations_withNoLocations_returnsEmptyList() {
        Plant plant = createTestPlant("empty-1", "Plant A", "Sci A");
        plant.location = null;
        plantDao.insertPlant(plant);

        List<String> locations = plantDao.getDistinctLocations();

        assertThat(locations).isEmpty();
    }

    private Plant createTestPlant(String id, String commonName, String scientificName) {
        Plant plant = new Plant();
        plant.id = id;
        plant.commonName = commonName;
        plant.scientificName = scientificName;
        plant.latestHealthScore = 7;
        plant.createdAt = System.currentTimeMillis();
        plant.updatedAt = System.currentTimeMillis();
        return plant;
    }
}
