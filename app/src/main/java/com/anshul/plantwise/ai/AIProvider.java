package com.anshul.plantwise.ai;

import com.anshul.plantwise.data.model.PlantAnalysisResult;

public interface AIProvider {
    PlantAnalysisResult analyzePhoto(String imageBase64, String prompt) throws AIProviderException;
    boolean isConfigured();
    String getDisplayName();
}
