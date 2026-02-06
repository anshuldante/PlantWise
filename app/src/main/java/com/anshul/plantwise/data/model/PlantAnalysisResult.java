package com.anshul.plantwise.data.model;

import java.util.List;

public class PlantAnalysisResult {
    public Identification identification;
    public HealthAssessment healthAssessment;
    public List<ImmediateAction> immediateActions;
    public CarePlan carePlan;
    public String funFact;

    public static class Identification {
        public String commonName;
        public String scientificName;
        public String confidence;  // "high", "medium", "low"
        public String notes;
    }

    public static class HealthAssessment {
        public int score;  // 1-10
        public String summary;
        public List<Issue> issues;

        public static class Issue {
            public String name;
            public String severity;  // "low", "medium", "high"
            public String description;
            public String affectedArea;
        }
    }

    public static class ImmediateAction {
        public String action;
        public String priority;  // "urgent", "soon", "when_convenient"
        public String detail;
    }

    public static class CarePlan {
        public Watering watering;
        public Light light;
        public Fertilizer fertilizer;
        public Pruning pruning;
        public Repotting repotting;
        public String seasonal;

        public static class Watering {
            public String frequency;
            public String amount;
            public String notes;
        }

        public static class Light {
            public String ideal;
            public String current;
            public String adjustment;
        }

        public static class Fertilizer {
            public String type;
            public String frequency;
            public String nextApplication;
        }

        public static class Pruning {
            public boolean needed;
            public String instructions;
            public String when;
        }

        public static class Repotting {
            public boolean needed;
            public String signs;
            public String recommendedPotSize;
        }
    }
}
