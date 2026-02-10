package com.leafiq.app.util;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;

@RunWith(AndroidJUnit4.class)
public class DateFormatterTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
    }

    // ==================== getSectionLabel tests ====================

    @Test
    public void getSectionLabel_today_returnsToday() {
        long now = System.currentTimeMillis();
        String label = DateFormatter.getSectionLabel(context, now);
        assertThat(label).isEqualTo("Today");
    }

    @Test
    public void getSectionLabel_earlierToday_returnsToday() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 1);

        String label = DateFormatter.getSectionLabel(context, cal.getTimeInMillis());
        assertThat(label).isEqualTo("Today");
    }

    @Test
    public void getSectionLabel_yesterday_returnsYesterday() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);

        String label = DateFormatter.getSectionLabel(context, cal.getTimeInMillis());
        assertThat(label).isEqualTo("Yesterday");
    }

    @Test
    public void getSectionLabel_twoDaysAgo_returnsFormattedDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -2);

        String label = DateFormatter.getSectionLabel(context, cal.getTimeInMillis());
        // Should not be "Today" or "Yesterday"
        assertThat(label).isNotEqualTo("Today");
        assertThat(label).isNotEqualTo("Yesterday");
        // Should be a non-empty formatted date
        assertThat(label).isNotEmpty();
    }

    @Test
    public void getSectionLabel_oldDate_includesYear() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -400);

        String label = DateFormatter.getSectionLabel(context, cal.getTimeInMillis());
        // Dates > 365 days old should include year
        int expectedYear = cal.get(Calendar.YEAR);
        assertThat(label).contains(String.valueOf(expectedYear));
    }

    // ==================== getRelativeTime tests ====================

    @Test
    public void getRelativeTime_zeroTimestamp_returnsUnknown() {
        String result = DateFormatter.getRelativeTime(context, 0);
        assertThat(result).isEqualTo("Unknown");
    }

    @Test
    public void getRelativeTime_negativeTimestamp_returnsUnknown() {
        String result = DateFormatter.getRelativeTime(context, -1);
        assertThat(result).isEqualTo("Unknown");
    }

    @Test
    public void getRelativeTime_justNow_returnsRelativeString() {
        long now = System.currentTimeMillis();
        String result = DateFormatter.getRelativeTime(context, now);
        // Should be something like "0 min. ago" or "just now"
        assertThat(result).isNotEmpty();
        assertThat(result).isNotEqualTo("Unknown");
    }

    @Test
    public void getRelativeTime_withinSevenDays_returnsRelativeString() {
        long threeDaysAgo = System.currentTimeMillis() - (3L * 24 * 60 * 60 * 1000);
        String result = DateFormatter.getRelativeTime(context, threeDaysAgo);
        // Should contain "ago" for relative time
        assertThat(result).isNotEmpty();
        assertThat(result).isNotEqualTo("Unknown");
    }

    @Test
    public void getRelativeTime_olderThanSevenDays_returnsFormattedDate() {
        long tenDaysAgo = System.currentTimeMillis() - (10L * 24 * 60 * 60 * 1000);
        String result = DateFormatter.getRelativeTime(context, tenDaysAgo);
        // Should be a formatted date, not "Unknown"
        assertThat(result).isNotEmpty();
        assertThat(result).isNotEqualTo("Unknown");
        // Should not contain "ago" since it's beyond the 7-day relative window
        assertThat(result).doesNotContain("ago");
    }

    @Test
    public void getRelativeTime_veryOldDate_includesYear() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -400);

        String result = DateFormatter.getRelativeTime(context, cal.getTimeInMillis());
        int expectedYear = cal.get(Calendar.YEAR);
        assertThat(result).contains(String.valueOf(expectedYear));
    }
}
