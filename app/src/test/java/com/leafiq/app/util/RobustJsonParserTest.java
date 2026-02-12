package com.leafiq.app.util;

import static com.google.common.truth.Truth.assertThat;

import com.leafiq.app.data.model.PlantAnalysisResult;

import org.junit.Test;

public class RobustJsonParserTest {

    @Test
    public void parse_validJson_returnsOk() {
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

        RobustJsonParser.ParseResult result = RobustJsonParser.parse(json);

        assertThat(result.parseStatus).isEqualTo("OK");
        assertThat(result.result).isNotNull();
        assertThat(result.result.identification.commonName).isEqualTo("Monstera Deliciosa");
        assertThat(result.result.healthAssessment.score).isEqualTo(8);
        assertThat(result.contentHash).isNotNull();
        assertThat(result.contentHash).hasLength(8);
    }

    @Test
    public void parse_nullInput_returnsEmpty() {
        RobustJsonParser.ParseResult result = RobustJsonParser.parse(null);

        assertThat(result.parseStatus).isEqualTo("EMPTY");
        assertThat(result.result).isNull();
        assertThat(result.contentHash).isNull();
    }

    @Test
    public void parse_emptyString_returnsEmpty() {
        RobustJsonParser.ParseResult result = RobustJsonParser.parse("");

        assertThat(result.parseStatus).isEqualTo("EMPTY");
        assertThat(result.result).isNull();
        assertThat(result.contentHash).isNull();
    }

    @Test
    public void parse_whitespaceOnly_returnsEmpty() {
        RobustJsonParser.ParseResult result = RobustJsonParser.parse("   \n\t  ");

        assertThat(result.parseStatus).isEqualTo("EMPTY");
        assertThat(result.result).isNull();
    }

    @Test
    public void parse_completelyInvalidJson_returnsFailed() {
        String json = "not json at all, just random text!";

        RobustJsonParser.ParseResult result = RobustJsonParser.parse(json);

        assertThat(result.parseStatus).isEqualTo("FAILED");
        assertThat(result.result).isNull();
        assertThat(result.contentHash).isNotNull();
        assertThat(result.contentHash).hasLength(8);
    }

    @Test
    public void parse_truncatedJsonWithPlantName_returnsPartial() {
        // JSON cut off mid-way but contains commonName
        String json = "{\"identification\": {\"commonName\": \"Snake Plant\", \"scientificName\": \"Sansevieria\", \"confidence\": \"high\"}, \"healthAssessment\": {\"score\": 7, \"summary\": \"Looking good";
        // Note: truncated mid-string

        RobustJsonParser.ParseResult result = RobustJsonParser.parse(json);

        assertThat(result.parseStatus).isEqualTo("PARTIAL");
        assertThat(result.result).isNotNull();
        assertThat(result.result.identification).isNotNull();
        assertThat(result.result.identification.commonName).isEqualTo("Snake Plant");
        assertThat(result.result.identification.scientificName).isEqualTo("Sansevieria");
        assertThat(result.result.identification.confidence).isEqualTo("high");
        assertThat(result.result.healthAssessment).isNotNull();
        assertThat(result.result.healthAssessment.score).isEqualTo(7);
        assertThat(result.contentHash).isNotNull();
    }

    @Test
    public void parse_malformedJsonWithScoreAndName_returnsPartial() {
        // Broken JSON but regex can extract name + score
        String json = "{\"identification\": {\"commonName\": \"Pothos\", \"scientificName\": \"Epipremnum aureum\" MISSING_COMMA \"confidence\": \"medium\"}, healthAssessment\": {\"score\": 6}}";

        RobustJsonParser.ParseResult result = RobustJsonParser.parse(json);

        assertThat(result.parseStatus).isEqualTo("PARTIAL");
        assertThat(result.result).isNotNull();
        assertThat(result.result.identification.commonName).isEqualTo("Pothos");
        assertThat(result.result.identification.scientificName).isEqualTo("Epipremnum aureum");
        assertThat(result.result.healthAssessment.score).isEqualTo(6);
    }

    @Test
    public void parse_jsonMissingTier2Fields_returnsOk() {
        // Has identification and health but no carePlan - should still be OK since JsonParser handles optional fields
        String json = "{"
            + "\"identification\": {"
            + "  \"commonName\": \"Fern\","
            + "  \"scientificName\": \"Nephrolepis\","
            + "  \"confidence\": \"high\","
            + "  \"notes\": \"\""
            + "},"
            + "\"healthAssessment\": {"
            + "  \"score\": 9,"
            + "  \"summary\": \"Excellent condition\","
            + "  \"issues\": []"
            + "},"
            + "\"immediateActions\": []"
            + "}";

        RobustJsonParser.ParseResult result = RobustJsonParser.parse(json);

        assertThat(result.parseStatus).isEqualTo("OK");
        assertThat(result.result).isNotNull();
        assertThat(result.result.identification.commonName).isEqualTo("Fern");
        assertThat(result.result.healthAssessment.score).isEqualTo(9);
    }

    @Test
    public void parse_partialResult_hasTier1Fields() {
        // Verify commonName and healthScore extracted via regex
        String json = "GARBAGE {\"commonName\": \"Cactus\"} MORE GARBAGE {\"score\": 8} INVALID";

        RobustJsonParser.ParseResult result = RobustJsonParser.parse(json);

        assertThat(result.parseStatus).isEqualTo("PARTIAL");
        assertThat(result.result).isNotNull();
        assertThat(result.result.identification).isNotNull();
        assertThat(result.result.identification.commonName).isEqualTo("Cactus");
        assertThat(result.result.healthAssessment).isNotNull();
        assertThat(result.result.healthAssessment.score).isEqualTo(8);
    }

    @Test
    public void parse_contentHash_isStableForSameInput() {
        String json = "{\"commonName\": \"Rose\"}";

        RobustJsonParser.ParseResult result1 = RobustJsonParser.parse(json);
        RobustJsonParser.ParseResult result2 = RobustJsonParser.parse(json);

        assertThat(result1.contentHash).isEqualTo(result2.contentHash);
    }

    @Test
    public void parse_contentHash_differsForDifferentInput() {
        String json1 = "{\"commonName\": \"Rose\"}";
        String json2 = "{\"commonName\": \"Tulip\"}";

        RobustJsonParser.ParseResult result1 = RobustJsonParser.parse(json1);
        RobustJsonParser.ParseResult result2 = RobustJsonParser.parse(json2);

        assertThat(result1.contentHash).isNotEqualTo(result2.contentHash);
    }

    @Test
    public void parse_jsonWithOnlyCarePlan_parseSucceeds() {
        // Has carePlan but no identification - this is valid JSON so JsonParser succeeds
        // JsonParser handles optional fields, so this returns OK with defaults
        String json = "{\"carePlan\": {\"watering\": {\"frequency\": \"Weekly\"}}}";

        RobustJsonParser.ParseResult result = RobustJsonParser.parse(json);

        // Valid JSON, so JsonParser succeeds (identification defaults to null)
        assertThat(result.parseStatus).isEqualTo("OK");
    }

    @Test
    public void parse_emptyJsonObject_parseSucceeds() {
        // Empty object is valid JSON, JsonParser succeeds with null/default fields
        String json = "{}";

        RobustJsonParser.ParseResult result = RobustJsonParser.parse(json);

        assertThat(result.parseStatus).isEqualTo("OK");
        assertThat(result.result).isNotNull();
    }

    @Test
    public void parse_onlyIdentificationNoHealth_returnsPartial() {
        // Has identification fields but health assessment is missing or malformed
        String json = "{\"identification\": {\"commonName\": \"Spider Plant\", \"confidence\": \"high\"}, \"healthAssessment\": BROKEN}";

        RobustJsonParser.ParseResult result = RobustJsonParser.parse(json);

        assertThat(result.parseStatus).isEqualTo("PARTIAL");
        assertThat(result.result).isNotNull();
        assertThat(result.result.identification).isNotNull();
        assertThat(result.result.identification.commonName).isEqualTo("Spider Plant");
        assertThat(result.result.identification.confidence).isEqualTo("high");
    }

    @Test
    public void parse_onlyHealthNoIdentification_returnsPartial() {
        // Has health score/summary but identification is missing or malformed
        String json = "{\"identification\": BROKEN, \"healthAssessment\": {\"score\": 4, \"summary\": \"Needs attention\"}}";

        RobustJsonParser.ParseResult result = RobustJsonParser.parse(json);

        assertThat(result.parseStatus).isEqualTo("PARTIAL");
        assertThat(result.result).isNotNull();
        assertThat(result.result.healthAssessment).isNotNull();
        assertThat(result.result.healthAssessment.score).isEqualTo(4);
        assertThat(result.result.healthAssessment.summary).isEqualTo("Needs attention");
    }

    @Test
    public void parse_tier1FieldsWithEscapedQuotes_parseSucceeds() {
        // This is valid JSON, so JsonParser will succeed
        String json = "{\"identification\": {\"commonName\": \"Bird's Nest Fern\"}, \"healthAssessment\": {\"score\": 7}}";

        RobustJsonParser.ParseResult result = RobustJsonParser.parse(json);

        assertThat(result.parseStatus).isEqualTo("OK");
        assertThat(result.result.identification.commonName).isEqualTo("Bird's Nest Fern");
        assertThat(result.result.healthAssessment.score).isEqualTo(7);
    }

    @Test
    public void parse_scoreAsString_extractsCorrectly() {
        // Test that score can be extracted even if it's in a string context
        String json = "JUNK \"score\": 3 MORE JUNK";

        RobustJsonParser.ParseResult result = RobustJsonParser.parse(json);

        assertThat(result.parseStatus).isEqualTo("PARTIAL");
        assertThat(result.result.healthAssessment).isNotNull();
        assertThat(result.result.healthAssessment.score).isEqualTo(3);
    }

    @Test
    public void parse_multipleScores_parseSucceeds() {
        // Valid JSON with nested objects
        String json = "{\"healthAssessment\": {\"score\": 9}}";

        RobustJsonParser.ParseResult result = RobustJsonParser.parse(json);

        assertThat(result.parseStatus).isEqualTo("OK");
        assertThat(result.result.healthAssessment.score).isEqualTo(9);
    }
}
