package com.leafiq.app.util;

import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PhotoTipsManager state machine behavior.
 */
@RunWith(JUnit4.class)
public class PhotoTipsManagerTest {

    private MockSharedPreferences mockPrefs;
    private PhotoTipsManager manager;

    /**
     * Simple mock SharedPreferences implementation for testing.
     * Tracks state in a HashMap instead of requiring Android framework.
     */
    private static class MockSharedPreferences implements SharedPreferences {
        private final Map<String, Object> data = new HashMap<>();

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            Object value = data.get(key);
            return value != null ? (Boolean) value : defValue;
        }

        @Override
        public String getString(String key, String defValue) {
            Object value = data.get(key);
            return value != null ? (String) value : defValue;
        }

        @Override
        public Editor edit() {
            return new MockEditor(data);
        }

        // Unused methods - not needed for tests
        @Override public Map<String, ?> getAll() { return null; }
        @Override public int getInt(String key, int defValue) { return 0; }
        @Override public long getLong(String key, long defValue) { return 0; }
        @Override public float getFloat(String key, float defValue) { return 0; }
        @Override public java.util.Set<String> getStringSet(String key, java.util.Set<String> defValues) { return null; }
        @Override public boolean contains(String key) { return false; }
        @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}
        @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

        private static class MockEditor implements Editor {
            private final Map<String, Object> data;

            MockEditor(Map<String, Object> data) {
                this.data = data;
            }

            @Override
            public Editor putString(String key, String value) {
                data.put(key, value);
                return this;
            }

            @Override
            public Editor putBoolean(String key, boolean value) {
                data.put(key, value);
                return this;
            }

            @Override
            public Editor remove(String key) {
                data.remove(key);
                return this;
            }

            @Override
            public void apply() {
                // No-op for tests
            }

            @Override
            public boolean commit() {
                return true;
            }

            // Unused methods
            @Override public Editor putInt(String key, int value) { return this; }
            @Override public Editor putLong(String key, long value) { return this; }
            @Override public Editor putFloat(String key, float value) { return this; }
            @Override public Editor putStringSet(String key, java.util.Set<String> values) { return this; }
            @Override public Editor clear() { return this; }
        }
    }

    @Before
    public void setUp() {
        mockPrefs = new MockSharedPreferences();
        manager = new PhotoTipsManager(mockPrefs);
    }

    // ==================== State Machine Tests ====================

    @Test
    public void testFirstTimeShouldShowTips() {
        // Fresh prefs - has_seen_tips is false
        assertTrue(manager.shouldShowTips());
    }

    @Test
    public void testAfterMarkSeenShouldNotShowTips() {
        manager.markTipsSeen();
        assertFalse(manager.shouldShowTips());
    }

    @Test
    public void testQualityFailureShouldShowTips() {
        // First mark as seen to suppress initial tips
        manager.markTipsSeen();
        assertFalse(manager.shouldShowTips());

        // Record quality failure
        manager.recordQualityFailure("blur");

        // Tips should re-appear
        assertTrue(manager.shouldShowTips());
    }

    @Test
    public void testMarkSeenClearsFailureReason() {
        // Record quality failure
        manager.recordQualityFailure("blur");
        assertEquals("blur", manager.getQualityFailureReason());

        // Mark tips seen
        manager.markTipsSeen();

        // Failure reason should be cleared
        assertNull(manager.getQualityFailureReason());
    }

    @Test
    public void testGetQualityFailureReasonReturnsRecordedReason() {
        // No failure initially
        assertNull(manager.getQualityFailureReason());

        // Record failure
        manager.recordQualityFailure("blur");
        assertEquals("blur", manager.getQualityFailureReason());
    }

    @Test
    public void testIsFirstTimeInitially() {
        // Fresh prefs - should be first time
        assertTrue(manager.isFirstTime());
    }

    @Test
    public void testIsFirstTimeAfterSeen() {
        manager.markTipsSeen();
        assertFalse(manager.isFirstTime());
    }

    @Test
    public void testMultipleFailureRecordsOverwrite() {
        // Record first failure
        manager.recordQualityFailure("blur");
        assertEquals("blur", manager.getQualityFailureReason());

        // Record second failure
        manager.recordQualityFailure("bright");
        assertEquals("bright", manager.getQualityFailureReason());
    }

    // ==================== Edge Case Tests ====================

    @Test
    public void testFailureReasonPersistsAcrossMultipleShouldShowTipsCalls() {
        manager.recordQualityFailure("dark");

        // Multiple shouldShowTips calls should return true
        assertTrue(manager.shouldShowTips());
        assertTrue(manager.shouldShowTips());
        assertTrue(manager.shouldShowTips());

        // Failure reason should still be present
        assertEquals("dark", manager.getQualityFailureReason());
    }

    @Test
    public void testMarkSeenWithoutFailureReasonWorks() {
        // First time - no failure reason
        assertTrue(manager.shouldShowTips());

        // Mark seen
        manager.markTipsSeen();

        // Should not show tips anymore
        assertFalse(manager.shouldShowTips());
        assertNull(manager.getQualityFailureReason());
    }

    @Test
    public void testAllQualityFailureTypes() {
        String[] failureTypes = {"blur", "dark", "bright", "resolution"};

        for (String type : failureTypes) {
            mockPrefs = new MockSharedPreferences();
            manager = new PhotoTipsManager(mockPrefs);

            manager.recordQualityFailure(type);
            assertEquals(type, manager.getQualityFailureReason());
            assertTrue(manager.shouldShowTips());

            manager.markTipsSeen();
            assertNull(manager.getQualityFailureReason());
            assertFalse(manager.shouldShowTips());
        }
    }

    @Test
    public void testFullUserJourney() {
        // 1. First-time user
        assertTrue(manager.isFirstTime());
        assertTrue(manager.shouldShowTips());
        assertNull(manager.getQualityFailureReason());

        // 2. User sees tips and clicks "Got It"
        manager.markTipsSeen();
        assertFalse(manager.isFirstTime());
        assertFalse(manager.shouldShowTips());

        // 3. User takes a blurry photo
        manager.recordQualityFailure("blur");
        assertFalse(manager.isFirstTime()); // Not first time anymore
        assertTrue(manager.shouldShowTips()); // But tips should show
        assertEquals("blur", manager.getQualityFailureReason());

        // 4. User sees contextual tips and clicks "Got It"
        manager.markTipsSeen();
        assertFalse(manager.shouldShowTips());
        assertNull(manager.getQualityFailureReason());

        // 5. User takes another quality-failed photo
        manager.recordQualityFailure("dark");
        assertTrue(manager.shouldShowTips());
        assertEquals("dark", manager.getQualityFailureReason());
    }
}
