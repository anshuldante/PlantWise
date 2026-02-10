package com.leafiq.app.util;

import android.content.Context;
import android.text.format.DateUtils;

import java.util.Calendar;

/**
 * Utility class for formatting dates and timestamps consistently across the app.
 * Provides section labels (Today/Yesterday/date) for timeline grouping and
 * relative time strings for timestamps.
 * <p>
 * Centralizes date formatting logic previously duplicated across adapters.
 */
public final class DateFormatter {

    private DateFormatter() {
        // Utility class - prevent instantiation
    }

    /**
     * Gets a section label for timeline grouping.
     * Returns "Today" if same calendar day, "Yesterday" if previous calendar day,
     * otherwise formatted date (e.g., "Feb 8" or "Feb 8, 2026" for older dates).
     *
     * @param context Android context for date formatting
     * @param timestamp Unix timestamp in milliseconds
     * @return Section label string (Today/Yesterday/formatted date)
     */
    public static String getSectionLabel(Context context, long timestamp) {
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(timestamp);

        int nowYear = now.get(Calendar.YEAR);
        int nowDay = now.get(Calendar.DAY_OF_YEAR);
        int targetYear = target.get(Calendar.YEAR);
        int targetDay = target.get(Calendar.DAY_OF_YEAR);

        // Same calendar day
        if (nowYear == targetYear && nowDay == targetDay) {
            return "Today";
        }

        // Previous calendar day
        if (nowYear == targetYear && nowDay - targetDay == 1) {
            return "Yesterday";
        }

        // For dates within the same year, show abbreviated month and day
        // For older dates (> 365 days), include year
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH;
        long daysSince = (now.getTimeInMillis() - timestamp) / DateUtils.DAY_IN_MILLIS;
        if (daysSince > 365) {
            flags |= DateUtils.FORMAT_SHOW_YEAR;
        }

        return DateUtils.formatDateTime(context, timestamp, flags);
    }

    /**
     * Gets a relative time string for a timestamp.
     * Returns "Unknown" for invalid timestamps (<= 0).
     * For recent timestamps (< 7 days), returns relative format (e.g., "3 days ago").
     * For older timestamps, returns formatted date (e.g., "Feb 1" or "Feb 1, 2025" if > 365 days).
     *
     * @param context Android context for date formatting
     * @param timestamp Unix timestamp in milliseconds
     * @return Relative time string or formatted date
     */
    public static String getRelativeTime(Context context, long timestamp) {
        if (timestamp <= 0) {
            return "Unknown";
        }

        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        // For recent timestamps (< 7 days), use relative time
        if (diff < 7 * DateUtils.DAY_IN_MILLIS) {
            return DateUtils.getRelativeTimeSpanString(
                    timestamp,
                    now,
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString();
        }

        // For older timestamps, use formatted date
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH;
        long daysSince = diff / DateUtils.DAY_IN_MILLIS;
        if (daysSince > 365) {
            flags |= DateUtils.FORMAT_SHOW_YEAR;
        }

        return DateUtils.formatDateTime(context, timestamp, flags);
    }
}
