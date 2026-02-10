package com.leafiq.app.care;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.leafiq.app.data.entity.CareCompletion;
import com.leafiq.app.data.entity.CareItem;
import com.leafiq.app.data.entity.CareSchedule;
import com.leafiq.app.data.repository.PlantRepository;
import com.leafiq.app.util.KeystoreHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

/**
 * Central manager for care schedule operations.
 * Handles schedule creation from CareItems, alarm scheduling via AlarmManager,
 * and schedule lifecycle management.
 */
public class CareScheduleManager {

    private static final int DAILY_ALARM_REQUEST_CODE = 0;
    private static final long MILLIS_PER_DAY = 24 * 60 * 60 * 1000L;

    private final Context context;
    private final PlantRepository repository;
    private final KeystoreHelper keystoreHelper;
    private final AlarmManager alarmManager;

    /**
     * Creates a CareScheduleManager.
     *
     * @param context Application context
     * @param repository PlantRepository for database access
     * @param keystoreHelper KeystoreHelper for reminder preferences
     */
    public CareScheduleManager(Context context, PlantRepository repository, KeystoreHelper keystoreHelper) {
        this.context = context.getApplicationContext();
        this.repository = repository;
        this.keystoreHelper = keystoreHelper;
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    /**
     * Creates or updates care schedules from AI-derived CareItems.
     * <p>
     * For each CareItem with type "water", "fertilize", or "repot":
     * - If schedule exists and isCustom=false: updates frequency and notes from new AI data
     * - If schedule exists and isCustom=true: DOES NOT update (preserves user customization)
     * - If schedule doesn't exist: creates new schedule
     * <p>
     * After processing all items, schedules the next daily alarm.
     *
     * @param plantId Plant ID to create schedules for
     * @param careItems List of CareItems from AI analysis
     * @return List of schedules that need user prompt (isCustom=true with different AI frequency)
     */
    public List<CareSchedule> createSchedulesFromCareItems(String plantId, List<CareItem> careItems) {
        List<CareSchedule> needsPrompt = new ArrayList<>();

        // Get existing schedules for this plant
        List<CareSchedule> existingSchedules = repository.getSchedulesByPlantIdSync(plantId);

        for (CareItem item : careItems) {
            // Only process water, fertilize, repot (NOT prune per user decision)
            if (!"water".equals(item.type) && !"fertilize".equals(item.type) && !"repot".equals(item.type)) {
                continue;
            }

            // Check if schedule already exists for this care type
            CareSchedule existingSchedule = findScheduleByType(existingSchedules, item.type);

            if (existingSchedule != null) {
                if (!existingSchedule.isCustom) {
                    // AI-derived schedule: update with new AI data
                    existingSchedule.frequencyDays = item.frequencyDays;
                    existingSchedule.notes = item.notes;

                    // Recalculate nextDue from last completion (or now if no completion)
                    CareCompletion lastCompletion = repository.getLastCompletionForScheduleSync(existingSchedule.id);
                    if (lastCompletion != null) {
                        existingSchedule.nextDue = lastCompletion.completedAt + (item.frequencyDays * MILLIS_PER_DAY);
                    } else {
                        existingSchedule.nextDue = System.currentTimeMillis() + (item.frequencyDays * MILLIS_PER_DAY);
                    }

                    repository.updateSchedule(existingSchedule, new PlantRepository.RepositoryCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            // Updated successfully
                        }

                        @Override
                        public void onError(Exception e) {
                            // Log error silently
                        }
                    });
                } else {
                    // User-customized schedule: DO NOT update, flag for prompt
                    if (existingSchedule.frequencyDays != item.frequencyDays) {
                        // Store AI-recommended frequency in notes temporarily for UI prompt
                        // Format: "AI_RECOMMENDED:X|original_notes"
                        existingSchedule.notes = "AI_RECOMMENDED:" + item.frequencyDays + "|" +
                                (item.notes != null ? item.notes : "");
                        needsPrompt.add(existingSchedule);
                    }
                }
            } else {
                // Create new schedule
                CareSchedule newSchedule = new CareSchedule();
                newSchedule.id = UUID.randomUUID().toString();
                newSchedule.plantId = plantId;
                newSchedule.careType = item.type;
                newSchedule.frequencyDays = item.frequencyDays;
                newSchedule.nextDue = System.currentTimeMillis() + (item.frequencyDays * MILLIS_PER_DAY);
                newSchedule.isCustom = false;
                newSchedule.isEnabled = true;
                newSchedule.snoozeCount = 0;
                newSchedule.notes = item.notes;

                repository.insertSchedule(newSchedule, new PlantRepository.RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        // Inserted successfully
                    }

                    @Override
                    public void onError(Exception e) {
                        // Log error silently
                    }
                });
            }
        }

        // Schedule next alarm after processing all items
        scheduleNextAlarm();

        return needsPrompt;
    }

    /**
     * Schedules the next daily alarm for reminder checking.
     * <p>
     * If reminders are paused, cancels any existing alarm and returns.
     * Otherwise, schedules an alarm for the user's preferred reminder time
     * (today if not yet passed, otherwise tomorrow).
     */
    public void scheduleNextAlarm() {
        if (keystoreHelper.areRemindersPaused()) {
            cancelAlarm();
            return;
        }

        // Get preferred reminder time
        int[] preferredTime = keystoreHelper.getPreferredReminderTime();
        int hourOfDay = preferredTime[0];
        int minute = preferredTime[1];

        // Calculate next alarm time
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If time has passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        long alarmTimeMillis = calendar.getTimeInMillis();

        // Create PendingIntent for CareReminderReceiver
        Intent intent = new Intent(context, CareReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                DAILY_ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Schedule exact alarm
        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarmTimeMillis,
                    pendingIntent
            );
        }
    }

    /**
     * Cancels the daily alarm.
     */
    public void cancelAlarm() {
        Intent intent = new Intent(context, CareReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                DAILY_ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    /**
     * Reschedules all alarms.
     * Called by BootReceiver after device reboot.
     */
    public void rescheduleAllAlarms() {
        if (!keystoreHelper.areRemindersPaused()) {
            scheduleNextAlarm();
        }
    }

    /**
     * Toggles reminders for a specific plant.
     * Updates all schedules for the plant and recalculates nextDue when re-enabling.
     *
     * @param plantId Plant ID
     * @param enabled True to enable reminders, false to disable
     */
    public void toggleRemindersForPlant(String plantId, boolean enabled) {
        List<CareSchedule> schedules = repository.getSchedulesByPlantIdSync(plantId);

        for (CareSchedule schedule : schedules) {
            schedule.isEnabled = enabled;

            if (enabled) {
                // Recalculate nextDue from last completion
                CareCompletion lastCompletion = repository.getLastCompletionForScheduleSync(schedule.id);
                if (lastCompletion != null) {
                    schedule.nextDue = lastCompletion.completedAt + (schedule.frequencyDays * MILLIS_PER_DAY);
                } else {
                    schedule.nextDue = System.currentTimeMillis() + (schedule.frequencyDays * MILLIS_PER_DAY);
                }
            }

            repository.updateSchedule(schedule, new PlantRepository.RepositoryCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    // Updated successfully
                }

                @Override
                public void onError(Exception e) {
                    // Log error silently
                }
            });
        }

        // Reschedule alarm
        scheduleNextAlarm();
    }

    /**
     * Updates the frequency of a care schedule and marks it as custom.
     *
     * @param scheduleId Schedule ID to update
     * @param newFrequencyDays New frequency in days
     */
    public void updateScheduleFrequency(String scheduleId, int newFrequencyDays) {
        CareSchedule schedule = repository.getScheduleByIdSync(scheduleId);
        if (schedule != null) {
            schedule.frequencyDays = newFrequencyDays;
            schedule.isCustom = true;

            // Recalculate nextDue from last completion (or now if no completion)
            CareCompletion lastCompletion = repository.getLastCompletionForScheduleSync(scheduleId);
            if (lastCompletion != null) {
                schedule.nextDue = lastCompletion.completedAt + (newFrequencyDays * MILLIS_PER_DAY);
            } else {
                schedule.nextDue = System.currentTimeMillis() + (newFrequencyDays * MILLIS_PER_DAY);
            }

            repository.updateSchedule(schedule, new PlantRepository.RepositoryCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    // Updated successfully
                }

                @Override
                public void onError(Exception e) {
                    // Log error silently
                }
            });

            // Reschedule alarm
            scheduleNextAlarm();
        }
    }

    /**
     * Helper method to find existing schedule by care type.
     */
    private CareSchedule findScheduleByType(List<CareSchedule> schedules, String careType) {
        for (CareSchedule schedule : schedules) {
            if (schedule.careType.equals(careType)) {
                return schedule;
            }
        }
        return null;
    }
}
