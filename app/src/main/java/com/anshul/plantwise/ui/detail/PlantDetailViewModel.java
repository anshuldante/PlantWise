package com.anshul.plantwise.ui.detail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.anshul.plantwise.data.db.AppDatabase;
import com.anshul.plantwise.data.db.AnalysisDao;
import com.anshul.plantwise.data.db.PlantDao;
import com.anshul.plantwise.data.entity.Analysis;
import com.anshul.plantwise.data.entity.Plant;

import java.util.List;

public class PlantDetailViewModel extends AndroidViewModel {

    private final PlantDao plantDao;
    private final AnalysisDao analysisDao;

    public PlantDetailViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        plantDao = db.plantDao();
        analysisDao = db.analysisDao();
    }

    public LiveData<Plant> getPlant(String plantId) {
        return plantDao.getPlantById(plantId);
    }

    public LiveData<List<Analysis>> getAnalyses(String plantId) {
        return analysisDao.getAnalysesForPlant(plantId);
    }
}
