package com.leafiq.app.data.db;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.leafiq.app.data.entity.Analysis;
import com.leafiq.app.data.entity.Plant;

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

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
            .allowMainThreadQueries()
            .build();
        plantDao = database.plantDao();
        analysisDao = database.analysisDao();

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
