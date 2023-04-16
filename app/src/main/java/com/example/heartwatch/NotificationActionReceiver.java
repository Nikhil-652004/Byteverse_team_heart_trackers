package com.example.heartwatch;
import static com.example.heartwatch.MainActivity.ACTION_I_NEED_HELP;
import static com.example.heartwatch.MainActivity.NOTIFICATION_ID;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

public class NotificationActionReceiver extends BroadcastReceiver {
    public static final String ACTION_I_AM_OKAY = "com.example.app.I_AM_OKAY";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(ACTION_I_AM_OKAY)) {
            // User is okay, remove notification
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(NOTIFICATION_ID);
        } else if (action.equals(ACTION_I_NEED_HELP)) {
            // User needs help, schedule SMS to be sent after 2 minutes
            String emergencyContactPhoneNumber = intent.getStringExtra("emergencyContactPhoneNumber");
            Intent smsIntent = new Intent(context, SMSSenderService.class);
            smsIntent.putExtra("emergencyContactPhoneNumber", emergencyContactPhoneNumber);
            PendingIntent pendingIntent = PendingIntent.getService(context, 0, smsIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 2 * 60 * 1000, pendingIntent);
        }


    }
}

