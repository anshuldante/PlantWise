package com.leafiq.app.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class PhotoQualityCheckerTest {

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
}
