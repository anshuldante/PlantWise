package com.leafiq.app.care;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.leafiq.app.LeafIQApplication;
import com.leafiq.app.data.entity.CareSchedule;
import com.leafiq.app.data.entity.Plant;
import com.leafiq.app.data.repository.PlantRepository;
import com.leafiq.app.util.KeystoreHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * BroadcastReceiver for care reminder alarm triggers and notification actions.
 * <p>
 * Handles three types of events:
 * 1. Daily alarm trigger (no action) - checks due schedules and fires notifications
 * 2. ACTION_DONE - marks care as complete and dismisses notification
 * 3. ACTION_SNOOZE - snoozes reminder and updates schedule
 */
public class CareReminderReceiver extends BroadcastReceiver {

    public static final String ACTION_DONE = "com.leafiq.app.ACTION_CARE_DONE";
    public static final String ACTION_SNOOZE = "com.leafiq.app.ACTION_CARE_SNOOZE";

    private static final String EXTRA_SCHEDULE_ID = "schedule_id";
    private static final String EXTRA_SNOOZE_OPTION = "snooze_option";

    private static final long MILLIS_PER_HOUR = 60 * 60 * 1000L;
    private static final long MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR;

    private static final String SUGGEST_ADJUST_MARKER = "[SUGGEST_ADJUST]";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // BroadcastReceiver must complete quickly - use goAsync for database work
        final PendingResult pendingResult = goAsync();

        LeafIQApplication app = (LeafIQApplication) context.getApplicationContext();
        PlantRepository repository = app.getPlantRepository();
        CareScheduleManager scheduleManager = app.getCareScheduleManager();
        KeystoreHelper keystoreHelper = new KeystoreHelper(context);

        // Run on background thread
        app.getAppExecutors().io().execute(() -> {
            try {
                if (action == null) {
                    // Daily alarm trigger - check due schedules
                    handleDailyAlarm(context, repository, scheduleManager, keystoreHelper);
                } else if (ACTION_DONE.equals(action)) {
                    // Mark care as complete
                    String scheduleId = intent.getStringExtra(EXTRA_SCHEDULE_ID);
                    handleDoneAction(context, repository, scheduleManager, scheduleId);
                } else if (ACTION_SNOOZE.equals(action)) {
                    // Snooze reminder
                    String scheduleId = intent.getStringExtra(EXTRA_SCHEDULE_ID);
                    int snoozeOption = intent.getIntExtra(EXTRA_SNOOZE_OPTION, 0);
                    handleSnoozeAction(context, repository, scheduleManager, scheduleId, snoozeOption);
                }
            } finally {
                // Signal that background work is complete
                pendingResult.finish();
            }
        });
    }

    /**
     * Handles daily alarm trigger.
     * Checks for due schedules and fires grouped notifications.
     */
    private void handleDailyAlarm(Context context, PlantRepository repository,
                                   CareScheduleManager scheduleManager, KeystoreHelper keystoreHelper) {
        // Check if reminders are paused
        if (keystoreHelper.areRemindersPaused()) {
            scheduleManager.scheduleNextAlarm();
            return;
        }

        // Get end of today for due schedule check
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        long endOfToday = calendar.getTimeInMillis();

        // Get all due schedules
        List<CareSchedule> dueSchedules = repository.getDueSchedules(endOfToday);

        if (dueSchedules.isEmpty()) {
            // No due schedules, just reschedule for tomorrow
            scheduleManager.scheduleNextAlarm();
            return;
        }

        // Build list of DueScheduleInfo objects (schedule + plant)
        List<NotificationHelper.DueScheduleInfo> dueScheduleInfoList = new ArrayList<>();
        for (CareSchedule schedule : dueSchedules) {
            Plant plant = repository.getPlantByIdSync(schedule.plantId);
            if (plant != null) {
                dueScheduleInfoList.add(new NotificationHelper.DueScheduleInfo(schedule, plant));
            }
        }

        // Build and show grouped notifications
        if (!dueScheduleInfoList.isEmpty()) {
            NotificationHelper.buildGroupedNotification(context, dueScheduleInfoList);
        }

        // Reschedule for tomorrow
        scheduleManager.scheduleNextAlarm();
    }

    /**
     * Handles "Done" action from notification.
     * Marks care as complete, dismisses notification, shows toast.
     */
    private void handleDoneAction(Context context, PlantRepository repository,
                                   CareScheduleManager scheduleManager, String scheduleId) {
        if (scheduleId == null) {
            return;
        }

        // Get schedule to retrieve plant info
        CareSchedule schedule = repository.getScheduleByIdSync(scheduleId);
        if (schedule == null) {
            return;
        }

        // Get plant for toast message
        Plant plant = repository.getPlantByIdSync(schedule.plantId);
        String plantName = plant != null && plant.nickname != null && !plant.nickname.isEmpty()
                ? plant.nickname
                : (plant != null && plant.commonName != null && !plant.commonName.isEmpty()
                        ? plant.commonName : "plant");

        // Mark care as complete
        repository.markCareComplete(scheduleId, "notification_action", new PlantRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // Dismiss notification
                NotificationHelper.dismissNotification(context, scheduleId);

                // Show toast on main thread
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> {
                    String careVerb = getCareVerb(schedule.careType);
                    Toast.makeText(context, careVerb + " " + plantName, Toast.LENGTH_SHORT).show();
                });

                // Reschedule alarm
                scheduleManager.scheduleNextAlarm();
            }

            @Override
            public void onError(Exception e) {
                // Log error silently
                android.util.Log.e("CareReminderReceiver", "Failed to mark care complete", e);
            }
        });
    }

    /**
     * Handles "Snooze" action from notification.
     * Updates nextDue based on snooze option, increments snooze count, dismisses notification.
     */
    private void handleSnoozeAction(Context context, PlantRepository repository,
                                     CareScheduleManager scheduleManager, String scheduleId,
                                     int snoozeOption) {
        if (scheduleId == null) {
            return;
        }

        // Get schedule
        CareSchedule schedule = repository.getScheduleByIdSync(scheduleId);
        if (schedule == null) {
            return;
        }

        // Calculate new nextDue based on snooze option
        long now = System.currentTimeMillis();
        switch (snoozeOption) {
            case 0: // 6 hours
                schedule.nextDue = now + (6 * MILLIS_PER_HOUR);
                break;
            case 1: // 1 day
                schedule.nextDue = now + MILLIS_PER_DAY;
                break;
            case 2: // Next regular cycle
                schedule.nextDue = schedule.nextDue + (schedule.frequencyDays * MILLIS_PER_DAY);
                break;
        }

        // Increment snooze count
        schedule.snoozeCount++;

        // If snoozed 3+ times, add marker to notes for UI prompt
        if (schedule.snoozeCount >= 3 && !schedule.notes.contains(SUGGEST_ADJUST_MARKER)) {
            schedule.notes = schedule.notes + " " + SUGGEST_ADJUST_MARKER;
        }

        // Update schedule
        repository.updateSchedule(schedule, new PlantRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // Dismiss notification
                NotificationHelper.dismissNotification(context, scheduleId);

                // Reschedule alarm
                scheduleManager.scheduleNextAlarm();
            }

            @Override
            public void onError(Exception e) {
                // Log error silently
                android.util.Log.e("CareReminderReceiver", "Failed to snooze reminder", e);
            }
        });
    }

    /**
     * Gets past tense verb for care type (for toast messages).
     */
    private String getCareVerb(String careType) {
        switch (careType) {
            case "water":
                return "Watered";
            case "fertilize":
                return "Fertilized";
            case "repot":
                return "Repotted";
            default:
                return "Completed care for";
        }
    }
}
