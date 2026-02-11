package com.leafiq.app.ui.analysis;

import static com.google.common.truth.Truth.assertThat;

import com.leafiq.app.R;

import org.junit.Test;

/**
 * Tests for AnalysisRenderer's pure helper functions.
 * These are testable without Android dependencies.
 */
public class AnalysisRendererTest {

    @Test
    public void getSeverityEmoji_high_returnsExclamation() {
        assertThat(AnalysisRenderer.getSeverityEmoji("high")).isEqualTo("!");
        assertThat(AnalysisRenderer.getSeverityEmoji("HIGH")).isEqualTo("!");
        assertThat(AnalysisRenderer.getSeverityEmoji("High")).isEqualTo("!");
    }

    @Test
    public void getSeverityEmoji_medium_returnsStar() {
        assertThat(AnalysisRenderer.getSeverityEmoji("medium")).isEqualTo("*");
        assertThat(AnalysisRenderer.getSeverityEmoji("MEDIUM")).isEqualTo("*");
        assertThat(AnalysisRenderer.getSeverityEmoji("Medium")).isEqualTo("*");
    }

    @Test
    public void getSeverityEmoji_low_returnsDash() {
        assertThat(AnalysisRenderer.getSeverityEmoji("low")).isEqualTo("-");
        assertThat(AnalysisRenderer.getSeverityEmoji("LOW")).isEqualTo("-");
    }

    @Test
    public void getSeverityEmoji_null_returnsDash() {
        assertThat(AnalysisRenderer.getSeverityEmoji(null)).isEqualTo("-");
    }

    @Test
    public void getSeverityEmoji_unknown_returnsDash() {
        assertThat(AnalysisRenderer.getSeverityEmoji("unknown")).isEqualTo("-");
        assertThat(AnalysisRenderer.getSeverityEmoji("")).isEqualTo("-");
    }

    @Test
    public void getPriorityPrefix_urgent_returnsUrgent() {
        assertThat(AnalysisRenderer.getPriorityPrefix("urgent")).isEqualTo("[URGENT] ");
        assertThat(AnalysisRenderer.getPriorityPrefix("URGENT")).isEqualTo("[URGENT] ");
        assertThat(AnalysisRenderer.getPriorityPrefix("Urgent")).isEqualTo("[URGENT] ");
    }

    @Test
    public void getPriorityPrefix_soon_returnsSoon() {
        assertThat(AnalysisRenderer.getPriorityPrefix("soon")).isEqualTo("[Soon] ");
        assertThat(AnalysisRenderer.getPriorityPrefix("SOON")).isEqualTo("[Soon] ");
        assertThat(AnalysisRenderer.getPriorityPrefix("Soon")).isEqualTo("[Soon] ");
    }

    @Test
    public void getPriorityPrefix_low_returnsDash() {
        assertThat(AnalysisRenderer.getPriorityPrefix("low")).isEqualTo("- ");
        assertThat(AnalysisRenderer.getPriorityPrefix("LOW")).isEqualTo("- ");
    }

    @Test
    public void getPriorityPrefix_null_returnsDash() {
        assertThat(AnalysisRenderer.getPriorityPrefix(null)).isEqualTo("- ");
    }

    @Test
    public void getPriorityPrefix_unknown_returnsDash() {
        assertThat(AnalysisRenderer.getPriorityPrefix("unknown")).isEqualTo("- ");
        assertThat(AnalysisRenderer.getPriorityPrefix("")).isEqualTo("- ");
    }

    @Test
    public void getHealthScoreColorRes_high_returnsGood() {
        assertThat(AnalysisRenderer.getHealthScoreColorRes(10)).isEqualTo(R.color.health_good);
        assertThat(AnalysisRenderer.getHealthScoreColorRes(9)).isEqualTo(R.color.health_good);
        assertThat(AnalysisRenderer.getHealthScoreColorRes(8)).isEqualTo(R.color.health_good);
        assertThat(AnalysisRenderer.getHealthScoreColorRes(7)).isEqualTo(R.color.health_good);
    }

    @Test
    public void getHealthScoreColorRes_medium_returnsWarning() {
        assertThat(AnalysisRenderer.getHealthScoreColorRes(6)).isEqualTo(R.color.health_warning);
        assertThat(AnalysisRenderer.getHealthScoreColorRes(5)).isEqualTo(R.color.health_warning);
        assertThat(AnalysisRenderer.getHealthScoreColorRes(4)).isEqualTo(R.color.health_warning);
    }

    @Test
    public void getHealthScoreColorRes_low_returnsBad() {
        assertThat(AnalysisRenderer.getHealthScoreColorRes(3)).isEqualTo(R.color.health_bad);
        assertThat(AnalysisRenderer.getHealthScoreColorRes(2)).isEqualTo(R.color.health_bad);
        assertThat(AnalysisRenderer.getHealthScoreColorRes(1)).isEqualTo(R.color.health_bad);
        assertThat(AnalysisRenderer.getHealthScoreColorRes(0)).isEqualTo(R.color.health_bad);
    }

    @Test
    public void getHealthScoreColorRes_negativeBoundary_returnsBad() {
        // Edge case: negative scores should still be handled
        assertThat(AnalysisRenderer.getHealthScoreColorRes(-1)).isEqualTo(R.color.health_bad);
    }

    @Test
    public void getHealthScoreColorRes_highBoundary_returnsGood() {
        // Edge case: scores above 10 should still be handled
        assertThat(AnalysisRenderer.getHealthScoreColorRes(11)).isEqualTo(R.color.health_good);
        assertThat(AnalysisRenderer.getHealthScoreColorRes(100)).isEqualTo(R.color.health_good);
    }
}
