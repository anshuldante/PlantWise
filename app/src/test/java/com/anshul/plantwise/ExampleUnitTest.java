package com.anshul.plantwise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Basic unit test to verify test infrastructure works.
 * Real tests are in specific package test files.
 *
 * @see com.anshul.plantwise.util.JsonParserTest
 * @see com.anshul.plantwise.ai.PromptBuilderTest
 * @see com.anshul.plantwise.ai.ClaudeProviderTest
 * @see com.anshul.plantwise.data.model.PlantAnalysisResultTest
 */
public class ExampleUnitTest {

    @Test
    public void testInfrastructure_works() {
        // Simple test to verify test infrastructure
        assertEquals(4, 2 + 2);
    }

    @Test
    public void strings_notNull() {
        String test = "PlantWise";
        assertNotNull(test);
        assertEquals("PlantWise", test);
    }
}
