package com.leafiq.app.ui.analysis;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.leafiq.app.LeafIQApplication;
import com.leafiq.app.data.repository.PlantRepository;
import com.leafiq.app.domain.service.AIAnalysisService;
import com.leafiq.app.domain.service.ImagePreprocessor;
import com.leafiq.app.domain.usecase.AnalyzePlantUseCase;
import com.leafiq.app.util.AppExecutors;
import com.leafiq.app.util.KeystoreHelper;

/**
 * Factory for creating AnalysisViewModel with dependency injection.
 * Assembles the full dependency graph: UseCase -> Services -> Repository -> Executors.
 * <p>
 * Usage in Activity:
 * <pre>
 * viewModel = new ViewModelProvider(this,
 *     new AnalysisViewModelFactory(getApplication()))
 *     .get(AnalysisViewModel.class);
 * </pre>
 */
public class AnalysisViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;

    /**
     * Creates a factory with the application instance.
     *
     * @param application Application instance (for accessing LeafIQApplication dependencies)
     */
    public AnalysisViewModelFactory(Application application) {
        this.application = application;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(AnalysisViewModel.class)) {
            // Get shared dependencies from Application
            LeafIQApplication app = (LeafIQApplication) application;
            AppExecutors appExecutors = app.getAppExecutors();
            PlantRepository plantRepository = app.getPlantRepository();

            // Create domain services
            ImagePreprocessor imagePreprocessor = new ImagePreprocessor(application);
            AIAnalysisService aiAnalysisService = new AIAnalysisService();

            // Create use case with all dependencies
            AnalyzePlantUseCase analyzePlantUseCase = new AnalyzePlantUseCase(
                    application,  // Context for network check
                    imagePreprocessor,
                    aiAnalysisService,
                    plantRepository,
                    appExecutors.network()
            );

            // Create KeystoreHelper for API key management
            KeystoreHelper keystoreHelper = new KeystoreHelper(application);

            // Get CareScheduleManager from Application
            com.leafiq.app.care.CareScheduleManager careScheduleManager = app.getCareScheduleManager();

            // Create ViewModel with all dependencies
            return (T) new AnalysisViewModel(
                    application,
                    analyzePlantUseCase,
                    plantRepository,
                    imagePreprocessor,
                    keystoreHelper,
                    careScheduleManager
            );
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
