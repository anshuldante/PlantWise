package com.leafiq.app.ui.detail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.leafiq.app.LeafIQApplication;
import com.leafiq.app.care.CareScheduleManager;
import com.leafiq.app.data.entity.Analysis;
import com.leafiq.app.data.entity.CareCompletion;
import com.leafiq.app.data.entity.CareSchedule;
import com.leafiq.app.data.entity.Plant;
import com.leafiq.app.data.repository.PlantRepository;

import java.util.List;

public class PlantDetailViewModel extends AndroidViewModel {

    private final PlantRepository repository;
    private final CareScheduleManager careScheduleManager;

    public PlantDetailViewModel(@NonNull Application application) {
        super(application);
        LeafIQApplication app = (LeafIQApplication) application;
        repository = app.getPlantRepository();
        careScheduleManager = app.getCareScheduleManager();
    }

    public LiveData<Plant> getPlant(String plantId) {
        return repository.getPlantById(plantId);
    }

    public LiveData<List<Analysis>> getAnalyses(String plantId) {
        return repository.getAnalysesForPlant(plantId);
    }

    public void updatePlant(Plant plant, PlantRepository.RepositoryCallback<Void> callback) {
        repository.updatePlant(plant, callback);
    }

    public void getDistinctLocations(PlantRepository.RepositoryCallback<List<String>> callback) {
        repository.getDistinctLocations(callback);
    }

    public void deletePlant(Plant plant, PlantRepository.RepositoryCallback<Void> callback) {
        repository.deletePlant(plant, callback);
    }

    public void updateAnalysis(Analysis analysis, PlantRepository.RepositoryCallback<Void> callback) {
        repository.updateAnalysis(analysis, callback);
    }

    public LiveData<List<CareSchedule>> getSchedulesForPlant(String plantId) {
        return repository.getSchedulesForPlant(plantId);
    }

    public LiveData<List<CareCompletion>> getRecentCompletionsForPlant(String plantId, int limit) {
        return repository.getRecentCompletionsForPlant(plantId, limit);
    }

    public void toggleReminders(String plantId, boolean enabled, PlantRepository.RepositoryCallback<Void> callback) {
        // Run on background thread
        new Thread(() -> {
            try {
                careScheduleManager.toggleRemindersForPlant(plantId, enabled);
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    public void updateScheduleFrequency(String scheduleId, int newFrequencyDays, PlantRepository.RepositoryCallback<Void> callback) {
        // Run on background thread
        new Thread(() -> {
            try {
                careScheduleManager.updateScheduleFrequency(scheduleId, newFrequencyDays);
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    /**
     * Get the photo path from the most recent analysis for lazy thumbnail regeneration.
     */
    public void getLatestPhotoPath(String plantId, java.util.function.Consumer<String> callback) {
        LeafIQApplication app = (LeafIQApplication) getApplication();
        app.getAppExecutors().io().execute(() -> {
            Analysis latest = repository.getLatestAnalysisSync(plantId);
            if (latest != null && latest.photoPath != null) {
                callback.accept(latest.photoPath);
            } else {
                callback.accept(null);
            }
        });
    }

    public LiveData<Integer> getAnalysisCount(String plantId) {
        return repository.getAnalysisCountForPlant(plantId);
    }

    public LiveData<Integer> getCareCompletionCount(String plantId) {
        return repository.getCareCompletionCountForPlant(plantId);
    }

    public LiveData<List<CareCompletion>> getLimitedCompletions(String plantId, int limit) {
        return repository.getLimitedCompletionsForPlant(plantId, limit);
    }

    public void deleteAnalysis(String analysisId, PlantRepository.RepositoryCallback<Void> callback) {
        repository.deleteAnalysis(analysisId, callback);
    }

    public LiveData<List<CareCompletion>> getAllCompletionsForPlant(String plantId) {
        return repository.getAllCompletionsForPlant(plantId);
    }

    public void deleteCareCompletion(String completionId, String scheduleId, PlantRepository.RepositoryCallback<Void> callback) {
        // Delete the completion
        repository.deleteCareCompletion(completionId, new PlantRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // After deletion, recalculate schedule's nextDue
                new Thread(() -> {
                    try {
                        careScheduleManager.recalculateNextDue(scheduleId);
                        callback.onSuccess(null);
                    } catch (Exception e) {
                        callback.onError(e);
                    }
                }).start();
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }
}
