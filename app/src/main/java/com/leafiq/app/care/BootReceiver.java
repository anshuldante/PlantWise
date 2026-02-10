package com.leafiq.app.care;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.leafiq.app.LeafIQApplication;

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
        }
    }
}
