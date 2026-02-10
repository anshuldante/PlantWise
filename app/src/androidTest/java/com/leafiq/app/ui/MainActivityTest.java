package com.leafiq.app.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.leafiq.app.MainActivity;
import com.leafiq.app.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
        new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void bottomNavigation_isDisplayed() {
        onView(withId(R.id.bottom_navigation))
            .check(matches(isDisplayed()));
    }

    @Test
    public void libraryFragment_isDisplayedByDefault() {
        // The library fragment should be displayed by default
        onView(withId(R.id.recycler_plants))
            .check(matches(isDisplayed()));
    }

    @Test
    public void emptyState_isDisplayedWhenNoPlants() {
        // With no plants in the database, empty state should show
        onView(withId(R.id.empty_state))
            .check(matches(isDisplayed()));
    }

    @Test
    public void emptyState_showsCorrectMessage() {
        onView(withText(R.string.empty_library_title))
            .check(matches(isDisplayed()));

        onView(withText(R.string.empty_library_message))
            .check(matches(isDisplayed()));
    }

    @Test
    public void clickTimelineTab_showsTimelineFragment() {
        // Click on timeline tab
        onView(withId(R.id.nav_timeline))
            .perform(click());

        // Timeline content should be visible (filter chips or empty state)
        onView(withId(R.id.filter_chips))
            .check(matches(isDisplayed()));
    }

    @Test
    public void clickTimelineTab_thenLibrary_returnsToLibrary() {
        // Click on timeline tab
        onView(withId(R.id.nav_timeline))
            .perform(click());

        // Click on library tab
        onView(withId(R.id.nav_library))
            .perform(click());

        // Library fragment should be visible again
        onView(withId(R.id.recycler_plants))
            .check(matches(isDisplayed()));
    }

    @Test
    public void timelineFragment_hasFilterChips() {
        onView(withId(R.id.nav_timeline))
            .perform(click());

        onView(withId(R.id.chip_all))
            .check(matches(isDisplayed()));

        onView(withId(R.id.chip_healthy))
            .check(matches(isDisplayed()));
    }
}
