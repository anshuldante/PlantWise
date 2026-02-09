package com.leafiq.app.data.repository;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.leafiq.app.data.db.AnalysisDao;
import com.leafiq.app.data.db.CareItemDao;
import com.leafiq.app.data.db.PlantDao;
import com.leafiq.app.data.entity.Analysis;
import com.leafiq.app.data.entity.CareItem;
import com.leafiq.app.data.entity.Plant;
import com.leafiq.app.data.model.AnalysisWithPlant;

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

    // ==================== LiveData read delegation tests ====================

    @Test
    public void getAllPlants_delegatesToPlantDao() {
        MutableLiveData<List<Plant>> expected = new MutableLiveData<>();
        when(mockPlantDao.getAllPlants()).thenReturn(expected);

        LiveData<List<Plant>> result = repository.getAllPlants();

        assertThat(result).isSameInstanceAs(expected);
        verify(mockPlantDao).getAllPlants();
    }

    @Test
    public void getPlantById_delegatesToPlantDao() {
        MutableLiveData<Plant> expected = new MutableLiveData<>();
        when(mockPlantDao.getPlantById("p1")).thenReturn(expected);

        LiveData<Plant> result = repository.getPlantById("p1");

        assertThat(result).isSameInstanceAs(expected);
        verify(mockPlantDao).getPlantById("p1");
    }

    @Test
    public void getAnalysesForPlant_delegatesToAnalysisDao() {
        MutableLiveData<List<Analysis>> expected = new MutableLiveData<>();
        when(mockAnalysisDao.getAnalysesForPlant("p1")).thenReturn(expected);

        LiveData<List<Analysis>> result = repository.getAnalysesForPlant("p1");

        assertThat(result).isSameInstanceAs(expected);
        verify(mockAnalysisDao).getAnalysesForPlant("p1");
    }

    @Test
    public void getCareItemsForPlant_delegatesToCareItemDao() {
        MutableLiveData<List<CareItem>> expected = new MutableLiveData<>();
        when(mockCareItemDao.getCareItemsForPlant("p1")).thenReturn(expected);

        LiveData<List<CareItem>> result = repository.getCareItemsForPlant("p1");

        assertThat(result).isSameInstanceAs(expected);
        verify(mockCareItemDao).getCareItemsForPlant("p1");
    }

    @Test
    public void getOverdueItems_delegatesToCareItemDao() {
        MutableLiveData<List<CareItem>> expected = new MutableLiveData<>();
        when(mockCareItemDao.getOverdueItems(5000L)).thenReturn(expected);

        LiveData<List<CareItem>> result = repository.getOverdueItems(5000L);

        assertThat(result).isSameInstanceAs(expected);
        verify(mockCareItemDao).getOverdueItems(5000L);
    }

    @Test
    public void getAllAnalysesWithPlant_delegatesToAnalysisDao() {
        MutableLiveData<List<AnalysisWithPlant>> expected = new MutableLiveData<>();
        when(mockAnalysisDao.getAllAnalysesWithPlant()).thenReturn(expected);

        LiveData<List<AnalysisWithPlant>> result = repository.getAllAnalysesWithPlant();

        assertThat(result).isSameInstanceAs(expected);
        verify(mockAnalysisDao).getAllAnalysesWithPlant();
    }

    @Test
    public void getAnalysesWithPlantForPlant_delegatesToAnalysisDao() {
        MutableLiveData<List<AnalysisWithPlant>> expected = new MutableLiveData<>();
        when(mockAnalysisDao.getAnalysesWithPlantForPlant("p1")).thenReturn(expected);

        LiveData<List<AnalysisWithPlant>> result = repository.getAnalysesWithPlantForPlant("p1");

        assertThat(result).isSameInstanceAs(expected);
        verify(mockAnalysisDao).getAnalysesWithPlantForPlant("p1");
    }

    // ==================== Sync read delegation tests ====================

    @Test
    public void getPlantByIdSync_delegatesToPlantDao() {
        Plant expected = createTestPlant("p1");
        when(mockPlantDao.getPlantByIdSync("p1")).thenReturn(expected);

        Plant result = repository.getPlantByIdSync("p1");

        assertThat(result).isSameInstanceAs(expected);
        verify(mockPlantDao).getPlantByIdSync("p1");
    }

    @Test
    public void getRecentAnalysesSync_delegatesToAnalysisDao() {
        List<Analysis> expected = new ArrayList<>();
        when(mockAnalysisDao.getRecentAnalysesSync("p1")).thenReturn(expected);

        List<Analysis> result = repository.getRecentAnalysesSync("p1");

        assertThat(result).isSameInstanceAs(expected);
        verify(mockAnalysisDao).getRecentAnalysesSync("p1");
    }

    @Test
    public void getAnalysisByIdSync_delegatesToAnalysisDao() {
        Analysis expected = new Analysis();
        expected.id = "a1";
        when(mockAnalysisDao.getAnalysisById("a1")).thenReturn(expected);

        Analysis result = repository.getAnalysisByIdSync("a1");

        assertThat(result).isSameInstanceAs(expected);
        verify(mockAnalysisDao).getAnalysisById("a1");
    }

    // ==================== Insert method tests ====================

    @Test
    public void insertPlant_callsDaoInsert_andCallbackSuccess() {
        Plant plant = createTestPlant("p1");

        AtomicBoolean successCalled = new AtomicBoolean(false);
        repository.insertPlant(plant, new PlantRepository.RepositoryCallback<Void>() {
            @Override public void onSuccess(Void result) { successCalled.set(true); }
            @Override public void onError(Exception e) {}
        });

        verify(mockPlantDao).insertPlant(plant);
        assertThat(successCalled.get()).isTrue();
    }

    @Test
    public void insertPlant_onException_callsCallbackError() {
        Plant plant = createTestPlant("p1");
        doThrow(new RuntimeException("Insert failed")).when(mockPlantDao).insertPlant(any());

        AtomicReference<Exception> capturedError = new AtomicReference<>();
        repository.insertPlant(plant, new PlantRepository.RepositoryCallback<Void>() {
            @Override public void onSuccess(Void result) {}
            @Override public void onError(Exception e) { capturedError.set(e); }
        });

        assertThat(capturedError.get()).isNotNull();
        assertThat(capturedError.get().getMessage()).contains("Insert failed");
    }

    @Test
    public void insertAnalysis_callsDaoInsert_andCallbackSuccess() {
        Analysis analysis = new Analysis();
        analysis.id = "a1";

        AtomicBoolean successCalled = new AtomicBoolean(false);
        repository.insertAnalysis(analysis, new PlantRepository.RepositoryCallback<Void>() {
            @Override public void onSuccess(Void result) { successCalled.set(true); }
            @Override public void onError(Exception e) {}
        });

        verify(mockAnalysisDao).insertAnalysis(analysis);
        assertThat(successCalled.get()).isTrue();
    }

    @Test
    public void insertCareItem_callsDaoInsert_andCallbackSuccess() {
        CareItem item = new CareItem();
        item.id = "c1";

        AtomicBoolean successCalled = new AtomicBoolean(false);
        repository.insertCareItem(item, new PlantRepository.RepositoryCallback<Void>() {
            @Override public void onSuccess(Void result) { successCalled.set(true); }
            @Override public void onError(Exception e) {}
        });

        verify(mockCareItemDao).insertCareItem(item);
        assertThat(successCalled.get()).isTrue();
    }

    // ==================== savePlantWithAnalysis tests ====================

    @Test
    public void savePlantWithAnalysis_insertsAllEntities() {
        Plant plant = createTestPlant("p1");
        Analysis analysis = new Analysis();
        analysis.id = "a1";
        analysis.plantId = "p1";
        CareItem care1 = new CareItem();
        care1.id = "c1";
        CareItem care2 = new CareItem();
        care2.id = "c2";
        List<CareItem> careItems = Arrays.asList(care1, care2);

        AtomicBoolean successCalled = new AtomicBoolean(false);
        repository.savePlantWithAnalysis(plant, analysis, careItems,
                new PlantRepository.RepositoryCallback<Void>() {
            @Override public void onSuccess(Void result) { successCalled.set(true); }
            @Override public void onError(Exception e) {}
        });

        verify(mockPlantDao).insertPlant(plant);
        verify(mockAnalysisDao).insertAnalysis(analysis);
        verify(mockCareItemDao).insertCareItem(care1);
        verify(mockCareItemDao).insertCareItem(care2);
        assertThat(successCalled.get()).isTrue();
    }

    @Test
    public void savePlantWithAnalysis_onException_callsCallbackError() {
        Plant plant = createTestPlant("p1");
        Analysis analysis = new Analysis();
        analysis.id = "a1";
        doThrow(new RuntimeException("DB error")).when(mockPlantDao).insertPlant(any());

        AtomicReference<Exception> capturedError = new AtomicReference<>();
        repository.savePlantWithAnalysis(plant, analysis, new ArrayList<>(),
                new PlantRepository.RepositoryCallback<Void>() {
            @Override public void onSuccess(Void result) {}
            @Override public void onError(Exception e) { capturedError.set(e); }
        });

        assertThat(capturedError.get()).isNotNull();
        assertThat(capturedError.get().getMessage()).contains("DB error");
    }

    @Test
    public void savePlantWithAnalysis_emptyCareItems_succeeds() {
        Plant plant = createTestPlant("p1");
        Analysis analysis = new Analysis();
        analysis.id = "a1";

        AtomicBoolean successCalled = new AtomicBoolean(false);
        repository.savePlantWithAnalysis(plant, analysis, new ArrayList<>(),
                new PlantRepository.RepositoryCallback<Void>() {
            @Override public void onSuccess(Void result) { successCalled.set(true); }
            @Override public void onError(Exception e) {}
        });

        verify(mockPlantDao).insertPlant(plant);
        verify(mockAnalysisDao).insertAnalysis(analysis);
        verify(mockCareItemDao, never()).insertCareItem(any());
        assertThat(successCalled.get()).isTrue();
    }

    // ==================== addAnalysisToExistingPlant tests (06-07 fix) ====================

    @Test
    public void addAnalysisToExistingPlant_updatesAIFields_preservesUserFields() {
        Plant existingPlant = createTestPlant("p1");
        existingPlant.nickname = "My Fern";
        existingPlant.location = "Living Room";
        existingPlant.commonName = "Old Name";
        existingPlant.scientificName = "Old Scientific";
        existingPlant.latestHealthScore = 5;
        existingPlant.thumbnailPath = "/old/thumb.jpg";
        long originalCreatedAt = existingPlant.createdAt;
        when(mockPlantDao.getPlantByIdSync("p1")).thenReturn(existingPlant);

        Analysis analysis = new Analysis();
        analysis.id = "a1";
        analysis.plantId = "p1";
        CareItem care1 = new CareItem();
        care1.id = "c1";
        List<CareItem> careItems = Arrays.asList(care1);

        AtomicBoolean successCalled = new AtomicBoolean(false);
        repository.addAnalysisToExistingPlant("p1", "New Name", "New Scientific", 9,
                "/new/thumb.jpg", analysis, careItems,
                new PlantRepository.RepositoryCallback<Void>() {
                    @Override public void onSuccess(Void result) { successCalled.set(true); }
                    @Override public void onError(Exception e) {}
                });

        assertThat(successCalled.get()).isTrue();
        // AI-derived fields updated
        assertThat(existingPlant.commonName).isEqualTo("New Name");
        assertThat(existingPlant.scientificName).isEqualTo("New Scientific");
        assertThat(existingPlant.latestHealthScore).isEqualTo(9);
        assertThat(existingPlant.thumbnailPath).isEqualTo("/new/thumb.jpg");
        // User-set fields preserved
        assertThat(existingPlant.nickname).isEqualTo("My Fern");
        assertThat(existingPlant.location).isEqualTo("Living Room");
        assertThat(existingPlant.createdAt).isEqualTo(originalCreatedAt);
    }

    @Test
    public void addAnalysisToExistingPlant_usesUpdateNotInsert() {
        Plant existingPlant = createTestPlant("p1");
        when(mockPlantDao.getPlantByIdSync("p1")).thenReturn(existingPlant);

        Analysis analysis = new Analysis();
        analysis.id = "a1";
        analysis.plantId = "p1";

        repository.addAnalysisToExistingPlant("p1", "Name", "Sci", 8,
                "/thumb.jpg", analysis, new ArrayList<>(),
                new PlantRepository.RepositoryCallback<Void>() {
                    @Override public void onSuccess(Void result) {}
                    @Override public void onError(Exception e) {}
                });

        // Must use updatePlant (not insertPlant) to avoid CASCADE deletion
        verify(mockPlantDao).updatePlant(existingPlant);
        verify(mockPlantDao, never()).insertPlant(any());
    }

    @Test
    public void addAnalysisToExistingPlant_insertsNewAnalysisAndCareItems() {
        Plant existingPlant = createTestPlant("p1");
        when(mockPlantDao.getPlantByIdSync("p1")).thenReturn(existingPlant);

        Analysis analysis = new Analysis();
        analysis.id = "a-new";
        analysis.plantId = "p1";
        CareItem care1 = new CareItem();
        care1.id = "c1";
        CareItem care2 = new CareItem();
        care2.id = "c2";
        List<CareItem> careItems = Arrays.asList(care1, care2);

        repository.addAnalysisToExistingPlant("p1", "Name", "Sci", 7,
                "/thumb.jpg", analysis, careItems,
                new PlantRepository.RepositoryCallback<Void>() {
                    @Override public void onSuccess(Void result) {}
                    @Override public void onError(Exception e) {}
                });

        verify(mockAnalysisDao).insertAnalysis(analysis);
        verify(mockCareItemDao).insertCareItem(care1);
        verify(mockCareItemDao).insertCareItem(care2);
    }

    @Test
    public void addAnalysisToExistingPlant_nullThumbnail_preservesExisting() {
        Plant existingPlant = createTestPlant("p1");
        existingPlant.thumbnailPath = "/existing/thumb.jpg";
        when(mockPlantDao.getPlantByIdSync("p1")).thenReturn(existingPlant);

        Analysis analysis = new Analysis();
        analysis.id = "a1";
        analysis.plantId = "p1";

        repository.addAnalysisToExistingPlant("p1", "Name", "Sci", 7,
                null, analysis, new ArrayList<>(),
                new PlantRepository.RepositoryCallback<Void>() {
                    @Override public void onSuccess(Void result) {}
                    @Override public void onError(Exception e) {}
                });

        // Thumbnail should be preserved when new one is null
        assertThat(existingPlant.thumbnailPath).isEqualTo("/existing/thumb.jpg");
        verify(mockPlantDao).updatePlant(existingPlant);
    }

    @Test
    public void addAnalysisToExistingPlant_nonexistentPlant_callsError() {
        when(mockPlantDao.getPlantByIdSync("nonexistent")).thenReturn(null);

        Analysis analysis = new Analysis();
        analysis.id = "a1";

        AtomicReference<Exception> capturedError = new AtomicReference<>();
        repository.addAnalysisToExistingPlant("nonexistent", "Name", "Sci", 7,
                "/thumb.jpg", analysis, new ArrayList<>(),
                new PlantRepository.RepositoryCallback<Void>() {
                    @Override public void onSuccess(Void result) {}
                    @Override public void onError(Exception e) { capturedError.set(e); }
                });

        assertThat(capturedError.get()).isNotNull();
        assertThat(capturedError.get().getMessage()).contains("Plant not found");
        verify(mockPlantDao, never()).updatePlant(any());
        verify(mockAnalysisDao, never()).insertAnalysis(any());
    }

    @Test
    public void addAnalysisToExistingPlant_updatesTimestamp() {
        Plant existingPlant = createTestPlant("p1");
        existingPlant.updatedAt = 1000L;
        when(mockPlantDao.getPlantByIdSync("p1")).thenReturn(existingPlant);

        long beforeCall = System.currentTimeMillis();
        Analysis analysis = new Analysis();
        analysis.id = "a1";
        analysis.plantId = "p1";

        repository.addAnalysisToExistingPlant("p1", "Name", "Sci", 7,
                "/thumb.jpg", analysis, new ArrayList<>(),
                new PlantRepository.RepositoryCallback<Void>() {
                    @Override public void onSuccess(Void result) {}
                    @Override public void onError(Exception e) {}
                });

        assertThat(existingPlant.updatedAt).isAtLeast(beforeCall);
    }

    @Test
    public void addAnalysisToExistingPlant_emptyCareItems_succeeds() {
        Plant existingPlant = createTestPlant("p1");
        when(mockPlantDao.getPlantByIdSync("p1")).thenReturn(existingPlant);

        Analysis analysis = new Analysis();
        analysis.id = "a1";
        analysis.plantId = "p1";

        AtomicBoolean successCalled = new AtomicBoolean(false);
        repository.addAnalysisToExistingPlant("p1", "Name", "Sci", 7,
                "/thumb.jpg", analysis, new ArrayList<>(),
                new PlantRepository.RepositoryCallback<Void>() {
                    @Override public void onSuccess(Void result) { successCalled.set(true); }
                    @Override public void onError(Exception e) {}
                });

        assertThat(successCalled.get()).isTrue();
        verify(mockCareItemDao, never()).insertCareItem(any());
    }

    @Test
    public void addAnalysisToExistingPlant_onDaoException_callsError() {
        Plant existingPlant = createTestPlant("p1");
        when(mockPlantDao.getPlantByIdSync("p1")).thenReturn(existingPlant);
        doThrow(new RuntimeException("DB error")).when(mockPlantDao).updatePlant(any());

        Analysis analysis = new Analysis();
        analysis.id = "a1";
        analysis.plantId = "p1";

        AtomicReference<Exception> capturedError = new AtomicReference<>();
        repository.addAnalysisToExistingPlant("p1", "Name", "Sci", 7,
                "/thumb.jpg", analysis, new ArrayList<>(),
                new PlantRepository.RepositoryCallback<Void>() {
                    @Override public void onSuccess(Void result) {}
                    @Override public void onError(Exception e) { capturedError.set(e); }
                });

        assertThat(capturedError.get()).isNotNull();
        assertThat(capturedError.get().getMessage()).contains("DB error");
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
