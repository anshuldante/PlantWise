package com.leafiq.app.util;

import android.util.Log;

import com.leafiq.app.data.model.PlantAnalysisResult;

import org.json.JSONException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Robust JSON parser with layered parsing strategy:
 * Layer 1: Try structured parsing via JsonParser
 * Layer 2: Fall back to regex extraction for Tier 1 fields (plant name, health score, confidence)
 *
 * Tier 1 fields (salvage aggressively): identification and health assessment basics
 * Tier 2 fields (drop silently if malformed): care plan, immediate actions, detailed issues
 */
public class RobustJsonParser {

    private static final String TAG = "AnalysisParser";

    // Tier 1 regex patterns
    private static final Pattern COMMON_NAME_PATTERN = Pattern.compile("\"commonName\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern SCIENTIFIC_NAME_PATTERN = Pattern.compile("\"scientificName\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern CONFIDENCE_PATTERN = Pattern.compile("\"confidence\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern HEALTH_SCORE_PATTERN = Pattern.compile("\"score\"\\s*:\\s*(\\d+)");
    private static final Pattern SUMMARY_PATTERN = Pattern.compile("\"summary\"\\s*:\\s*\"([^\"]+)\"");

    /**
     * Result of parsing with status tracking
     */
    public static class ParseResult {
        public final PlantAnalysisResult result;
        public final String parseStatus;  // "OK", "PARTIAL", "FAILED", "EMPTY"
        public final String contentHash;  // Short hash for log correlation

        private ParseResult(PlantAnalysisResult result, String parseStatus, String contentHash) {
            this.result = result;
            this.parseStatus = parseStatus;
            this.contentHash = contentHash;
        }

        public static ParseResult ok(PlantAnalysisResult result, String contentHash) {
            return new ParseResult(result, "OK", contentHash);
        }

        public static ParseResult partial(PlantAnalysisResult result, String contentHash) {
            return new ParseResult(result, "PARTIAL", contentHash);
        }

        public static ParseResult failed(String contentHash) {
            return new ParseResult(null, "FAILED", contentHash);
        }

        public static ParseResult empty() {
            return new ParseResult(null, "EMPTY", null);
        }
    }

    /**
     * Parse raw JSON response with layered fallback strategy
     */
    public static ParseResult parse(String rawResponse) {
        // Handle null/empty
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            Log.d(TAG, "Parse result: status=EMPTY");
            return ParseResult.empty();
        }

        String contentHash = computeContentHash(rawResponse);

        // Layer 1: Try structured parsing
        try {
            PlantAnalysisResult result = JsonParser.parsePlantAnalysis(rawResponse);
            Log.i(TAG, "Parse result: status=OK hash=" + contentHash + " (structured parse succeeded)");
            return ParseResult.ok(result, contentHash);
        } catch (JSONException e) {
            Log.d(TAG, "Structured parse failed, attempting Tier 1 fallback: " + e.getMessage());
        }

        // Layer 2: Tier 1 field extraction via regex
        PlantAnalysisResult partialResult = extractTier1Fields(rawResponse);

        if (partialResult != null) {
            int fieldCount = countExtractedFields(partialResult);
            String fieldList = buildFieldList(partialResult);
            Log.i(TAG, String.format("analysis_parse_partial: hash=%s fields=[%s]", contentHash, fieldList));
            Log.i(TAG, String.format("Parse result: status=PARTIAL hash=%s tier1Fields=%d", contentHash, fieldCount));
            return ParseResult.partial(partialResult, contentHash);
        }

        // Nothing extracted
        Log.i(TAG, "analysis_parse_failed: hash=" + contentHash);
        Log.i(TAG, String.format("Parse result: status=FAILED hash=%s tier1Fields=0", contentHash));
        return ParseResult.failed(contentHash);
    }

    /**
     * Extract Tier 1 fields using regex patterns
     */
    private static PlantAnalysisResult extractTier1Fields(String rawResponse) {
        PlantAnalysisResult result = new PlantAnalysisResult();
        boolean anyFieldExtracted = false;

        // Try to extract identification fields
        String commonName = extractMatch(COMMON_NAME_PATTERN, rawResponse);
        String scientificName = extractMatch(SCIENTIFIC_NAME_PATTERN, rawResponse);
        String confidence = extractMatch(CONFIDENCE_PATTERN, rawResponse);

        if (commonName != null || scientificName != null || confidence != null) {
            result.identification = new PlantAnalysisResult.Identification();
            result.identification.commonName = commonName != null ? commonName : "Unknown";
            result.identification.scientificName = scientificName != null ? scientificName : "";
            result.identification.confidence = confidence != null ? confidence : "low";
            result.identification.notes = "";
            anyFieldExtracted = true;
            Log.d(TAG, String.format("Tier 1 fallback: commonName=%s scientificName=%s confidence=%s",
                commonName, scientificName, confidence));
        }

        // Try to extract health assessment fields
        String scoreStr = extractMatch(HEALTH_SCORE_PATTERN, rawResponse);
        String summary = extractMatch(SUMMARY_PATTERN, rawResponse);

        if (scoreStr != null || summary != null) {
            result.healthAssessment = new PlantAnalysisResult.HealthAssessment();
            try {
                result.healthAssessment.score = scoreStr != null ? Integer.parseInt(scoreStr) : 5;
            } catch (NumberFormatException e) {
                result.healthAssessment.score = 5;
            }
            result.healthAssessment.summary = summary != null ? summary : "";
            result.healthAssessment.issues = new ArrayList<>();
            anyFieldExtracted = true;
            Log.d(TAG, String.format("Tier 1 fallback: score=%d summary=%s",
                result.healthAssessment.score, summary));
        }

        // Initialize empty collections for Tier 2 fields (silently dropped)
        result.immediateActions = new ArrayList<>();
        result.funFact = "";

        return anyFieldExtracted ? result : null;
    }

    /**
     * Extract first match from pattern
     */
    private static String extractMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Count how many Tier 1 fields were extracted
     */
    private static int countExtractedFields(PlantAnalysisResult result) {
        int count = 0;
        if (result.identification != null) {
            if (result.identification.commonName != null && !result.identification.commonName.equals("Unknown")) {
                count++;
            }
            if (result.identification.scientificName != null && !result.identification.scientificName.isEmpty()) {
                count++;
            }
            if (result.identification.confidence != null && !result.identification.confidence.equals("low")) {
                count++;
            }
        }
        if (result.healthAssessment != null) {
            if (result.healthAssessment.score != 5) {
                count++;
            }
            if (result.healthAssessment.summary != null && !result.healthAssessment.summary.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Build comma-separated list of extracted fields for logging
     */
    private static String buildFieldList(PlantAnalysisResult result) {
        ArrayList<String> fields = new ArrayList<>();
        if (result.identification != null) {
            if (result.identification.commonName != null && !result.identification.commonName.equals("Unknown")) {
                fields.add("commonName");
            }
            if (result.identification.scientificName != null && !result.identification.scientificName.isEmpty()) {
                fields.add("scientificName");
            }
            if (result.identification.confidence != null && !result.identification.confidence.equals("low")) {
                fields.add("confidence");
            }
        }
        if (result.healthAssessment != null) {
            if (result.healthAssessment.score != 5) {
                fields.add("score");
            }
            if (result.healthAssessment.summary != null && !result.healthAssessment.summary.isEmpty()) {
                fields.add("summary");
            }
        }
        return String.join(", ", fields);
    }

    /**
     * Compute short SHA-256 hash for content correlation
     */
    private static String computeContentHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes());
            // Return first 8 characters of hex encoding
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < Math.min(4, hash.length); i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return "00000000";
        }
    }
}
