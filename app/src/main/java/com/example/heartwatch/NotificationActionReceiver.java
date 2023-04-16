package com.example.heartwatch;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationActionReceiver extends BroadcastReceiver {
    public static final String ACTION_I_AM_OKAY = "com.example.app.I_AM_OKAY";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (NotificationActionReceiver.ACTION_I_AM_OKAY.equals(action)) {
            // User clicked "I'm okay" button, handle it here
        }
    }
}

