package com.leafiq.app.ai;

import com.leafiq.app.data.model.PlantAnalysisResult;

public interface AIProvider {
    PlantAnalysisResult analyzePhoto(String imageBase64, String prompt) throws AIProviderException;
    boolean isConfigured();
    String getDisplayName();
    boolean supportsVision();
}
