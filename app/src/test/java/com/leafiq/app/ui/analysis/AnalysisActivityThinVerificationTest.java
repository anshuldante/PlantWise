package com.leafiq.app.ui.analysis;

import org.junit.Test;

import java.lang.reflect.Method;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNotNull;

/**
 * Verifies AnalysisActivity is thin UI glue after extraction.
 * Checks that business logic methods have been moved to extracted classes.
 *
 * This is a structural test that uses reflection to verify the Activity
 * doesn't contain methods that were extracted to:
 * - AnalysisRenderer (rendering logic)
 * - AnalysisCoordinator (flow decisions)
 * - AnalysisStateMapper (state mapping)
 *
 * Plan 12-10, Task 2: Final thin Activity verification.
 */
public class AnalysisActivityThinVerificationTest {

    @Test
    public void activity_doesNotContain_displayResults() {
        assertMethodNotPresent("displayResults");
    }

    @Test
    public void activity_doesNotContain_addIssueView() {
        assertMethodNotPresent("addIssueView");
    }

    @Test
    public void activity_doesNotContain_addActionView() {
        assertMethodNotPresent("addActionView");
    }

    @Test
    public void activity_doesNotContain_addCarePlanSection() {
        assertMethodNotPresent("addCarePlanSection");
    }

    @Test
    public void activity_doesNotContain_addCarePlanItem() {
        assertMethodNotPresent("addCarePlanItem");
    }

    @Test
    public void activity_doesNotContain_setHealthScoreColor() {
        assertMethodNotPresent("setHealthScoreColor");
    }

    @Test
    public void activity_doesNotContain_getSeverityEmoji() {
        assertMethodNotPresent("getSeverityEmoji");
    }

    @Test
    public void activity_doesNotContain_getPriorityPrefix() {
        assertMethodNotPresent("getPriorityPrefix");
    }

    @Test
    public void extractedClasses_exist() {
        // Verify all extracted classes are accessible
        assertNotNull(AnalysisRenderer.class);
        assertNotNull(AnalysisCoordinator.class);
        assertNotNull(AnalysisStateMapper.class);
    }

    @Test
    public void extractedClasses_haveExpectedMethods() {
        // Verify key methods exist in extracted classes
        assertMethodPresent(AnalysisRenderer.class, "render");
        assertMethodPresent(AnalysisRenderer.class, "getHealthScoreColorRes");
        assertMethodPresent(AnalysisRenderer.class, "getSeverityEmoji");
        assertMethodPresent(AnalysisRenderer.class, "getPriorityPrefix");

        assertMethodPresent(AnalysisCoordinator.class, "evaluateQuality");
        assertMethodPresent(AnalysisCoordinator.class, "evaluateReanalyze");
        assertMethodPresent(AnalysisCoordinator.class, "shouldUseQuickForReanalyze");

        assertMethodPresent(AnalysisStateMapper.class, "buildMinimalResult");
        assertMethodPresent(AnalysisStateMapper.class, "getFallbackMessage");
        assertMethodPresent(AnalysisStateMapper.class, "shouldShowReanalyze");
    }

    /**
     * Verifies that a method with the given name is NOT present in AnalysisActivity.
     */
    private void assertMethodNotPresent(String methodName) {
        Method[] methods = AnalysisActivity.class.getDeclaredMethods();
        for (Method method : methods) {
            assertThat(method.getName()).isNotEqualTo(methodName);
        }
    }

    /**
     * Verifies that a method with the given name IS present in the specified class.
     */
    private void assertMethodPresent(Class<?> clazz, String methodName) {
        Method[] methods = clazz.getDeclaredMethods();
        boolean found = false;
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                found = true;
                break;
            }
        }
        assertThat(found).isTrue();
    }
}
