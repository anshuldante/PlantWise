package com.leafiq.app.data.db;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.leafiq.app.data.entity.Analysis;
import com.leafiq.app.data.entity.CareItem;
import com.leafiq.app.data.entity.Plant;
import com.leafiq.app.data.model.AnalysisWithPlant;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class AnalysisDaoTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase database;
    private PlantDao plantDao;
    private AnalysisDao analysisDao;
    private CareItemDao careItemDao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
            .allowMainThreadQueries()
            .build();
        plantDao = database.plantDao();
        analysisDao = database.analysisDao();
        careItemDao = database.careItemDao();

        // Insert a plant for foreign key constraint
        Plant plant = new Plant();
        plant.id = "plant-1";
        plant.commonName = "Test Plant";
        plant.createdAt = System.currentTimeMillis();
        plant.updatedAt = System.currentTimeMillis();
        plantDao.insertPlant(plant);
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void insertAnalysis_andGetById_returnsAnalysis() {
        Analysis analysis = createTestAnalysis("1", "plant-1", 8, "Healthy plant");
        analysisDao.insertAnalysis(analysis);

        Analysis retrieved = analysisDao.getAnalysisById("1");

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.healthScore).isEqualTo(8);
        assertThat(retrieved.summary).isEqualTo("Healthy plant");
    }

    @Test
    public void getAnalysesForPlant_returnsAnalyses_orderedByCreatedAtDesc() throws InterruptedException {
        Analysis analysis1 = createTestAnalysis("1", "plant-1", 6, "First");
        analysis1.createdAt = 1000L;
        analysisDao.insertAnalysis(analysis1);

        Analysis analysis2 = createTestAnalysis("2", "plant-1", 7, "Second");
        analysis2.createdAt = 3000L;
        analysisDao.insertAnalysis(analysis2);

        Analysis analysis3 = createTestAnalysis("3", "plant-1", 8, "Third");
        analysis3.createdAt = 2000L;
        analysisDao.insertAnalysis(analysis3);

        List<Analysis> analyses = LiveDataTestUtil.getValue(
            analysisDao.getAnalysesForPlant("plant-1"));

        assertThat(analyses).hasSize(3);
        // Should be ordered by createdAt DESC (newest first)
        assertThat(analyses.get(0).summary).isEqualTo("Second");
        assertThat(analyses.get(1).summary).isEqualTo("Third");
        assertThat(analyses.get(2).summary).isEqualTo("First");
    }

    @Test
    public void getRecentAnalysesSync_returnsLast5() {
        // Insert 7 analyses
        for (int i = 0; i < 7; i++) {
            Analysis analysis = createTestAnalysis(
                String.valueOf(i),
                "plant-1",
                5 + i % 3,
                "Analysis " + i
            );
            analysis.createdAt = 1000L + i * 100;
            analysisDao.insertAnalysis(analysis);
        }

        List<Analysis> recent = analysisDao.getRecentAnalysesSync("plant-1");

        assertThat(recent).hasSize(5);
        // Should be the most recent 5
        assertThat(recent.get(0).summary).isEqualTo("Analysis 6");
    }

    @Test
    public void getAnalysesForPlant_withDifferentPlant_returnsEmpty() throws InterruptedException {
        Analysis analysis = createTestAnalysis("1", "plant-1", 8, "Test");
        analysisDao.insertAnalysis(analysis);

        List<Analysis> analyses = LiveDataTestUtil.getValue(
            analysisDao.getAnalysesForPlant("other-plant"));

        assertThat(analyses).isEmpty();
    }

    @Test
    public void deletePlant_cascadeDeletesAnalyses() throws InterruptedException {
        Analysis analysis = createTestAnalysis("1", "plant-1", 8, "Will be deleted");
        analysisDao.insertAnalysis(analysis);

        // Verify analysis exists
        Analysis before = analysisDao.getAnalysisById("1");
        assertThat(before).isNotNull();

        // Delete the plant
        Plant plant = plantDao.getPlantByIdSync("plant-1");
        plantDao.deletePlant(plant);

        // Analysis should be cascade deleted
        Analysis after = analysisDao.getAnalysisById("1");
        assertThat(after).isNull();
    }

    @Test
    public void multipleAnalysesForPlant_allRetrieved() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            Analysis analysis = createTestAnalysis(
                "analysis-" + i,
                "plant-1",
                7,
                "Analysis " + i
            );
            analysis.createdAt = System.currentTimeMillis() + i;
            analysisDao.insertAnalysis(analysis);
        }

        List<Analysis> analyses = LiveDataTestUtil.getValue(
            analysisDao.getAnalysesForPlant("plant-1"));

        assertThat(analyses).hasSize(10);
    }

    @Test
    public void insertAnalysis_withRawResponse_persistsRawResponse() {
        Analysis analysis = createTestAnalysis("raw-1", "plant-1", 8, "With raw response");
        analysis.rawResponse = "{\"choices\":[{\"message\":{\"content\":\"test\"}}]}";
        analysisDao.insertAnalysis(analysis);

        Analysis retrieved = analysisDao.getAnalysisById("raw-1");

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.rawResponse).isEqualTo("{\"choices\":[{\"message\":{\"content\":\"test\"}}]}");
    }

    // ==================== Phase 5 tests ====================

    @Test
    public void updateAnalysis_updatesFields() {
        Analysis analysis = createTestAnalysis("upd-1", "plant-1", 6, "Original summary");
        analysisDao.insertAnalysis(analysis);

        analysis.healthScore = 9;
        analysis.summary = "Updated summary";
        analysisDao.updateAnalysis(analysis);

        Analysis retrieved = analysisDao.getAnalysisById("upd-1");
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.healthScore).isEqualTo(9);
        assertThat(retrieved.summary).isEqualTo("Updated summary");
    }

    @Test
    public void deleteAnalysisById_removesAnalysis() {
        Analysis analysis = createTestAnalysis("del-1", "plant-1", 7, "To delete");
        analysisDao.insertAnalysis(analysis);

        // Verify it exists
        assertThat(analysisDao.getAnalysisById("del-1")).isNotNull();

        analysisDao.deleteAnalysisById("del-1");

        assertThat(analysisDao.getAnalysisById("del-1")).isNull();
    }

    @Test
    public void getPhotoPathsForPlantSync_returnsPhotoPaths() {
        Analysis analysis1 = createTestAnalysis("pp-1", "plant-1", 7, "With photo");
        analysis1.photoPath = "/photos/photo1.jpg";
        analysisDao.insertAnalysis(analysis1);

        Analysis analysis2 = createTestAnalysis("pp-2", "plant-1", 8, "With photo 2");
        analysis2.photoPath = "/photos/photo2.jpg";
        analysisDao.insertAnalysis(analysis2);

        List<String> paths = analysisDao.getPhotoPathsForPlantSync("plant-1");

        assertThat(paths).hasSize(2);
        assertThat(paths).contains("/photos/photo1.jpg");
        assertThat(paths).contains("/photos/photo2.jpg");
    }

    @Test
    public void getPhotoPathsForPlantSync_excludesNullPaths() {
        Analysis withPath = createTestAnalysis("ep-1", "plant-1", 7, "Has path");
        withPath.photoPath = "/photos/exists.jpg";
        analysisDao.insertAnalysis(withPath);

        Analysis withoutPath = createTestAnalysis("ep-2", "plant-1", 6, "No path");
        withoutPath.photoPath = null;
        analysisDao.insertAnalysis(withoutPath);

        List<String> paths = analysisDao.getPhotoPathsForPlantSync("plant-1");

        assertThat(paths).hasSize(1);
        assertThat(paths).contains("/photos/exists.jpg");
    }

    // ==================== Phase 6: JOIN query tests ====================

    @Test
    public void getAllAnalysesWithPlant_returnsAnalysesWithPlantData() throws InterruptedException {
        // Insert a second plant
        Plant plant2 = new Plant();
        plant2.id = "plant-2";
        plant2.commonName = "Snake Plant";
        plant2.scientificName = "Sansevieria";
        plant2.nickname = "Snakey";
        plant2.latestHealthScore = 9;
        plant2.createdAt = System.currentTimeMillis();
        plant2.updatedAt = System.currentTimeMillis();
        plantDao.insertPlant(plant2);

        // Update plant-1 with more data
        Plant plant1 = plantDao.getPlantByIdSync("plant-1");
        plant1.nickname = "Testy";
        plant1.scientificName = "Testus plantus";
        plant1.thumbnailPath = "/thumb/test.jpg";
        plant1.latestHealthScore = 7;
        plantDao.updatePlant(plant1);

        Analysis a1 = createTestAnalysis("a1", "plant-1", 7, "First");
        a1.createdAt = 2000L;
        analysisDao.insertAnalysis(a1);

        Analysis a2 = createTestAnalysis("a2", "plant-2", 9, "Second");
        a2.createdAt = 3000L;
        analysisDao.insertAnalysis(a2);

        List<AnalysisWithPlant> results = LiveDataTestUtil.getValue(
                analysisDao.getAllAnalysesWithPlant());

        assertThat(results).hasSize(2);
        // Ordered by created_at DESC (newest first)
        assertThat(results.get(0).analysis.id).isEqualTo("a2");
        assertThat(results.get(0).plantCommonName).isEqualTo("Snake Plant");
        assertThat(results.get(0).plantNickname).isEqualTo("Snakey");
        assertThat(results.get(0).plantLatestHealthScore).isEqualTo(9);

        assertThat(results.get(1).analysis.id).isEqualTo("a1");
        assertThat(results.get(1).plantCommonName).isEqualTo("Test Plant");
        assertThat(results.get(1).plantNickname).isEqualTo("Testy");
        assertThat(results.get(1).plantThumbnailPath).isEqualTo("/thumb/test.jpg");
        assertThat(results.get(1).plantLatestHealthScore).isEqualTo(7);
    }

    @Test
    public void getAllAnalysesWithPlant_withNoAnalyses_returnsEmpty() throws InterruptedException {
        List<AnalysisWithPlant> results = LiveDataTestUtil.getValue(
                analysisDao.getAllAnalysesWithPlant());

        assertThat(results).isEmpty();
    }

    @Test
    public void getAnalysesWithPlantForPlant_returnsOnlyTargetPlant() throws InterruptedException {
        // Insert a second plant
        Plant plant2 = new Plant();
        plant2.id = "plant-2";
        plant2.commonName = "Cactus";
        plant2.createdAt = System.currentTimeMillis();
        plant2.updatedAt = System.currentTimeMillis();
        plantDao.insertPlant(plant2);

        Analysis a1 = createTestAnalysis("a1", "plant-1", 7, "Plant 1 analysis");
        a1.createdAt = 1000L;
        analysisDao.insertAnalysis(a1);

        Analysis a2 = createTestAnalysis("a2", "plant-2", 5, "Plant 2 analysis");
        a2.createdAt = 2000L;
        analysisDao.insertAnalysis(a2);

        Analysis a3 = createTestAnalysis("a3", "plant-1", 8, "Plant 1 second");
        a3.createdAt = 3000L;
        analysisDao.insertAnalysis(a3);

        List<AnalysisWithPlant> results = LiveDataTestUtil.getValue(
                analysisDao.getAnalysesWithPlantForPlant("plant-1"));

        assertThat(results).hasSize(2);
        // Ordered by created_at DESC
        assertThat(results.get(0).analysis.id).isEqualTo("a3");
        assertThat(results.get(1).analysis.id).isEqualTo("a1");
        // Should all have plant-1 data
        assertThat(results.get(0).plantCommonName).isEqualTo("Test Plant");
        assertThat(results.get(1).plantCommonName).isEqualTo("Test Plant");
    }

    @Test
    public void getAnalysesWithPlantForPlant_nonexistentPlant_returnsEmpty() throws InterruptedException {
        Analysis a1 = createTestAnalysis("a1", "plant-1", 7, "Test");
        analysisDao.insertAnalysis(a1);

        List<AnalysisWithPlant> results = LiveDataTestUtil.getValue(
                analysisDao.getAnalysesWithPlantForPlant("nonexistent"));

        assertThat(results).isEmpty();
    }

    @Test
    public void getAllAnalysesWithPlant_includesScientificName() throws InterruptedException {
        Plant plant1 = plantDao.getPlantByIdSync("plant-1");
        plant1.scientificName = "Monstera deliciosa";
        plantDao.updatePlant(plant1);

        Analysis a1 = createTestAnalysis("a1", "plant-1", 8, "Test");
        analysisDao.insertAnalysis(a1);

        List<AnalysisWithPlant> results = LiveDataTestUtil.getValue(
                analysisDao.getAllAnalysesWithPlant());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).plantScientificName).isEqualTo("Monstera deliciosa");
    }

    // ==================== Phase 6 bug fix: updatePlant vs insertPlant CASCADE (06-07) ====================

    @Test
    public void updatePlant_preservesChildRecords() throws InterruptedException {
        // Insert analyses and care items for plant-1
        Analysis a1 = createTestAnalysis("a1", "plant-1", 7, "First analysis");
        analysisDao.insertAnalysis(a1);
        Analysis a2 = createTestAnalysis("a2", "plant-1", 8, "Second analysis");
        analysisDao.insertAnalysis(a2);

        CareItem care1 = createTestCareItem("c1", "plant-1", "water");
        careItemDao.insertCareItem(care1);
        CareItem care2 = createTestCareItem("c2", "plant-1", "fertilize");
        careItemDao.insertCareItem(care2);

        // Verify children exist
        List<Analysis> analysesBefore = LiveDataTestUtil.getValue(
                analysisDao.getAnalysesForPlant("plant-1"));
        assertThat(analysesBefore).hasSize(2);
        List<CareItem> careItemsBefore = LiveDataTestUtil.getValue(
                careItemDao.getCareItemsForPlant("plant-1"));
        assertThat(careItemsBefore).hasSize(2);

        // Update plant using @Update (the 06-07 fix)
        Plant plant = plantDao.getPlantByIdSync("plant-1");
        plant.commonName = "Updated Name";
        plant.latestHealthScore = 9;
        plant.updatedAt = System.currentTimeMillis();
        plantDao.updatePlant(plant);

        // Child records MUST still exist after updatePlant
        List<Analysis> analysesAfter = LiveDataTestUtil.getValue(
                analysisDao.getAnalysesForPlant("plant-1"));
        assertThat(analysesAfter).hasSize(2);
        assertThat(analysesAfter.get(0).summary).isNotEmpty();

        List<CareItem> careItemsAfter = LiveDataTestUtil.getValue(
                careItemDao.getCareItemsForPlant("plant-1"));
        assertThat(careItemsAfter).hasSize(2);

        // Plant fields should be updated
        Plant updated = plantDao.getPlantByIdSync("plant-1");
        assertThat(updated.commonName).isEqualTo("Updated Name");
        assertThat(updated.latestHealthScore).isEqualTo(9);
    }

    @Test
    public void insertPlantReplace_triggersChildCascadeDeletion() throws InterruptedException {
        // Insert analyses and care items for plant-1
        Analysis a1 = createTestAnalysis("a1", "plant-1", 7, "Will be deleted");
        analysisDao.insertAnalysis(a1);
        CareItem care1 = createTestCareItem("c1", "plant-1", "water");
        careItemDao.insertCareItem(care1);

        // Verify children exist
        assertThat(analysisDao.getAnalysisById("a1")).isNotNull();
        List<CareItem> careItemsBefore = LiveDataTestUtil.getValue(
                careItemDao.getCareItemsForPlant("plant-1"));
        assertThat(careItemsBefore).hasSize(1);

        // Re-insert same plant using insertPlant (REPLACE strategy) - this is the OLD buggy path
        Plant replacedPlant = new Plant();
        replacedPlant.id = "plant-1";
        replacedPlant.commonName = "Replaced Plant";
        replacedPlant.createdAt = System.currentTimeMillis();
        replacedPlant.updatedAt = System.currentTimeMillis();
        plantDao.insertPlant(replacedPlant);

        // CASCADE deletion should have removed all child records
        Analysis analysisAfter = analysisDao.getAnalysisById("a1");
        assertThat(analysisAfter).isNull();

        List<CareItem> careItemsAfter = LiveDataTestUtil.getValue(
                careItemDao.getCareItemsForPlant("plant-1"));
        assertThat(careItemsAfter).isEmpty();
    }

    @Test
    public void updatePlant_preservesUserFields_updatesAIFields() {
        // Set user fields on existing plant
        Plant plant = plantDao.getPlantByIdSync("plant-1");
        plant.nickname = "My Fern";
        plant.location = "Kitchen";
        long originalCreatedAt = plant.createdAt;
        plantDao.updatePlant(plant);

        // Simulate re-analysis: update only AI-derived fields
        Plant toUpdate = plantDao.getPlantByIdSync("plant-1");
        toUpdate.commonName = "New AI Name";
        toUpdate.scientificName = "Newus scientificus";
        toUpdate.latestHealthScore = 10;
        toUpdate.updatedAt = System.currentTimeMillis();
        // Do NOT touch nickname, location, createdAt
        plantDao.updatePlant(toUpdate);

        Plant result = plantDao.getPlantByIdSync("plant-1");
        // AI fields updated
        assertThat(result.commonName).isEqualTo("New AI Name");
        assertThat(result.scientificName).isEqualTo("Newus scientificus");
        assertThat(result.latestHealthScore).isEqualTo(10);
        // User fields preserved
        assertThat(result.nickname).isEqualTo("My Fern");
        assertThat(result.location).isEqualTo("Kitchen");
        assertThat(result.createdAt).isEqualTo(originalCreatedAt);
    }

    @Test
    public void updatePlant_thenInsertNewAnalysis_appendsToHistory() throws InterruptedException {
        // Existing analysis
        Analysis a1 = createTestAnalysis("a1", "plant-1", 6, "Original analysis");
        a1.createdAt = 1000L;
        analysisDao.insertAnalysis(a1);

        // Update plant (re-analysis path)
        Plant plant = plantDao.getPlantByIdSync("plant-1");
        plant.commonName = "Updated via re-analysis";
        plant.latestHealthScore = 8;
        plantDao.updatePlant(plant);

        // Insert new analysis (appends to history)
        Analysis a2 = createTestAnalysis("a2", "plant-1", 8, "Re-analysis result");
        a2.createdAt = 2000L;
        analysisDao.insertAnalysis(a2);

        // Both analyses should exist
        List<Analysis> analyses = LiveDataTestUtil.getValue(
                analysisDao.getAnalysesForPlant("plant-1"));
        assertThat(analyses).hasSize(2);
        // Newest first
        assertThat(analyses.get(0).id).isEqualTo("a2");
        assertThat(analyses.get(1).id).isEqualTo("a1");
    }

    private CareItem createTestCareItem(String id, String plantId, String type) {
        CareItem item = new CareItem();
        item.id = id;
        item.plantId = plantId;
        item.type = type;
        item.frequencyDays = 7;
        item.lastDone = System.currentTimeMillis();
        item.nextDue = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L;
        return item;
    }

    private Analysis createTestAnalysis(String id, String plantId, int score, String summary) {
        Analysis analysis = new Analysis();
        analysis.id = id;
        analysis.plantId = plantId;
        analysis.healthScore = score;
        analysis.summary = summary;
        analysis.photoPath = "/test/path.jpg";
        analysis.createdAt = System.currentTimeMillis();
        return analysis;
    }
}
