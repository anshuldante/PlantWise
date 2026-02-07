package com.leafiq.app.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.leafiq.app.MainActivity;
import com.leafiq.app.R;
import com.leafiq.app.data.db.AppDatabase;
import com.leafiq.app.data.entity.Plant;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class LibraryFragmentTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
        new ActivityScenarioRule<>(MainActivity.class);

    private AppDatabase database;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = AppDatabase.getInstance(context);
    }

    @After
    public void tearDown() {
        // Clean up test data
        // Note: In a real app, you'd use a test database or dependency injection
    }

    @Test
    public void emptyState_visibleWhenNoPlants() {
        // Empty state should be visible when there are no plants
        onView(withId(R.id.empty_state))
            .check(matches(isDisplayed()));

        onView(withText(R.string.empty_library_title))
            .check(matches(isDisplayed()));
    }

    @Test
    public void recyclerView_existsInLayout() {
        onView(withId(R.id.recycler_plants))
            .check(matches(isDisplayed()));
    }

    // Note: Tests that require database state would need proper test setup
    // with dependency injection or a test database. Here's an example structure:

    /*
    @Test
    public void plantCard_displaysPlantInfo() {
        // Insert test plant
        Plant plant = new Plant();
        plant.id = "test-1";
        plant.commonName = "Test Monstera";
        plant.scientificName = "Monstera testicus";
        plant.latestHealthScore = 8;
        plant.updatedAt = System.currentTimeMillis();
        database.plantDao().insertPlant(plant);

        // Verify plant card shows correct info
        onView(withId(R.id.recycler_plants))
            .check(matches(hasDescendant(withText("Test Monstera"))));
    }

    @Test
    public void plantCard_clickOpensDetailActivity() {
        // Insert test plant and click on it
        // Would need Espresso Intents to verify the detail activity opens
    }
    */
}
