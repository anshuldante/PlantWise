package com.leafiq.app.data.db;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.leafiq.app.data.entity.Analysis;
import com.leafiq.app.data.entity.Plant;
import com.leafiq.app.util.ImageUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Integration tests for the save plant flow.
 * These tests verify the complete flow of saving a plant with analysis,
 * including the database operations and image handling.
 */
@RunWith(AndroidJUnit4.class)
public class SavePlantFlowTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase database;
    private PlantDao plantDao;
    private AnalysisDao analysisDao;
    private Context context;
    private Uri testImageUri;

    @Before
    public void setUp() throws IOException {
        context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
            .allowMainThreadQueries()
            .build();
        plantDao = database.plantDao();
        analysisDao = database.analysisDao();

        // Create test image
        testImageUri = createTestImage();
    }

    @After
    public void tearDown() {
        database.close();
        // Cleanup test image
        if (testImageUri != null) {
            new File(testImageUri.getPath()).delete();
        }
    }

    private Uri createTestImage() throws IOException {
        Bitmap bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(0xFF00FF00);

        File cacheDir = context.getCacheDir();
        File testFile = new File(cacheDir, "save_flow_test_image.jpg");
        try (FileOutputStream out = new FileOutputStream(testFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        }
        bitmap.recycle();

        return Uri.fromFile(testFile);
    }

    @Test
    public void savePlantFlow_newPlant_savesPlantAndAnalysis() throws IOException, InterruptedException {
        // Simulate the save flow from AnalysisActivity
        String plantId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        // Create and save plant
        Plant plant = new Plant();
        plant.id = plantId;
        plant.commonName = "Test Monstera";
        plant.scientificName = "Monstera deliciosa";
        plant.latestHealthScore = 8;
        plant.createdAt = now;
        plant.updatedAt = now;

        // Save thumbnail
        String thumbnailPath = ImageUtils.saveThumbnail(context, testImageUri, plantId);
        plant.thumbnailPath = thumbnailPath;

        plantDao.insertPlant(plant);

        // Save photo and analysis
        String photoPath = ImageUtils.savePhoto(context, testImageUri, plantId);

        Analysis analysis = new Analysis();
        analysis.id = UUID.randomUUID().toString();
        analysis.plantId = plantId;
        analysis.photoPath = photoPath;
        analysis.healthScore = 8;
        analysis.summary = "Plant looks healthy";
        analysis.createdAt = now;

        analysisDao.insertAnalysis(analysis);

        // Verify plant was saved
        Plant savedPlant = plantDao.getPlantByIdSync(plantId);
        assertThat(savedPlant).isNotNull();
        assertThat(savedPlant.commonName).isEqualTo("Test Monstera");
        assertThat(savedPlant.thumbnailPath).isNotNull();
        assertThat(new File(savedPlant.thumbnailPath).exists()).isTrue();

        // Verify analysis was saved
        List<Analysis> analyses = LiveDataTestUtil.getValue(
            analysisDao.getAnalysesForPlant(plantId));
        assertThat(analyses).hasSize(1);
        assertThat(analyses.get(0).photoPath).isNotNull();
        assertThat(new File(analyses.get(0).photoPath).exists()).isTrue();

        // Cleanup
        new File(thumbnailPath).delete();
        new File(photoPath).delete();
    }

    @Test
    public void savePlantFlow_existingPlant_updatesPlantAndAddsAnalysis() throws IOException, InterruptedException {
        // First, create an existing plant
        String plantId = UUID.randomUUID().toString();
        long originalTime = System.currentTimeMillis() - 10000;

        Plant existingPlant = new Plant();
        existingPlant.id = plantId;
        existingPlant.commonName = "Original Name";
        existingPlant.scientificName = "Original Scientific";
        existingPlant.latestHealthScore = 5;
        existingPlant.createdAt = originalTime;
        existingPlant.updatedAt = originalTime;
        plantDao.insertPlant(existingPlant);

        // Now simulate re-analyzing with updated data
        long now = System.currentTimeMillis();

        Plant plant = plantDao.getPlantByIdSync(plantId);
        assertThat(plant).isNotNull();

        plant.commonName = "Updated Monstera";
        plant.latestHealthScore = 9;
        plant.updatedAt = now;

        String thumbnailPath = ImageUtils.saveThumbnail(context, testImageUri, plantId);
        plant.thumbnailPath = thumbnailPath;

        plantDao.insertPlant(plant); // Uses REPLACE strategy

        // Add new analysis
        String photoPath = ImageUtils.savePhoto(context, testImageUri, plantId);

        Analysis analysis = new Analysis();
        analysis.id = UUID.randomUUID().toString();
        analysis.plantId = plantId;
        analysis.photoPath = photoPath;
        analysis.healthScore = 9;
        analysis.summary = "Improved health";
        analysis.createdAt = now;

        analysisDao.insertAnalysis(analysis);

        // Verify plant was updated
        Plant updatedPlant = plantDao.getPlantByIdSync(plantId);
        assertThat(updatedPlant.commonName).isEqualTo("Updated Monstera");
        assertThat(updatedPlant.latestHealthScore).isEqualTo(9);
        assertThat(updatedPlant.createdAt).isEqualTo(originalTime); // Original creation time preserved
        assertThat(updatedPlant.updatedAt).isEqualTo(now);

        // Verify analysis was added
        List<Analysis> analyses = LiveDataTestUtil.getValue(
            analysisDao.getAnalysesForPlant(plantId));
        assertThat(analyses).hasSize(1);

        // Cleanup
        new File(thumbnailPath).delete();
        new File(photoPath).delete();
    }

    @Test
    public void savePlantFlow_withNullThumbnail_stillSavesPlant() throws InterruptedException {
        // Test that plant can be saved even without thumbnail
        String plantId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        Plant plant = new Plant();
        plant.id = plantId;
        plant.commonName = "No Thumbnail Plant";
        plant.scientificName = "Plantus nullus";
        plant.latestHealthScore = 7;
        plant.createdAt = now;
        plant.updatedAt = now;
        plant.thumbnailPath = null; // Simulates failed image save

        plantDao.insertPlant(plant);

        // Verify plant was saved
        Plant savedPlant = plantDao.getPlantByIdSync(plantId);
        assertThat(savedPlant).isNotNull();
        assertThat(savedPlant.commonName).isEqualTo("No Thumbnail Plant");
        assertThat(savedPlant.thumbnailPath).isNull();
    }

    @Test
    public void savePlantFlow_withNullPhotoPath_stillSavesAnalysis() throws InterruptedException {
        // First create a plant
        String plantId = UUID.randomUUID().toString();
        Plant plant = new Plant();
        plant.id = plantId;
        plant.commonName = "Test Plant";
        plant.createdAt = System.currentTimeMillis();
        plant.updatedAt = System.currentTimeMillis();
        plantDao.insertPlant(plant);

        // Test that analysis can be saved even without photo path
        Analysis analysis = new Analysis();
        analysis.id = UUID.randomUUID().toString();
        analysis.plantId = plantId;
        analysis.photoPath = null; // Simulates failed image save
        analysis.healthScore = 6;
        analysis.summary = "Analysis without photo";
        analysis.createdAt = System.currentTimeMillis();

        analysisDao.insertAnalysis(analysis);

        // Verify analysis was saved
        Analysis savedAnalysis = analysisDao.getAnalysisById(analysis.id);
        assertThat(savedAnalysis).isNotNull();
        assertThat(savedAnalysis.summary).isEqualTo("Analysis without photo");
        assertThat(savedAnalysis.photoPath).isNull();
    }

    @Test
    public void savePlantFlow_multipleAnalyses_allSaved() throws IOException, InterruptedException {
        String plantId = UUID.randomUUID().toString();

        // Create plant
        Plant plant = new Plant();
        plant.id = plantId;
        plant.commonName = "Multi Analysis Plant";
        plant.createdAt = System.currentTimeMillis();
        plant.updatedAt = System.currentTimeMillis();
        plantDao.insertPlant(plant);

        // Add multiple analyses
        for (int i = 0; i < 5; i++) {
            Analysis analysis = new Analysis();
            analysis.id = UUID.randomUUID().toString();
            analysis.plantId = plantId;
            analysis.healthScore = 5 + i;
            analysis.summary = "Analysis " + i;
            analysis.createdAt = System.currentTimeMillis() + i * 1000;
            analysisDao.insertAnalysis(analysis);
        }

        // Verify all analyses were saved
        List<Analysis> analyses = LiveDataTestUtil.getValue(
            analysisDao.getAnalysesForPlant(plantId));
        assertThat(analyses).hasSize(5);
    }
}
