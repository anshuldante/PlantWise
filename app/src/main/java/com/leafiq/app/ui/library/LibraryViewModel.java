package com.leafiq.app.ui.library;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.leafiq.app.LeafIQApplication;
import com.leafiq.app.data.entity.Plant;
import com.leafiq.app.data.repository.PlantRepository;

import java.util.List;

public class LibraryViewModel extends AndroidViewModel {

    private final PlantRepository repository;
    private final LiveData<List<Plant>> allPlants;

    public LibraryViewModel(@NonNull Application application) {
        super(application);
        repository = ((LeafIQApplication) application).getPlantRepository();
        allPlants = repository.getAllPlants();
    }

    public LiveData<List<Plant>> getAllPlants() {
        return allPlants;
    }

    public void deletePlant(Plant plant, PlantRepository.RepositoryCallback<Void> callback) {
        repository.deletePlant(plant, callback);
    }
}
