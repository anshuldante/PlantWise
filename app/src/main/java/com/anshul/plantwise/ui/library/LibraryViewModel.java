package com.anshul.plantwise.ui.library;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.anshul.plantwise.data.db.AppDatabase;
import com.anshul.plantwise.data.db.PlantDao;
import com.anshul.plantwise.data.entity.Plant;

import java.util.List;

public class LibraryViewModel extends AndroidViewModel {

    private final PlantDao plantDao;
    private final LiveData<List<Plant>> allPlants;

    public LibraryViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        plantDao = db.plantDao();
        allPlants = plantDao.getAllPlants();
    }

    public LiveData<List<Plant>> getAllPlants() {
        return allPlants;
    }
}
