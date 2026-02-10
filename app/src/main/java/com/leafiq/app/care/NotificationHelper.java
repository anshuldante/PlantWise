package com.leafiq.app.care;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.leafiq.app.MainActivity;
import com.leafiq.app.R;
import com.leafiq.app.data.entity.CareSchedule;
import com.leafiq.app.ui.care.CareOverviewActivity;
import com.leafiq.app.data.entity.Plant;

import java.util.List;

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

    private static final int MAX_VISIBLE_ITEMS = 5;

    /**
     * Simple POJO to pair a care schedule with its plant.
     */
    public static class DueScheduleInfo {
        public final CareSchedule schedule;
        public final Plant plant;

        public DueScheduleInfo(CareSchedule schedule, Plant plant) {
            this.schedule = schedule;
            this.plant = plant;
        }
    }

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

    /**
     * Builds and displays grouped notifications for due plant care schedules.
     * Creates child notifications for each schedule and a summary notification.
     *
     * @param context Application context
     * @param dueSchedules List of schedules due with their associated plants
     */
    public static void buildGroupedNotification(Context context, List<DueScheduleInfo> dueSchedules) {
        if (dueSchedules == null || dueSchedules.isEmpty()) {
            return;
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        // Build child notifications (up to MAX_VISIBLE_ITEMS)
        int itemCount = Math.min(dueSchedules.size(), MAX_VISIBLE_ITEMS);
        for (int i = 0; i < itemCount; i++) {
            DueScheduleInfo info = dueSchedules.get(i);
            CareSchedule schedule = info.schedule;
            Plant plant = info.plant;

            String careEmoji = getCareEmoji(schedule.careType);
            String careVerb = getCareVerb(schedule.careType);
            String displayName = getPlantDisplayName(plant);

            // Load circular plant thumbnail (sync load on background thread)
            Bitmap circularThumbnail = null;
            if (plant.thumbnailPath != null && !plant.thumbnailPath.isEmpty()) {
                try {
                    circularThumbnail = Glide.with(context)
                            .asBitmap()
                            .load(plant.thumbnailPath)
                            .transform(new CircleCrop())
                            .submit(48, 48)
                            .get();
                } catch (Exception e) {
                    // If thumbnail loading fails, continue without it
                    android.util.Log.w("NotificationHelper", "Failed to load thumbnail for " + displayName, e);
                }
            }

            // Build child notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(careEmoji + " " + displayName)
                    .setContentText(context.getString(R.string.time_to_care, careVerb.toLowerCase()))
                    .setGroup(GROUP_KEY)
                    .addAction(R.drawable.ic_check, "Done", createDonePendingIntent(context, schedule.id))
                    .addAction(R.drawable.ic_snooze, "Snooze", createSnoozePendingIntent(context, schedule.id))
                    .setAutoCancel(false)
                    .setContentIntent(createCareOverviewPendingIntent(context));

            if (circularThumbnail != null) {
                builder.setLargeIcon(circularThumbnail);
            }

            // Notify with unique ID per schedule
            notificationManager.notify(schedule.id.hashCode(), builder.build());
        }

        // Build summary notification with InboxStyle
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        for (int i = 0; i < itemCount; i++) {
            DueScheduleInfo info = dueSchedules.get(i);
            String careEmoji = getCareEmoji(info.schedule.careType);
            String careVerb = getCareVerb(info.schedule.careType);
            String displayName = getPlantDisplayName(info.plant);
            inboxStyle.addLine(careEmoji + " " + displayName + " - " + careVerb);
        }

        // Add overflow text if more than MAX_VISIBLE_ITEMS
        if (dueSchedules.size() > MAX_VISIBLE_ITEMS) {
            int overflow = dueSchedules.size() - MAX_VISIBLE_ITEMS;
            inboxStyle.addLine(context.getString(R.string.n_more_view_in_app, overflow));
        }

        inboxStyle.setBigContentTitle(context.getString(R.string.n_plants_need_care, dueSchedules.size()));
        inboxStyle.setSummaryText(context.getString(R.string.tap_to_view_all));

        NotificationCompat.Builder summaryBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setStyle(inboxStyle)
                .setGroup(GROUP_KEY)
                .setGroupSummary(true)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                .setContentIntent(createCareOverviewPendingIntent(context));

        notificationManager.notify(SUMMARY_NOTIFICATION_ID, summaryBuilder.build());
    }

    /**
     * Dismisses a notification for a specific schedule.
     * Also dismisses the summary notification if no child notifications remain.
     *
     * @param context Application context
     * @param scheduleId Schedule ID to dismiss
     */
    public static void dismissNotification(Context context, String scheduleId) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        // Cancel the child notification
        notificationManager.cancel(scheduleId.hashCode());

        // TODO: Check if any child notifications remain; if not, also cancel summary
        // For now, we'll let the summary remain (Android may auto-dismiss when last child is dismissed)
    }

    /**
     * Creates PendingIntent for "Done" action.
     */
    private static PendingIntent createDonePendingIntent(Context context, String scheduleId) {
        Intent intent = new Intent(context, CareReminderReceiver.class);
        intent.setAction(CareReminderReceiver.ACTION_DONE);
        intent.putExtra("schedule_id", scheduleId);

        int requestCode = ("done_" + scheduleId).hashCode();
        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

    /**
     * Creates PendingIntent for "Snooze" action.
     * Defaults to snooze option 2 (next regular cycle).
     */
    private static PendingIntent createSnoozePendingIntent(Context context, String scheduleId) {
        Intent intent = new Intent(context, CareReminderReceiver.class);
        intent.setAction(CareReminderReceiver.ACTION_SNOOZE);
        intent.putExtra("schedule_id", scheduleId);
        intent.putExtra("snooze_option", 2); // Default to "next due window"

        int requestCode = ("snooze_" + scheduleId).hashCode();
        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

    /**
     * Creates PendingIntent for tapping the notification (opens Care Overview).
     */
    private static PendingIntent createCareOverviewPendingIntent(Context context) {
        Intent intent = new Intent(context, CareOverviewActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(
                context,
                9998, // CARE_OVERVIEW_REQUEST_CODE
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

    /**
     * Gets emoji for care type.
     */
    private static String getCareEmoji(String careType) {
        switch (careType) {
            case "water":
                return "💧";
            case "fertilize":
                return "🌱";
            case "repot":
                return "🪴";
            default:
                return "🌿";
        }
    }

    /**
     * Gets verb for care type (capitalized).
     */
    private static String getCareVerb(String careType) {
        switch (careType) {
            case "water":
                return "Water";
            case "fertilize":
                return "Fertilize";
            case "repot":
                return "Repot";
            default:
                return "Care for";
        }
    }

    /**
     * Gets display name for plant.
     * Priority: nickname > commonName > "Your plant"
     */
    private static String getPlantDisplayName(Plant plant) {
        if (plant.nickname != null && !plant.nickname.isEmpty()) {
            return plant.nickname;
        }
        if (plant.commonName != null && !plant.commonName.isEmpty()) {
            return plant.commonName;
        }
        return "Your plant";
    }
}
