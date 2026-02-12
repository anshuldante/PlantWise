package com.leafiq.app.util;

import static com.google.common.truth.Truth.assertThat;

import com.leafiq.app.data.model.PlantAnalysisResult;

import org.json.JSONException;
import org.junit.Test;

public class JsonParserTest {

    @Test
    public void parsePlantAnalysis_withValidJson_parsesIdentification() throws JSONException {
        String json = "{"
            + "\"identification\": {"
            + "  \"commonName\": \"Monstera Deliciosa\","
            + "  \"scientificName\": \"Monstera deliciosa\","
            + "  \"confidence\": \"high\","
            + "  \"notes\": \"A popular houseplant\""
            + "},"
            + "\"healthAssessment\": {"
            + "  \"score\": 8,"
            + "  \"summary\": \"Healthy plant\","
            + "  \"issues\": []"
            + "},"
            + "\"immediateActions\": [],"
            + "\"carePlan\": {"
            + "  \"watering\": {\"frequency\": \"Weekly\", \"amount\": \"Moderate\", \"notes\": \"Let soil dry\"},"
            + "  \"light\": {\"ideal\": \"Bright indirect\", \"current\": \"Good\", \"adjustment\": null},"
            + "  \"fertilizer\": {\"type\": \"Balanced\", \"frequency\": \"Monthly\", \"nextApplication\": \"Spring\"},"
            + "  \"pruning\": {\"needed\": false, \"instructions\": \"\", \"when\": \"\"},"
            + "  \"repotting\": {\"needed\": false, \"signs\": \"\", \"recommendedPotSize\": null},"
            + "  \"seasonal\": \"Reduce watering in winter\""
            + "},"
            + "\"funFact\": \"Can grow up to 65 feet tall\""
            + "}";

        PlantAnalysisResult result = JsonParser.parsePlantAnalysis(json);

        assertThat(result.identification).isNotNull();
        assertThat(result.identification.commonName).isEqualTo("Monstera Deliciosa");
        assertThat(result.identification.scientificName).isEqualTo("Monstera deliciosa");
        assertThat(result.identification.confidence).isEqualTo("high");
        assertThat(result.identification.notes).isEqualTo("A popular houseplant");
    }

    @Test
    public void parsePlantAnalysis_withValidJson_parsesHealthAssessment() throws JSONException {
        String json = "{"
            + "\"identification\": {"
            + "  \"commonName\": \"Snake Plant\","
            + "  \"scientificName\": \"Sansevieria\","
            + "  \"confidence\": \"medium\","
            + "  \"notes\": \"\""
            + "},"
            + "\"healthAssessment\": {"
            + "  \"score\": 6,"
            + "  \"summary\": \"Some issues detected\","
            + "  \"issues\": ["
            + "    {\"name\": \"Yellow leaves\", \"severity\": \"medium\", \"description\": \"Possible overwatering\", \"affectedArea\": \"Lower leaves\"},"
            + "    {\"name\": \"Brown tips\", \"severity\": \"low\", \"description\": \"Low humidity\", \"affectedArea\": \"Leaf tips\"}"
            + "  ]"
            + "},"
            + "\"immediateActions\": [],"
            + "\"carePlan\": {},"
            + "\"funFact\": \"\""
            + "}";

        PlantAnalysisResult result = JsonParser.parsePlantAnalysis(json);

        assertThat(result.healthAssessment).isNotNull();
        assertThat(result.healthAssessment.score).isEqualTo(6);
        assertThat(result.healthAssessment.summary).isEqualTo("Some issues detected");
        assertThat(result.healthAssessment.issues).hasSize(2);
        assertThat(result.healthAssessment.issues.get(0).name).isEqualTo("Yellow leaves");
        assertThat(result.healthAssessment.issues.get(0).severity).isEqualTo("medium");
        assertThat(result.healthAssessment.issues.get(1).name).isEqualTo("Brown tips");
    }

    @Test
    public void parsePlantAnalysis_withValidJson_parsesImmediateActions() throws JSONException {
        String json = "{"
            + "\"identification\": {\"commonName\": \"Fern\", \"scientificName\": \"Nephrolepis\", \"confidence\": \"high\", \"notes\": \"\"},"
            + "\"healthAssessment\": {\"score\": 4, \"summary\": \"Needs attention\", \"issues\": []},"
            + "\"immediateActions\": ["
            + "  {\"action\": \"Water immediately\", \"priority\": \"urgent\", \"detail\": \"Soil is very dry\"},"
            + "  {\"action\": \"Move to humid area\", \"priority\": \"soon\", \"detail\": \"Increase humidity\"}"
            + "],"
            + "\"carePlan\": {},"
            + "\"funFact\": \"\""
            + "}";

        PlantAnalysisResult result = JsonParser.parsePlantAnalysis(json);

        assertThat(result.immediateActions).hasSize(2);
        assertThat(result.immediateActions.get(0).action).isEqualTo("Water immediately");
        assertThat(result.immediateActions.get(0).priority).isEqualTo("urgent");
        assertThat(result.immediateActions.get(1).priority).isEqualTo("soon");
    }

    @Test
    public void parsePlantAnalysis_withValidJson_parsesCarePlan() throws JSONException {
        String json = "{"
            + "\"identification\": {\"commonName\": \"Pothos\", \"scientificName\": \"Epipremnum aureum\", \"confidence\": \"high\", \"notes\": \"\"},"
            + "\"healthAssessment\": {\"score\": 9, \"summary\": \"Excellent health\", \"issues\": []},"
            + "\"immediateActions\": [],"
            + "\"carePlan\": {"
            + "  \"watering\": {\"frequency\": \"Every 7-10 days\", \"amount\": \"Until water drains\", \"notes\": \"Let top inch dry first\"},"
            + "  \"light\": {\"ideal\": \"Medium indirect light\", \"current\": \"Adequate\", \"adjustment\": \"Could use more light\"},"
            + "  \"fertilizer\": {\"type\": \"20-20-20\", \"frequency\": \"Monthly during growing season\", \"nextApplication\": \"March\"},"
            + "  \"pruning\": {\"needed\": true, \"instructions\": \"Trim leggy vines\", \"when\": \"Spring\"},"
            + "  \"repotting\": {\"needed\": true, \"signs\": \"Roots visible at drainage holes\", \"recommendedPotSize\": \"8 inch\"},"
            + "  \"seasonal\": \"Slow growth in winter\""
            + "},"
            + "\"funFact\": \"NASA study showed it removes toxins\""
            + "}";

        PlantAnalysisResult result = JsonParser.parsePlantAnalysis(json);

        assertThat(result.carePlan).isNotNull();
        assertThat(result.carePlan.watering.frequency).isEqualTo("Every 7-10 days");
        assertThat(result.carePlan.light.adjustment).isEqualTo("Could use more light");
        assertThat(result.carePlan.pruning.needed).isTrue();
        assertThat(result.carePlan.repotting.needed).isTrue();
        assertThat(result.carePlan.repotting.recommendedPotSize).isEqualTo("8 inch");
        assertThat(result.funFact).isEqualTo("NASA study showed it removes toxins");
    }

    @Test
    public void parsePlantAnalysis_withMissingFields_usesDefaults() throws JSONException {
        String json = "{"
            + "\"identification\": {\"commonName\": \"Unknown Plant\"},"
            + "\"healthAssessment\": {\"score\": 5}"
            + "}";

        PlantAnalysisResult result = JsonParser.parsePlantAnalysis(json);

        assertThat(result.identification.commonName).isEqualTo("Unknown Plant");
        assertThat(result.identification.scientificName).isEmpty();
        assertThat(result.identification.confidence).isEqualTo("low");
        assertThat(result.healthAssessment.score).isEqualTo(5);
        assertThat(result.healthAssessment.summary).isEmpty();
        assertThat(result.immediateActions).isEmpty();
    }

    @Test
    public void parsePlantAnalysis_withEmptyIssues_returnsEmptyList() throws JSONException {
        String json = "{"
            + "\"identification\": {\"commonName\": \"Cactus\"},"
            + "\"healthAssessment\": {\"score\": 10, \"summary\": \"Perfect\", \"issues\": []}"
            + "}";

        PlantAnalysisResult result = JsonParser.parsePlantAnalysis(json);

        assertThat(result.healthAssessment.issues).isEmpty();
    }

    @Test(expected = JSONException.class)
    public void parsePlantAnalysis_withInvalidJson_throwsException() throws JSONException {
        String json = "not valid json";
        JsonParser.parsePlantAnalysis(json);
    }

    // ==================== Boolean edge case tests (09-05 - BUG-18) ====================

    @Test
    public void parsePlantAnalysis_pruningNeeded_booleanTrue() throws Exception {
        String json = "{\"carePlan\":{\"pruning\":{\"needed\":true,\"instructions\":\"trim\",\"when\":\"spring\"}}}";
        PlantAnalysisResult result = JsonParser.parsePlantAnalysis(json);
        assertThat(result.carePlan.pruning.needed).isTrue();
    }

    @Test
    public void parsePlantAnalysis_pruningNeeded_stringTrue() throws Exception {
        String json = "{\"carePlan\":{\"pruning\":{\"needed\":\"true\",\"instructions\":\"trim\",\"when\":\"spring\"}}}";
        PlantAnalysisResult result = JsonParser.parsePlantAnalysis(json);
        assertThat(result.carePlan.pruning.needed).isTrue();
    }

    @Test
    public void parsePlantAnalysis_pruningNeeded_stringFalse() throws Exception {
        String json = "{\"carePlan\":{\"pruning\":{\"needed\":\"false\",\"instructions\":\"\",\"when\":\"\"}}}";
        PlantAnalysisResult result = JsonParser.parsePlantAnalysis(json);
        assertThat(result.carePlan.pruning.needed).isFalse();
    }

    @Test
    public void parsePlantAnalysis_repottingNeeded_stringTrue() throws Exception {
        String json = "{\"carePlan\":{\"repotting\":{\"needed\":\"true\",\"signs\":\"roots visible\"}}}";
        PlantAnalysisResult result = JsonParser.parsePlantAnalysis(json);
        assertThat(result.carePlan.repotting.needed).isTrue();
    }

    @Test
    public void parsePlantAnalysis_pruningNeeded_missing_defaultsFalse() throws Exception {
        String json = "{\"carePlan\":{\"pruning\":{\"instructions\":\"none\"}}}";
        PlantAnalysisResult result = JsonParser.parsePlantAnalysis(json);
        assertThat(result.carePlan.pruning.needed).isFalse();
    }
}
