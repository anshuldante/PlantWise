package com.leafiq.app.ui.detail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.leafiq.app.LeafIQApplication;
import com.leafiq.app.data.entity.Analysis;
import com.leafiq.app.data.entity.Plant;
import com.leafiq.app.data.repository.PlantRepository;

import java.util.List;

public class PlantDetailViewModel extends AndroidViewModel {

    private final PlantRepository repository;

    public PlantDetailViewModel(@NonNull Application application) {
        super(application);
        repository = ((LeafIQApplication) application).getPlantRepository();
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
}
