package com.leafiq.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Basic unit test to verify test infrastructure works.
 * Real tests are in specific package test files.
 *
 * @see com.leafiq.app.util.JsonParserTest
 * @see com.leafiq.app.ai.PromptBuilderTest
 * @see com.leafiq.app.ai.ClaudeProviderTest
 * @see com.leafiq.app.data.model.PlantAnalysisResultTest
 */
public class ExampleUnitTest {

    @Test
    public void testInfrastructure_works() {
        // Simple test to verify test infrastructure
        assertEquals(4, 2 + 2);
    }

    @Test
    public void strings_notNull() {
        String test = "LeafIQ";
        assertNotNull(test);
        assertEquals("LeafIQ", test);
    }
}
