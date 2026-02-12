package com.leafiq.app.care;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.leafiq.app.LeafIQApplication;
import com.leafiq.app.data.entity.CareSchedule;
import com.leafiq.app.data.entity.Plant;
import com.leafiq.app.data.repository.PlantRepository;
import com.leafiq.app.util.KeystoreHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * BroadcastReceiver that reschedules care reminder alarms after device reboot.
 * <p>
 * AlarmManager alarms are cleared on device reboot, so this receiver
 * ensures reminders are rescheduled when the device boots up.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            LeafIQApplication app = (LeafIQApplication) context.getApplicationContext();
            CareScheduleManager scheduleManager = app.getCareScheduleManager();
            scheduleManager.rescheduleAllAlarms();

            // Re-create notifications for currently overdue schedules
            app.getAppExecutors().io().execute(() -> {
                PlantRepository repository = app.getPlantRepository();
                KeystoreHelper keystoreHelper = new KeystoreHelper(context);

                // Trigger same logic as daily alarm to show overdue notifications
                if (!keystoreHelper.areRemindersPaused()) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(Calendar.HOUR_OF_DAY, 23);
                    calendar.set(Calendar.MINUTE, 59);
                    calendar.set(Calendar.SECOND, 59);
                    calendar.set(Calendar.MILLISECOND, 999);
                    long endOfToday = calendar.getTimeInMillis();

                    List<CareSchedule> dueSchedules = repository.getDueSchedules(endOfToday);

                    if (!dueSchedules.isEmpty()) {
                        List<NotificationHelper.DueScheduleInfo> dueInfoList = new ArrayList<>();
                        for (CareSchedule schedule : dueSchedules) {
                            Plant plant = repository.getPlantByIdSync(schedule.plantId);
                            if (plant != null) {
                                dueInfoList.add(new NotificationHelper.DueScheduleInfo(schedule, plant));
                            }
                        }
                        if (!dueInfoList.isEmpty()) {
                            NotificationHelper.buildGroupedNotification(context, dueInfoList);
                            Log.i("CareSystem", "Recreated " + dueInfoList.size() + " overdue notifications after boot");
                        }
                    }
                }
            });
        }
    }
}
