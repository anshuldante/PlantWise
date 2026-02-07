package com.leafiq.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented test to verify test infrastructure on device/emulator.
 * Real instrumented tests are in specific package test files.
 *
 * @see com.leafiq.app.data.db.PlantDaoTest
 * @see com.leafiq.app.data.db.AnalysisDaoTest
 * @see com.leafiq.app.ui.MainActivityTest
 * @see com.leafiq.app.ui.LibraryFragmentTest
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    @Test
    public void useAppContext() {
        // Context of the app under test
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.leafiq.app", appContext.getPackageName());
    }

    @Test
    public void appContext_isNotNull() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertNotNull(appContext);
    }
}
