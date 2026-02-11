package com.leafiq.app.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

/**
 * Unit tests for PhotoQualityChecker threshold logic and QualityResult model.
 * Tests verify two-tier rejection, override eligibility, and threshold sanity.
 */
public class PhotoQualityCheckerTest {

    // ========== Existing Tests (backward compatibility) ==========

    @Test
    public void qualityResult_ok_passedTrueAndNullMessage() {
        PhotoQualityChecker.QualityResult result = PhotoQualityChecker.QualityResult.ok();

        assertThat(result.passed).isTrue();
        assertThat(result.message).isNull();
    }

    @Test
    public void qualityResult_fail_passedFalseWithMessage() {
        PhotoQualityChecker.QualityResult result =
                PhotoQualityChecker.QualityResult.fail("Too dark", "dark");

        assertThat(result.passed).isFalse();
        assertThat(result.message).isEqualTo("Too dark");
    }

    @Test
    public void qualityResult_fail_preservesIssueType() {
        PhotoQualityChecker.QualityResult result =
                PhotoQualityChecker.QualityResult.fail("Blurry photo", "blur");

        assertThat(result.issueType).isEqualTo("blur");
    }

    @Test
    public void qualityResult_ok_nullIssueType() {
        PhotoQualityChecker.QualityResult result = PhotoQualityChecker.QualityResult.ok();

        assertThat(result.issueType).isNull();
    }

    // ========== New Tests (enhanced behavior) ==========

    @Test
    public void qualityResult_ok_overrideAllowedFalse() {
        PhotoQualityChecker.QualityResult result = PhotoQualityChecker.QualityResult.ok();

        assertThat(result.overrideAllowed).isFalse();
        assertThat(result.issueSeverity).isNull();
    }

    @Test
    public void qualityResult_okWithScores_preservesScores() {
        PhotoQualityChecker.QualityResult result =
                PhotoQualityChecker.QualityResult.ok(50.5f, 0.75f);

        assertThat(result.passed).isTrue();
        assertThat(result.blurScore).isWithin(0.01f).of(50.5f);
        assertThat(result.brightnessScore).isWithin(0.01f).of(0.75f);
        assertThat(result.overrideAllowed).isFalse();
    }

    @Test
    public void qualityResult_failBorderline_overrideAllowedTrue() {
        PhotoQualityChecker.QualityResult result =
                PhotoQualityChecker.QualityResult.fail("Too blurry", "blur", true, "borderline", 40f, 0.5f);

        assertThat(result.passed).isFalse();
        assertThat(result.overrideAllowed).isTrue();
        assertThat(result.issueSeverity).isEqualTo("borderline");
        assertThat(result.blurScore).isWithin(0.01f).of(40f);
        assertThat(result.brightnessScore).isWithin(0.01f).of(0.5f);
    }

    @Test
    public void qualityResult_egregiousFail_overrideAllowedFalse() {
        PhotoQualityChecker.QualityResult result =
                PhotoQualityChecker.QualityResult.egregiousFail("Extremely blurry", "blur", 10f, 0.6f);

        assertThat(result.passed).isFalse();
        assertThat(result.overrideAllowed).isFalse();
        assertThat(result.issueSeverity).isEqualTo("egregious");
        assertThat(result.blurScore).isWithin(0.01f).of(10f);
        assertThat(result.brightnessScore).isWithin(0.01f).of(0.6f);
    }

    @Test
    public void qualityResult_failBackwardCompatible_defaultsToOverrideAllowed() {
        // Old fail(message, issueType) signature should default to borderline with override allowed
        PhotoQualityChecker.QualityResult result =
                PhotoQualityChecker.QualityResult.fail("Some issue", "blur");

        assertThat(result.passed).isFalse();
        assertThat(result.overrideAllowed).isTrue();
        assertThat(result.issueSeverity).isEqualTo("borderline");
    }

    // ========== Threshold Sanity Tests ==========

    @Test
    public void thresholds_egregiousBlurScorePositive() {
        assertThat(PhotoQualityChecker.EGREGIOUS_BLUR_SCORE).isGreaterThan(0f);
    }

    @Test
    public void thresholds_minBlurScoreGreaterThanEgregious() {
        assertThat(PhotoQualityChecker.MIN_BLUR_SCORE)
                .isGreaterThan(PhotoQualityChecker.EGREGIOUS_BLUR_SCORE);
    }

    @Test
    public void thresholds_quickDiagnosisBlurBetweenEgregiousAndStandard() {
        // Quick Diagnosis is more lenient (lower threshold) than standard
        // but stricter than egregious
        assertThat(PhotoQualityChecker.QUICK_DIAGNOSIS_BLUR_SCORE)
                .isGreaterThan(PhotoQualityChecker.EGREGIOUS_BLUR_SCORE);
        assertThat(PhotoQualityChecker.QUICK_DIAGNOSIS_BLUR_SCORE)
                .isLessThan(PhotoQualityChecker.MIN_BLUR_SCORE);
    }

    @Test
    public void thresholds_minBrightnessLessThanMaxBrightness() {
        assertThat(PhotoQualityChecker.MIN_BRIGHTNESS)
                .isLessThan(PhotoQualityChecker.MAX_BRIGHTNESS);
    }

    @Test
    public void thresholds_egregiousMinBrightnessLessThanStandard() {
        assertThat(PhotoQualityChecker.EGREGIOUS_MIN_BRIGHTNESS)
                .isLessThan(PhotoQualityChecker.MIN_BRIGHTNESS);
    }

    @Test
    public void thresholds_egregiousMaxBrightnessGreaterThanStandard() {
        assertThat(PhotoQualityChecker.EGREGIOUS_MAX_BRIGHTNESS)
                .isGreaterThan(PhotoQualityChecker.MAX_BRIGHTNESS);
    }

    @Test
    public void thresholds_quickDiagnosisBrightnessMoreLenient() {
        // Quick Diagnosis accepts darker photos (lower min) and brighter photos (higher max)
        assertThat(PhotoQualityChecker.QUICK_DIAGNOSIS_MIN_BRIGHTNESS)
                .isLessThan(PhotoQualityChecker.MIN_BRIGHTNESS);
        assertThat(PhotoQualityChecker.QUICK_DIAGNOSIS_MAX_BRIGHTNESS)
                .isGreaterThan(PhotoQualityChecker.MAX_BRIGHTNESS);
    }

    @Test
    public void thresholds_brightnessRangesValid() {
        // All brightness thresholds should be between 0 and 1
        assertThat(PhotoQualityChecker.EGREGIOUS_MIN_BRIGHTNESS).isAtLeast(0f);
        assertThat(PhotoQualityChecker.MIN_BRIGHTNESS).isAtLeast(0f);
        assertThat(PhotoQualityChecker.QUICK_DIAGNOSIS_MIN_BRIGHTNESS).isAtLeast(0f);

        assertThat(PhotoQualityChecker.MAX_BRIGHTNESS).isAtMost(1f);
        assertThat(PhotoQualityChecker.QUICK_DIAGNOSIS_MAX_BRIGHTNESS).isAtMost(1f);
        assertThat(PhotoQualityChecker.EGREGIOUS_MAX_BRIGHTNESS).isAtMost(1f);
    }
}
