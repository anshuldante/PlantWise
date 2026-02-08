package com.leafiq.app.domain.service;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leafiq.app.ai.AIProvider;
import com.leafiq.app.ai.AIProviderException;
import com.leafiq.app.data.entity.Analysis;
import com.leafiq.app.data.model.PlantAnalysisResult;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AIAnalysisServiceTest {

    private AIAnalysisService service;
    private AIProvider mockProvider;

    @Before
    public void setUp() {
        service = new AIAnalysisService();
        mockProvider = mock(AIProvider.class);
    }

    @Test
    public void supportsVision_delegatesToProvider_true() {
        when(mockProvider.supportsVision()).thenReturn(true);
        assertThat(service.supportsVision(mockProvider)).isTrue();
    }

    @Test
    public void supportsVision_delegatesToProvider_false() {
        when(mockProvider.supportsVision()).thenReturn(false);
        assertThat(service.supportsVision(mockProvider)).isFalse();
    }

    @Test
    public void analyze_callsProviderWithBase64ImageAndPrompt() throws AIProviderException, IOException {
        PlantAnalysisResult expectedResult = new PlantAnalysisResult();
        when(mockProvider.analyzePhoto(anyString(), anyString())).thenReturn(expectedResult);

        PlantAnalysisResult result = service.analyze(mockProvider, "base64data", null, null);

        assertThat(result).isSameInstanceAs(expectedResult);
        verify(mockProvider).analyzePhoto(anyString(), anyString());
    }

    @Test
    public void analyze_withKnownPlantName_callsProvider() throws AIProviderException, IOException {
        PlantAnalysisResult expectedResult = new PlantAnalysisResult();
        when(mockProvider.analyzePhoto(anyString(), anyString())).thenReturn(expectedResult);

        PlantAnalysisResult result = service.analyze(mockProvider, "base64data", "Rose", null);

        assertThat(result).isSameInstanceAs(expectedResult);
    }

    @Test
    public void analyze_withPreviousAnalyses_callsProvider() throws AIProviderException, IOException {
        PlantAnalysisResult expectedResult = new PlantAnalysisResult();
        when(mockProvider.analyzePhoto(anyString(), anyString())).thenReturn(expectedResult);

        List<Analysis> previousAnalyses = new ArrayList<>();
        Analysis prev = new Analysis();
        prev.summary = "Previous analysis";
        previousAnalyses.add(prev);

        PlantAnalysisResult result = service.analyze(mockProvider, "base64data", "Rose", previousAnalyses);

        assertThat(result).isSameInstanceAs(expectedResult);
    }

    @Test(expected = AIProviderException.class)
    public void analyze_providerThrowsException_propagates() throws AIProviderException, IOException {
        when(mockProvider.analyzePhoto(anyString(), anyString()))
                .thenThrow(new AIProviderException("API error"));

        service.analyze(mockProvider, "base64data", null, null);
    }

    @Test
    public void analyze_withNullPlantNameAndNullAnalyses_succeeds() throws AIProviderException, IOException {
        PlantAnalysisResult expectedResult = new PlantAnalysisResult();
        when(mockProvider.analyzePhoto(anyString(), anyString())).thenReturn(expectedResult);

        PlantAnalysisResult result = service.analyze(mockProvider, "base64data", null, null);

        assertThat(result).isNotNull();
    }
}
