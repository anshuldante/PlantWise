package com.leafiq.app.care;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

/**
 * Utility class for notification channel creation and notification building.
 * Handles setup for plant care reminder notifications.
 */
public class NotificationHelper {

    public static final String CHANNEL_ID = "plant_care_reminders";
    public static final String CHANNEL_NAME = "Plant Care Reminders";
    public static final String CHANNEL_DESC = "Notifications for watering, fertilizing, and repotting your plants";
    public static final int SUMMARY_NOTIFICATION_ID = 9999;
    public static final String GROUP_KEY = "com.leafiq.app.CARE_REMINDERS";

    /**
     * Creates the notification channel for plant care reminders.
     * Safe to call multiple times (no-op if channel already exists).
     * Only creates channel on Android O (API 26) and above.
     *
     * @param context Application context
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESC);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
