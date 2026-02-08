package com.leafiq.app.data.repository;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leafiq.app.data.db.AnalysisDao;
import com.leafiq.app.data.db.CareItemDao;
import com.leafiq.app.data.db.PlantDao;
import com.leafiq.app.data.entity.Plant;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class PlantRepositoryTest {

    private PlantDao mockPlantDao;
    private AnalysisDao mockAnalysisDao;
    private CareItemDao mockCareItemDao;
    private PlantRepository repository;

    @Before
    public void setUp() {
        mockPlantDao = mock(PlantDao.class);
        mockAnalysisDao = mock(AnalysisDao.class);
        mockCareItemDao = mock(CareItemDao.class);
        // Synchronous executor for tests
        repository = new PlantRepository(mockPlantDao, mockAnalysisDao, mockCareItemDao, Runnable::run);
    }

    // ==================== deletePlant tests ====================

    @Test
    public void deletePlant_callsGetPhotoPathsAndDeletesFiles() {
        Plant plant = createTestPlant("p1");
        plant.thumbnailPath = null;
        when(mockAnalysisDao.getPhotoPathsForPlantSync("p1")).thenReturn(new ArrayList<>());

        AtomicBoolean successCalled = new AtomicBoolean(false);
        repository.deletePlant(plant, new PlantRepository.RepositoryCallback<Void>() {
            @Override public void onSuccess(Void result) { successCalled.set(true); }
            @Override public void onError(Exception e) {}
        });

        verify(mockAnalysisDao).getPhotoPathsForPlantSync("p1");
        assertThat(successCalled.get()).isTrue();
    }

    @Test
    public void deletePlant_callsDaoDeleteAfterPhotoCleanup() {
        Plant plant = createTestPlant("p1");
        plant.thumbnailPath = null;
        when(mockAnalysisDao.getPhotoPathsForPlantSync("p1")).thenReturn(new ArrayList<>());

        repository.deletePlant(plant, new PlantRepository.RepositoryCallback<Void>() {
            @Override public void onSuccess(Void result) {}
            @Override public void onError(Exception e) {}
        });

        verify(mockPlantDao).deletePlant(plant);
    }

    @Test
    public void deletePlant_withNullThumbnailPath_skipsThumbCleanup() {
        Plant plant = createTestPlant("p1");
        plant.thumbnailPath = null;
        when(mockAnalysisDao.getPhotoPathsForPlantSync("p1")).thenReturn(new ArrayList<>());

        AtomicBoolean successCalled = new AtomicBoolean(false);
        repository.deletePlant(plant, new PlantRepository.RepositoryCallback<Void>() {
            @Override public void onSuccess(Void result) { successCalled.set(true); }
            @Override public void onError(Exception e) {}
        });

        // Should succeed without NPE even though thumbnailPath is null
        assertThat(successCalled.get()).isTrue();
        verify(mockPlantDao).deletePlant(plant);
    }

    @Test
    public void deletePlant_onSuccess_callsCallbackSuccess() {
        Plant plant = createTestPlant("p1");
        plant.thumbnailPath = null;
        when(mockAnalysisDao.getPhotoPathsForPlantSync("p1")).thenReturn(new ArrayList<>());

        AtomicBoolean successCalled = new AtomicBoolean(false);
        repository.deletePlant(plant, new PlantRepository.RepositoryCallback<Void>() {
            @Override public void onSuccess(Void result) { successCalled.set(true); }
            @Override public void onError(Exception e) {}
        });

        assertThat(successCalled.get()).isTrue();
    }

    @Test
    public void deletePlant_onException_callsCallbackError() {
        Plant plant = createTestPlant("p1");
        plant.thumbnailPath = null;
        when(mockAnalysisDao.getPhotoPathsForPlantSync("p1"))
                .thenThrow(new RuntimeException("DB error"));

        AtomicReference<Exception> capturedError = new AtomicReference<>();
        repository.deletePlant(plant, new PlantRepository.RepositoryCallback<Void>() {
            @Override public void onSuccess(Void result) {}
            @Override public void onError(Exception e) { capturedError.set(e); }
        });

        assertThat(capturedError.get()).isNotNull();
        assertThat(capturedError.get().getMessage()).contains("DB error");
    }

    // ==================== updatePlant tests ====================

    @Test
    public void updatePlant_callsDaoUpdate_andCallbackSuccess() {
        Plant plant = createTestPlant("p1");

        AtomicBoolean successCalled = new AtomicBoolean(false);
        repository.updatePlant(plant, new PlantRepository.RepositoryCallback<Void>() {
            @Override public void onSuccess(Void result) { successCalled.set(true); }
            @Override public void onError(Exception e) {}
        });

        verify(mockPlantDao).updatePlant(plant);
        assertThat(successCalled.get()).isTrue();
    }

    @Test
    public void updatePlant_onException_callsCallbackError() {
        Plant plant = createTestPlant("p1");
        doThrow(new RuntimeException("Update failed")).when(mockPlantDao).updatePlant(any());

        AtomicReference<Exception> capturedError = new AtomicReference<>();
        repository.updatePlant(plant, new PlantRepository.RepositoryCallback<Void>() {
            @Override public void onSuccess(Void result) {}
            @Override public void onError(Exception e) { capturedError.set(e); }
        });

        assertThat(capturedError.get()).isNotNull();
        assertThat(capturedError.get().getMessage()).contains("Update failed");
    }

    // ==================== updateAnalysis tests ====================

    @Test
    public void updateAnalysis_callsDaoUpdate_andCallbackSuccess() {
        com.leafiq.app.data.entity.Analysis analysis = new com.leafiq.app.data.entity.Analysis();
        analysis.id = "a1";

        AtomicBoolean successCalled = new AtomicBoolean(false);
        repository.updateAnalysis(analysis, new PlantRepository.RepositoryCallback<Void>() {
            @Override public void onSuccess(Void result) { successCalled.set(true); }
            @Override public void onError(Exception e) {}
        });

        verify(mockAnalysisDao).updateAnalysis(analysis);
        assertThat(successCalled.get()).isTrue();
    }

    // ==================== deleteAnalysis tests ====================

    @Test
    public void deleteAnalysis_callsDaoDeleteById() {
        AtomicBoolean successCalled = new AtomicBoolean(false);
        repository.deleteAnalysis("a1", new PlantRepository.RepositoryCallback<Void>() {
            @Override public void onSuccess(Void result) { successCalled.set(true); }
            @Override public void onError(Exception e) {}
        });

        verify(mockAnalysisDao).deleteAnalysisById("a1");
        assertThat(successCalled.get()).isTrue();
    }

    // ==================== updatePlantName tests ====================

    @Test
    public void updatePlantName_existingPlant_updatesNameAndTimestamp() {
        Plant existingPlant = createTestPlant("p1");
        existingPlant.commonName = "Old Name";
        long beforeTimestamp = System.currentTimeMillis();
        when(mockPlantDao.getPlantByIdSync("p1")).thenReturn(existingPlant);

        AtomicBoolean successCalled = new AtomicBoolean(false);
        repository.updatePlantName("p1", "New Name", new PlantRepository.RepositoryCallback<Void>() {
            @Override public void onSuccess(Void result) { successCalled.set(true); }
            @Override public void onError(Exception e) {}
        });

        assertThat(successCalled.get()).isTrue();
        assertThat(existingPlant.commonName).isEqualTo("New Name");
        assertThat(existingPlant.updatedAt).isAtLeast(beforeTimestamp);
        verify(mockPlantDao).updatePlant(existingPlant);
    }

    @Test
    public void updatePlantName_nonexistentPlant_callsCallbackError() {
        when(mockPlantDao.getPlantByIdSync("nonexistent")).thenReturn(null);

        AtomicReference<Exception> capturedError = new AtomicReference<>();
        repository.updatePlantName("nonexistent", "Name", new PlantRepository.RepositoryCallback<Void>() {
            @Override public void onSuccess(Void result) {}
            @Override public void onError(Exception e) { capturedError.set(e); }
        });

        assertThat(capturedError.get()).isNotNull();
        assertThat(capturedError.get().getMessage()).contains("Plant not found");
        verify(mockPlantDao, never()).updatePlant(any());
    }

    // ==================== getDistinctLocations tests ====================

    @Test
    public void getDistinctLocations_returnsListFromDao() {
        List<String> locations = Arrays.asList("Living Room", "Bedroom", "Office");
        when(mockPlantDao.getDistinctLocations()).thenReturn(locations);

        AtomicReference<List<String>> capturedResult = new AtomicReference<>();
        repository.getDistinctLocations(new PlantRepository.RepositoryCallback<List<String>>() {
            @Override public void onSuccess(List<String> result) { capturedResult.set(result); }
            @Override public void onError(Exception e) {}
        });

        assertThat(capturedResult.get()).isEqualTo(locations);
    }

    // ==================== Helpers ====================

    private Plant createTestPlant(String id) {
        Plant plant = new Plant();
        plant.id = id;
        plant.commonName = "Test Plant";
        plant.createdAt = System.currentTimeMillis();
        plant.updatedAt = System.currentTimeMillis();
        return plant;
    }
}
