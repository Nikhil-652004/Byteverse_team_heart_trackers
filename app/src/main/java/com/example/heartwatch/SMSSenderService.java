package com.example.heartwatch;

import android.app.IntentService;
import android.content.Intent;
import android.telephony.SmsManager;

public class SMSSenderService extends IntentService {

    public SMSSenderService() {
        super("SMSSenderService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String phoneNumber = intent.getStringExtra("emergencyContactPhoneNumber");

        // Send SMS message
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, "Help! I need assistance.", null, null);
    }
}
