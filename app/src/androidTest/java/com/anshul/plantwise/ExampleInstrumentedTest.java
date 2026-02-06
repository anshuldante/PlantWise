package com.anshul.plantwise;

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
 * @see com.anshul.plantwise.data.db.PlantDaoTest
 * @see com.anshul.plantwise.data.db.AnalysisDaoTest
 * @see com.anshul.plantwise.ui.MainActivityTest
 * @see com.anshul.plantwise.ui.LibraryFragmentTest
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    @Test
    public void useAppContext() {
        // Context of the app under test
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.anshul.plantwise", appContext.getPackageName());
    }

    @Test
    public void appContext_isNotNull() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertNotNull(appContext);
    }
}
