package com.leafiq.app.domain.usecase;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.net.Uri;

import com.leafiq.app.ai.AIProvider;
import com.leafiq.app.ai.AIProviderException;
import com.leafiq.app.data.entity.Analysis;
import com.leafiq.app.data.entity.Plant;
import com.leafiq.app.data.model.PlantAnalysisResult;
import com.leafiq.app.data.repository.PlantRepository;
import com.leafiq.app.domain.service.AIAnalysisService;
import com.leafiq.app.domain.service.ImagePreprocessor;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class AnalyzePlantUseCaseTest {

    private ImagePreprocessor mockPreprocessor;
    private AIAnalysisService mockAnalysisService;
    private PlantRepository mockRepository;
    private AIProvider mockProvider;
    private Uri mockUri;

    // Synchronous executor for tests - runs immediately on calling thread
    private final Executor synchronousExecutor = Runnable::run;

    private AnalyzePlantUseCase useCase;

    @Before
    public void setUp() {
        mockPreprocessor = mock(ImagePreprocessor.class);
        mockAnalysisService = mock(AIAnalysisService.class);
        mockRepository = mock(PlantRepository.class);
        mockProvider = mock(AIProvider.class);
        mockUri = mock(Uri.class);

        useCase = new AnalyzePlantUseCase(
                mockPreprocessor, mockAnalysisService, mockRepository, synchronousExecutor);
    }

    @Test
    public void execute_visionNotSupported_callsOnVisionNotSupported() {
        when(mockAnalysisService.supportsVision(mockProvider)).thenReturn(false);
        when(mockProvider.getDisplayName()).thenReturn("Perplexity");

        AtomicReference<String> capturedProvider = new AtomicReference<>();
        AnalyzePlantUseCase.Callback callback = new AnalyzePlantUseCase.Callback() {
            @Override
            public void onSuccess(PlantAnalysisResult result) {}

            @Override
            public void onError(String message) {}

            @Override
            public void onVisionNotSupported(String providerDisplayName) {
                capturedProvider.set(providerDisplayName);
            }
        };

        useCase.execute(mockUri, null, mockProvider, callback);

        assertThat(capturedProvider.get()).isEqualTo("Perplexity");
    }

    @Test
    public void execute_visionNotSupported_doesNotCallPreprocessor() throws IOException {
        when(mockAnalysisService.supportsVision(mockProvider)).thenReturn(false);
        when(mockProvider.getDisplayName()).thenReturn("Perplexity");

        AnalyzePlantUseCase.Callback callback = mock(AnalyzePlantUseCase.Callback.class);
        useCase.execute(mockUri, null, mockProvider, callback);

        verify(mockPreprocessor, never()).prepareForApi(any());
    }

    @Test
    public void execute_newPlant_callsOnSuccessWithResult() throws IOException, AIProviderException {
        when(mockAnalysisService.supportsVision(mockProvider)).thenReturn(true);
        when(mockPreprocessor.prepareForApi(mockUri)).thenReturn("base64data");

        PlantAnalysisResult expectedResult = new PlantAnalysisResult();
        expectedResult.funFact = "Test fact";
        when(mockAnalysisService.analyze(eq(mockProvider), eq("base64data"), isNull(), isNull(), isNull()))
                .thenReturn(expectedResult);

        AtomicReference<PlantAnalysisResult> capturedResult = new AtomicReference<>();
        AnalyzePlantUseCase.Callback callback = new AnalyzePlantUseCase.Callback() {
            @Override
            public void onSuccess(PlantAnalysisResult result) {
                capturedResult.set(result);
            }

            @Override
            public void onError(String message) {}

            @Override
            public void onVisionNotSupported(String providerDisplayName) {}
        };

        useCase.execute(mockUri, null, mockProvider, callback);

        assertThat(capturedResult.get()).isSameInstanceAs(expectedResult);
    }

    @Test
    public void execute_newPlant_doesNotQueryRepository() throws IOException, AIProviderException {
        when(mockAnalysisService.supportsVision(mockProvider)).thenReturn(true);
        when(mockPreprocessor.prepareForApi(mockUri)).thenReturn("base64data");
        when(mockAnalysisService.analyze(any(), anyString(), isNull(), isNull(), isNull()))
                .thenReturn(new PlantAnalysisResult());

        AnalyzePlantUseCase.Callback callback = mock(AnalyzePlantUseCase.Callback.class);
        useCase.execute(mockUri, null, mockProvider, callback);

        verify(mockRepository, never()).getPlantByIdSync(anyString());
        verify(mockRepository, never()).getRecentAnalysesSync(anyString());
    }

    @Test
    public void execute_existingPlant_loadsContextFromRepository() throws IOException, AIProviderException {
        when(mockAnalysisService.supportsVision(mockProvider)).thenReturn(true);
        when(mockPreprocessor.prepareForApi(mockUri)).thenReturn("base64data");

        Plant existingPlant = new Plant();
        existingPlant.commonName = "Rose";
        existingPlant.location = null;
        when(mockRepository.getPlantByIdSync("plant-123")).thenReturn(existingPlant);

        List<Analysis> previousAnalyses = new ArrayList<>();
        when(mockRepository.getRecentAnalysesSync("plant-123")).thenReturn(previousAnalyses);

        when(mockAnalysisService.analyze(eq(mockProvider), eq("base64data"), eq("Rose"), eq(previousAnalyses), isNull()))
                .thenReturn(new PlantAnalysisResult());

        AnalyzePlantUseCase.Callback callback = mock(AnalyzePlantUseCase.Callback.class);
        useCase.execute(mockUri, "plant-123", mockProvider, callback);

        verify(mockRepository).getPlantByIdSync("plant-123");
        verify(mockRepository).getRecentAnalysesSync("plant-123");
        verify(mockAnalysisService).analyze(eq(mockProvider), eq("base64data"), eq("Rose"), eq(previousAnalyses), isNull());
    }

    @Test
    public void execute_existingPlant_nullPlantRecord_passesNullName() throws IOException, AIProviderException {
        when(mockAnalysisService.supportsVision(mockProvider)).thenReturn(true);
        when(mockPreprocessor.prepareForApi(mockUri)).thenReturn("base64data");
        when(mockRepository.getPlantByIdSync("plant-456")).thenReturn(null);
        when(mockRepository.getRecentAnalysesSync("plant-456")).thenReturn(new ArrayList<>());
        when(mockAnalysisService.analyze(any(), anyString(), isNull(), any(), isNull()))
                .thenReturn(new PlantAnalysisResult());

        AnalyzePlantUseCase.Callback callback = mock(AnalyzePlantUseCase.Callback.class);
        useCase.execute(mockUri, "plant-456", mockProvider, callback);

        verify(mockAnalysisService).analyze(eq(mockProvider), eq("base64data"), isNull(), any(), isNull());
    }

    @Test
    public void execute_ioException_callsOnError() throws IOException {
        when(mockAnalysisService.supportsVision(mockProvider)).thenReturn(true);
        when(mockPreprocessor.prepareForApi(mockUri)).thenThrow(new IOException("File not found"));

        AtomicReference<String> capturedError = new AtomicReference<>();
        AnalyzePlantUseCase.Callback callback = new AnalyzePlantUseCase.Callback() {
            @Override
            public void onSuccess(PlantAnalysisResult result) {}

            @Override
            public void onError(String message) {
                capturedError.set(message);
            }

            @Override
            public void onVisionNotSupported(String providerDisplayName) {}
        };

        useCase.execute(mockUri, null, mockProvider, callback);

        assertThat(capturedError.get()).contains("Failed to process image");
        assertThat(capturedError.get()).contains("File not found");
    }

    @Test
    public void execute_aiProviderException_callsOnError() throws IOException, AIProviderException {
        when(mockAnalysisService.supportsVision(mockProvider)).thenReturn(true);
        when(mockPreprocessor.prepareForApi(mockUri)).thenReturn("base64data");
        when(mockAnalysisService.analyze(any(), anyString(), isNull(), isNull(), isNull()))
                .thenThrow(new AIProviderException("Rate limited"));

        AtomicReference<String> capturedError = new AtomicReference<>();
        AnalyzePlantUseCase.Callback callback = new AnalyzePlantUseCase.Callback() {
            @Override
            public void onSuccess(PlantAnalysisResult result) {}

            @Override
            public void onError(String message) {
                capturedError.set(message);
            }

            @Override
            public void onVisionNotSupported(String providerDisplayName) {}
        };

        useCase.execute(mockUri, null, mockProvider, callback);

        assertThat(capturedError.get()).contains("Analysis failed");
        assertThat(capturedError.get()).contains("Rate limited");
    }

    @Test
    public void execute_runsOnProvidedExecutor() {
        AtomicBoolean executorUsed = new AtomicBoolean(false);
        Executor trackingExecutor = runnable -> {
            executorUsed.set(true);
            runnable.run();
        };

        AnalyzePlantUseCase trackingUseCase = new AnalyzePlantUseCase(
                mockPreprocessor, mockAnalysisService, mockRepository, trackingExecutor);

        when(mockAnalysisService.supportsVision(mockProvider)).thenReturn(false);
        when(mockProvider.getDisplayName()).thenReturn("Test");

        AnalyzePlantUseCase.Callback callback = mock(AnalyzePlantUseCase.Callback.class);
        trackingUseCase.execute(mockUri, null, mockProvider, callback);

        assertThat(executorUsed.get()).isTrue();
    }

    // ==================== execute with location ====================

    @Test
    public void execute_existingPlant_passesLocationToService() throws IOException, AIProviderException {
        when(mockAnalysisService.supportsVision(mockProvider)).thenReturn(true);
        when(mockPreprocessor.prepareForApi(mockUri)).thenReturn("base64data");

        Plant existingPlant = new Plant();
        existingPlant.commonName = "Fern";
        existingPlant.location = "Bathroom shelf";
        when(mockRepository.getPlantByIdSync("plant-loc")).thenReturn(existingPlant);
        when(mockRepository.getRecentAnalysesSync("plant-loc")).thenReturn(new ArrayList<>());

        when(mockAnalysisService.analyze(
                eq(mockProvider), eq("base64data"), eq("Fern"), any(), eq("Bathroom shelf")))
                .thenReturn(new PlantAnalysisResult());

        AnalyzePlantUseCase.Callback callback = mock(AnalyzePlantUseCase.Callback.class);
        useCase.execute(mockUri, "plant-loc", mockProvider, callback);

        verify(mockAnalysisService).analyze(
                eq(mockProvider), eq("base64data"), eq("Fern"), any(), eq("Bathroom shelf"));
    }

    // ==================== executeWithCorrections ====================

    @Test
    public void executeWithCorrections_visionNotSupported_callsOnVisionNotSupported() {
        when(mockAnalysisService.supportsVision(mockProvider)).thenReturn(false);
        when(mockProvider.getDisplayName()).thenReturn("TextOnly");

        AtomicReference<String> capturedProvider = new AtomicReference<>();
        AnalyzePlantUseCase.Callback callback = new AnalyzePlantUseCase.Callback() {
            @Override public void onSuccess(PlantAnalysisResult result) {}
            @Override public void onError(String message) {}
            @Override public void onVisionNotSupported(String providerDisplayName) {
                capturedProvider.set(providerDisplayName);
            }
        };

        useCase.executeWithCorrections(mockUri, null, "Rose", "Context", mockProvider, callback);

        assertThat(capturedProvider.get()).isEqualTo("TextOnly");
    }

    @Test
    public void executeWithCorrections_newPlant_callsOnSuccess() throws IOException, AIProviderException {
        when(mockAnalysisService.supportsVision(mockProvider)).thenReturn(true);
        when(mockPreprocessor.prepareForApi(mockUri)).thenReturn("base64data");

        PlantAnalysisResult expectedResult = new PlantAnalysisResult();
        when(mockAnalysisService.analyzeWithCorrections(
                eq(mockProvider), eq("base64data"), eq("Monstera"), eq("Yellow leaves"),
                isNull(), isNull()))
                .thenReturn(expectedResult);

        AtomicReference<PlantAnalysisResult> capturedResult = new AtomicReference<>();
        AnalyzePlantUseCase.Callback callback = new AnalyzePlantUseCase.Callback() {
            @Override public void onSuccess(PlantAnalysisResult result) { capturedResult.set(result); }
            @Override public void onError(String message) {}
            @Override public void onVisionNotSupported(String providerDisplayName) {}
        };

        useCase.executeWithCorrections(mockUri, null, "Monstera", "Yellow leaves", mockProvider, callback);

        assertThat(capturedResult.get()).isSameInstanceAs(expectedResult);
    }

    @Test
    public void executeWithCorrections_existingPlant_loadsLocationFromRepository() throws IOException, AIProviderException {
        when(mockAnalysisService.supportsVision(mockProvider)).thenReturn(true);
        when(mockPreprocessor.prepareForApi(mockUri)).thenReturn("base64data");

        Plant existingPlant = new Plant();
        existingPlant.location = "Office desk";
        when(mockRepository.getPlantByIdSync("plant-corr")).thenReturn(existingPlant);
        when(mockRepository.getRecentAnalysesSync("plant-corr")).thenReturn(new ArrayList<>());

        when(mockAnalysisService.analyzeWithCorrections(
                eq(mockProvider), eq("base64data"), eq("Cactus"), isNull(),
                any(), eq("Office desk")))
                .thenReturn(new PlantAnalysisResult());

        AnalyzePlantUseCase.Callback callback = mock(AnalyzePlantUseCase.Callback.class);
        useCase.executeWithCorrections(mockUri, "plant-corr", "Cactus", null, mockProvider, callback);

        verify(mockAnalysisService).analyzeWithCorrections(
                eq(mockProvider), eq("base64data"), eq("Cactus"), isNull(),
                any(), eq("Office desk"));
    }

    @Test
    public void executeWithCorrections_passesCorrectionsToService() throws IOException, AIProviderException {
        when(mockAnalysisService.supportsVision(mockProvider)).thenReturn(true);
        when(mockPreprocessor.prepareForApi(mockUri)).thenReturn("base64data");

        when(mockAnalysisService.analyzeWithCorrections(
                any(), anyString(), eq("Aloe Vera"), eq("Recently watered"),
                isNull(), isNull()))
                .thenReturn(new PlantAnalysisResult());

        AnalyzePlantUseCase.Callback callback = mock(AnalyzePlantUseCase.Callback.class);
        useCase.executeWithCorrections(mockUri, null, "Aloe Vera", "Recently watered", mockProvider, callback);

        verify(mockAnalysisService).analyzeWithCorrections(
                eq(mockProvider), eq("base64data"), eq("Aloe Vera"), eq("Recently watered"),
                isNull(), isNull());
    }

    @Test
    public void executeWithCorrections_ioException_callsOnError() throws IOException {
        when(mockAnalysisService.supportsVision(mockProvider)).thenReturn(true);
        when(mockPreprocessor.prepareForApi(mockUri)).thenThrow(new IOException("Cannot read"));

        AtomicReference<String> capturedError = new AtomicReference<>();
        AnalyzePlantUseCase.Callback callback = new AnalyzePlantUseCase.Callback() {
            @Override public void onSuccess(PlantAnalysisResult result) {}
            @Override public void onError(String message) { capturedError.set(message); }
            @Override public void onVisionNotSupported(String providerDisplayName) {}
        };

        useCase.executeWithCorrections(mockUri, null, "Rose", null, mockProvider, callback);

        assertThat(capturedError.get()).contains("Failed to process image");
    }

    @Test
    public void executeWithCorrections_aiProviderException_callsOnError() throws IOException, AIProviderException {
        when(mockAnalysisService.supportsVision(mockProvider)).thenReturn(true);
        when(mockPreprocessor.prepareForApi(mockUri)).thenReturn("base64data");
        when(mockAnalysisService.analyzeWithCorrections(
                any(), anyString(), any(), any(), any(), any()))
                .thenThrow(new AIProviderException("Provider down"));

        AtomicReference<String> capturedError = new AtomicReference<>();
        AnalyzePlantUseCase.Callback callback = new AnalyzePlantUseCase.Callback() {
            @Override public void onSuccess(PlantAnalysisResult result) {}
            @Override public void onError(String message) { capturedError.set(message); }
            @Override public void onVisionNotSupported(String providerDisplayName) {}
        };

        useCase.executeWithCorrections(mockUri, null, "Rose", null, mockProvider, callback);

        assertThat(capturedError.get()).contains("Re-analysis failed");
        assertThat(capturedError.get()).contains("Provider down");
    }
}
