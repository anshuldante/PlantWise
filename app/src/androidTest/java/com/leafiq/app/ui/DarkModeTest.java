package com.leafiq.app.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.leafiq.app.MainActivity;
import com.leafiq.app.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for dark mode support.
 * Verifies that the app properly responds to system theme changes.
 */
@RunWith(AndroidJUnit4.class)
public class DarkModeTest {

    private Context context;

    @Rule
    public ActivityScenarioRule<MainActivity> activityScenarioRule =
        new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void darkModeColors_areDefined() {
        // Verify dark mode colors exist and are different from light mode
        int lightTextPrimary = context.getResources().getColor(R.color.text_primary, null);

        // Force dark mode configuration
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.uiMode = Configuration.UI_MODE_NIGHT_YES | (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK);
        Context darkContext = context.createConfigurationContext(config);

        int darkTextPrimary = darkContext.getResources().getColor(R.color.text_primary, null);

        // In dark mode, text_primary should be light (high value)
        // In light mode, text_primary should be dark (low value)
        // The colors should be different
        assertThat(lightTextPrimary).isNotEqualTo(darkTextPrimary);
    }

    @Test
    public void lightModeColors_areDefined() {
        // Force light mode configuration
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.uiMode = Configuration.UI_MODE_NIGHT_NO | (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK);
        Context lightContext = context.createConfigurationContext(config);

        int lightBackground = lightContext.getResources().getColor(R.color.md_theme_background, null);

        // Light mode background should be light colored (high RGB values)
        int red = (lightBackground >> 16) & 0xFF;
        int green = (lightBackground >> 8) & 0xFF;
        int blue = lightBackground & 0xFF;

        // Light background should have high RGB values (close to white)
        assertThat(red).isGreaterThan(200);
        assertThat(green).isGreaterThan(200);
        assertThat(blue).isGreaterThan(200);
    }

    @Test
    public void darkModeBackground_isDark() {
        // Force dark mode configuration
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.uiMode = Configuration.UI_MODE_NIGHT_YES | (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK);
        Context darkContext = context.createConfigurationContext(config);

        int darkBackground = darkContext.getResources().getColor(R.color.md_theme_background, null);

        // Dark mode background should be dark colored (low RGB values)
        int red = (darkBackground >> 16) & 0xFF;
        int green = (darkBackground >> 8) & 0xFF;
        int blue = darkBackground & 0xFF;

        // Dark background should have low RGB values (close to black)
        assertThat(red).isLessThan(50);
        assertThat(green).isLessThan(50);
        assertThat(blue).isLessThan(50);
    }

    @Test
    public void darkModeTextPrimary_isLight() {
        // Force dark mode configuration
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.uiMode = Configuration.UI_MODE_NIGHT_YES | (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK);
        Context darkContext = context.createConfigurationContext(config);

        int darkTextPrimary = darkContext.getResources().getColor(R.color.text_primary, null);

        // Dark mode text should be light colored for readability
        int red = (darkTextPrimary >> 16) & 0xFF;
        int green = (darkTextPrimary >> 8) & 0xFF;
        int blue = darkTextPrimary & 0xFF;

        // Text should be light (high RGB values)
        assertThat(red).isGreaterThan(200);
        assertThat(green).isGreaterThan(200);
        assertThat(blue).isGreaterThan(200);
    }

    @Test
    public void lightModeTextPrimary_isDark() {
        // Force light mode configuration
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.uiMode = Configuration.UI_MODE_NIGHT_NO | (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK);
        Context lightContext = context.createConfigurationContext(config);

        int lightTextPrimary = lightContext.getResources().getColor(R.color.text_primary, null);

        // Light mode text should be dark colored for readability
        int red = (lightTextPrimary >> 16) & 0xFF;
        int green = (lightTextPrimary >> 8) & 0xFF;
        int blue = lightTextPrimary & 0xFF;

        // Text should be dark (low RGB values)
        assertThat(red).isLessThan(50);
        assertThat(green).isLessThan(50);
        assertThat(blue).isLessThan(50);
    }

    @Test
    public void themeUsesSystemDefault() {
        // Verify the app uses DayNight theme which follows system settings
        activityScenarioRule.getScenario().onActivity(activity -> {
            int currentNightMode = AppCompatDelegate.getDefaultNightMode();
            // Default should be MODE_NIGHT_FOLLOW_SYSTEM or MODE_NIGHT_UNSPECIFIED
            assertThat(currentNightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM ||
                       currentNightMode == AppCompatDelegate.MODE_NIGHT_UNSPECIFIED).isTrue();
        });
    }

    @Test
    public void activityLaunches_inCurrentTheme() {
        // Verify the activity launches successfully in current theme
        activityScenarioRule.getScenario().onActivity(activity -> {
            assertThat(activity).isNotNull();
            assertThat(activity.isFinishing()).isFalse();
        });

        // Verify the main content is visible
        onView(withId(R.id.bottom_navigation)).check(matches(isDisplayed()));
    }

    @Test
    public void cardBackgroundColor_differsBetweenModes() {
        // Force light mode configuration
        Configuration lightConfig = new Configuration(context.getResources().getConfiguration());
        lightConfig.uiMode = Configuration.UI_MODE_NIGHT_NO | (lightConfig.uiMode & ~Configuration.UI_MODE_NIGHT_MASK);
        Context lightContext = context.createConfigurationContext(lightConfig);

        // Force dark mode configuration
        Configuration darkConfig = new Configuration(context.getResources().getConfiguration());
        darkConfig.uiMode = Configuration.UI_MODE_NIGHT_YES | (darkConfig.uiMode & ~Configuration.UI_MODE_NIGHT_MASK);
        Context darkContext = context.createConfigurationContext(darkConfig);

        int lightCardBg = lightContext.getResources().getColor(R.color.card_background, null);
        int darkCardBg = darkContext.getResources().getColor(R.color.card_background, null);

        // Card backgrounds should be different between modes
        assertThat(lightCardBg).isNotEqualTo(darkCardBg);
    }
}
