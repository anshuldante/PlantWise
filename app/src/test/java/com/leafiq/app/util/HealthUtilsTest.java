package com.leafiq.app.util;

import static com.google.common.truth.Truth.assertThat;

import com.leafiq.app.R;

import org.junit.Test;

public class HealthUtilsTest {

    // ==================== getHealthColorRes tests ====================

    @Test
    public void getHealthColorRes_score10_returnsHealthGood() {
        assertThat(HealthUtils.getHealthColorRes(10)).isEqualTo(R.color.health_good);
    }

    @Test
    public void getHealthColorRes_score7_returnsHealthGood() {
        assertThat(HealthUtils.getHealthColorRes(7)).isEqualTo(R.color.health_good);
    }

    @Test
    public void getHealthColorRes_score6_returnsHealthWarning() {
        assertThat(HealthUtils.getHealthColorRes(6)).isEqualTo(R.color.health_warning);
    }

    @Test
    public void getHealthColorRes_score4_returnsHealthWarning() {
        assertThat(HealthUtils.getHealthColorRes(4)).isEqualTo(R.color.health_warning);
    }

    @Test
    public void getHealthColorRes_score3_returnsHealthBad() {
        assertThat(HealthUtils.getHealthColorRes(3)).isEqualTo(R.color.health_bad);
    }

    @Test
    public void getHealthColorRes_score0_returnsHealthBad() {
        assertThat(HealthUtils.getHealthColorRes(0)).isEqualTo(R.color.health_bad);
    }

    // ==================== getHealthLabel tests ====================

    @Test
    public void getHealthLabel_score10_returnsHealthy() {
        assertThat(HealthUtils.getHealthLabel(10)).isEqualTo("Healthy");
    }

    @Test
    public void getHealthLabel_score7_returnsHealthy() {
        assertThat(HealthUtils.getHealthLabel(7)).isEqualTo("Healthy");
    }

    @Test
    public void getHealthLabel_score6_returnsNeedsAttention() {
        assertThat(HealthUtils.getHealthLabel(6)).isEqualTo("Needs Attention");
    }

    @Test
    public void getHealthLabel_score4_returnsNeedsAttention() {
        assertThat(HealthUtils.getHealthLabel(4)).isEqualTo("Needs Attention");
    }

    @Test
    public void getHealthLabel_score3_returnsCritical() {
        assertThat(HealthUtils.getHealthLabel(3)).isEqualTo("Critical");
    }

    @Test
    public void getHealthLabel_score0_returnsCritical() {
        assertThat(HealthUtils.getHealthLabel(0)).isEqualTo("Critical");
    }

    // ==================== Consistency tests ====================

    @Test
    public void colorAndLabel_shareThresholds_score7boundary() {
        // Score 7 should be healthy in both methods
        assertThat(HealthUtils.getHealthColorRes(7)).isEqualTo(R.color.health_good);
        assertThat(HealthUtils.getHealthLabel(7)).isEqualTo("Healthy");

        // Score 6 should be warning in both methods
        assertThat(HealthUtils.getHealthColorRes(6)).isEqualTo(R.color.health_warning);
        assertThat(HealthUtils.getHealthLabel(6)).isEqualTo("Needs Attention");
    }

    @Test
    public void colorAndLabel_shareThresholds_score4boundary() {
        // Score 4 should be warning in both methods
        assertThat(HealthUtils.getHealthColorRes(4)).isEqualTo(R.color.health_warning);
        assertThat(HealthUtils.getHealthLabel(4)).isEqualTo("Needs Attention");

        // Score 3 should be bad/critical in both methods
        assertThat(HealthUtils.getHealthColorRes(3)).isEqualTo(R.color.health_bad);
        assertThat(HealthUtils.getHealthLabel(3)).isEqualTo("Critical");
    }
}
