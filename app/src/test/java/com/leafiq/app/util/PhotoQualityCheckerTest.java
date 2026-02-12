package com.leafiq.app.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

/**
 * Unit tests for PhotoQualityChecker threshold logic and QualityResult model.
 * Tests verify two-tier rejection, override eligibility, and threshold sanity.
 */
public class PhotoQualityCheckerTest {

    // ========== QualityResult model tests ==========

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
                PhotoQualityChecker.QualityResult.fail("Too dark", "dark");

        assertThat(result.issueType).isEqualTo("dark");
    }

    @Test
    public void qualityResult_ok_nullIssueType() {
        PhotoQualityChecker.QualityResult result = PhotoQualityChecker.QualityResult.ok();

        assertThat(result.issueType).isNull();
    }

    @Test
    public void qualityResult_ok_overrideAllowedFalse() {
        PhotoQualityChecker.QualityResult result = PhotoQualityChecker.QualityResult.ok();

        assertThat(result.overrideAllowed).isFalse();
        assertThat(result.issueSeverity).isNull();
    }

    @Test
    public void qualityResult_okWithBrightness_preservesScore() {
        PhotoQualityChecker.QualityResult result =
                PhotoQualityChecker.QualityResult.ok(0.75f);

        assertThat(result.passed).isTrue();
        assertThat(result.brightnessScore).isWithin(0.01f).of(0.75f);
        assertThat(result.overrideAllowed).isFalse();
    }

    @Test
    public void qualityResult_failBorderline_overrideAllowedTrue() {
        PhotoQualityChecker.QualityResult result =
                PhotoQualityChecker.QualityResult.fail("Too dark", "dark", true, "borderline", 0.12f);

        assertThat(result.passed).isFalse();
        assertThat(result.overrideAllowed).isTrue();
        assertThat(result.issueSeverity).isEqualTo("borderline");
        assertThat(result.brightnessScore).isWithin(0.01f).of(0.12f);
    }

    @Test
    public void qualityResult_egregiousFail_overrideAllowedFalse() {
        PhotoQualityChecker.QualityResult result =
                PhotoQualityChecker.QualityResult.egregiousFail("Extremely dark", "dark", 0.03f);

        assertThat(result.passed).isFalse();
        assertThat(result.overrideAllowed).isFalse();
        assertThat(result.issueSeverity).isEqualTo("egregious");
        assertThat(result.brightnessScore).isWithin(0.01f).of(0.03f);
    }

    @Test
    public void qualityResult_failBackwardCompatible_defaultsToOverrideAllowed() {
        PhotoQualityChecker.QualityResult result =
                PhotoQualityChecker.QualityResult.fail("Some issue", "dark");

        assertThat(result.passed).isFalse();
        assertThat(result.overrideAllowed).isTrue();
        assertThat(result.issueSeverity).isEqualTo("borderline");
    }

    // ========== Threshold Sanity Tests ==========

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
        assertThat(PhotoQualityChecker.QUICK_DIAGNOSIS_MIN_BRIGHTNESS)
                .isLessThan(PhotoQualityChecker.MIN_BRIGHTNESS);
        assertThat(PhotoQualityChecker.QUICK_DIAGNOSIS_MAX_BRIGHTNESS)
                .isGreaterThan(PhotoQualityChecker.MAX_BRIGHTNESS);
    }

    @Test
    public void thresholds_brightnessRangesValid() {
        assertThat(PhotoQualityChecker.EGREGIOUS_MIN_BRIGHTNESS).isAtLeast(0f);
        assertThat(PhotoQualityChecker.MIN_BRIGHTNESS).isAtLeast(0f);
        assertThat(PhotoQualityChecker.QUICK_DIAGNOSIS_MIN_BRIGHTNESS).isAtLeast(0f);

        assertThat(PhotoQualityChecker.MAX_BRIGHTNESS).isAtMost(1f);
        assertThat(PhotoQualityChecker.QUICK_DIAGNOSIS_MAX_BRIGHTNESS).isAtMost(1f);
        assertThat(PhotoQualityChecker.EGREGIOUS_MAX_BRIGHTNESS).isAtMost(1f);
    }
}
