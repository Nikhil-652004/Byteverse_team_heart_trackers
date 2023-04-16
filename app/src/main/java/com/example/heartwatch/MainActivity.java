package com.example.heartwatch;

import static com.example.heartwatch.NotificationActionReceiver.ACTION_I_AM_OKAY;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import android.location.LocationListener;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.UUID;
import java.nio.charset.*;
public class MainActivity extends AppCompatActivity implements LocationListener {

    public static final String NOTIFICATION_CHANNEL_ID = "YOUR_CHANNEL_ID";
    private static final int PERMISSIONS_REQUEST_LOCATION = 123;
    private static  final int REQUEST_BLUETOOTH_SCAN_PERMISSION = 1;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice smartWatchDevice;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Location lastKnownLocation;

    private NotificationManager notificationManager;
    private boolean isMonitoringHeartRate = false;
    private int heartRate;
    private Ringtone alarmRingtone;
    private Uri notificationSound;
    private LocationManager locationManager;
    private String emergencyContactName;
    private static final String KEY_EMERGENCY_CONTACT_NAME = "emergency_contact_name";
    private static final String KEY_EMERGENCY_CONTACT_PHONE_NUMBER = "emergency_contact_phone_number";
    public static final String ACTION_I_NEED_HELP = "com.example.app.ACTION_I_NEED_HELP";

    public static final int MSG_HEART_RATE = 1;
    static final int NOTIFICATION_ID = 1;


    static final int PERMISSIONS_REQUEST_SMS = 123;

    private String emergencyContactPhoneNumber;
    private boolean isDemoMode = false;

    private TextView heartRateTextView;
    private TextView statusTextView;
    private HeartRateThread heartRateThread;
    private View.OnClickListener listener = null;

//    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    private Button startButton;
    private Button stopButton;
    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case MSG_HEART_RATE:
                    String heartRate = (String) message.obj;
                    heartRateTextView.setText(heartRate);
                    break;
            }
            return false;
        }
    });


    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getName().equals("SmartWatch")) {
                    smartWatchDevice = device;
                    bluetoothAdapter.cancelDiscovery();
                    connectToBluetoothDevice();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        heartRateTextView = findViewById(R.id.heartRateTextView);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        statusTextView = findViewById(R.id.statusTextView);
        Button demoButton;
        demoButton = findViewById(R.id.demoButton);

        // Set up Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Set up alarm ringtone
        alarmRingtone = RingtoneManager.getRingtone(getApplicationContext(), notificationSound);

        // Load emergency contact information from preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        emergencyContactName = sharedPreferences.getString(KEY_EMERGENCY_CONTACT_NAME, "");
        emergencyContactPhoneNumber = sharedPreferences.getString(KEY_EMERGENCY_CONTACT_PHONE_NUMBER, "");

        // Set up notification sound
        notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Set up location listener
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_LOCATION);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        }

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start Bluetooth discovery process
                if (bluetoothAdapter.startDiscovery()) {
                    Toast.makeText(getApplicationContext(), "Scanning for smartwatch...", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Bluetooth discovery process failed", Toast.LENGTH_SHORT).show();
                }
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Stop monitoring heart rate
                isMonitoringHeartRate = false;
                try {
                    bluetoothSocket.close();
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        // Set up Demo button
        demoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDemoMode = true;
                showNotification();
            }
        });
    }

    private void connectToBluetoothDevice() {
        // Establish Bluetooth connection
        try {
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            bluetoothSocket = smartWatchDevice.createInsecureRfcommSocketToServiceRecord(uuid);
            bluetoothSocket.connect();
            inputStream = bluetoothSocket.getInputStream();
            outputStream = bluetoothSocket.getOutputStream();

            // Start monitoring heart rate
            startHeartRateMonitoring();

            // Update UI
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    statusTextView.setText("Connected to Smartwatch");
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    statusTextView.setText("Connection Failed");
                }
            });
        }
    }

    public void openSettingsActivity(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }


    private void startHeartRateMonitoring() {
        // Start thread to read heart rate data from Bluetooth input stream
        heartRateThread = new HeartRateThread(inputStream, mHandler);
        heartRateThread.start();
        isMonitoringHeartRate = true;
        showToast("Monitoring heart rate");
    }

    private void stopMonitoringHeartRate() {
        // Stop thread reading heart rate data
        if (heartRateThread != null) {
            heartRateThread.stopReading();
            isMonitoringHeartRate = false;
        }
        showToast("Heart rate monitoring stopped");
    }

    private void sendSmsToEmergencyContact(String message) {
        if (emergencyContactPhoneNumber.isEmpty()) {
            showToast("No emergency contact phone number found");
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSIONS_REQUEST_SMS);
            return;
        }

        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(emergencyContactPhoneNumber, null, message, null, null);

        showToast("Emergency SMS sent to " + emergencyContactName);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        // Update last known location
        lastKnownLocation = location;
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

                if(requestCode==PERMISSIONS_REQUEST_LOCATION){
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
                } else {
                    Toast.makeText(this, "Location permission is required to use this app", Toast.LENGTH_SHORT).show();
                }

                if (requestCode == REQUEST_BLUETOOTH_SCAN_PERMISSION) {
                        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                            // Permission granted, start discovery
                            bluetoothAdapter.startDiscovery();
                        } else {
                            // Permission denied, show a message to the user
                            Toast.makeText(this, "Bluetooth scan permission denied", Toast.LENGTH_SHORT).show();
                        }
                }

                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }

    private void sendSMS(String phoneNumber, String message) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSIONS_REQUEST_SMS);
        } else {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(this, "SMS sent to " + phoneNumber, Toast.LENGTH_SHORT).show();
        }
    }

    private void showNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    "Heart rate alerts", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String emergencyContactName = sharedPreferences.getString(SettingsActivity.KEY_EMERGENCY_CONTACT_NAME, "");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_heart)
                .setContentTitle("Heart rate alert!")
                .setContentText("Your heart rate is above 120 BPM.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(notificationSound)
                .setAutoCancel(false);

        // Add action buttons to notification
        Intent intentOkay = new Intent(this, NotificationActionReceiver.class);
        intentOkay.setAction(ACTION_I_AM_OKAY);
        PendingIntent pendingIntentOkay = PendingIntent.getBroadcast(this, 0, intentOkay, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        builder.addAction(R.drawable.ic_check, "I am okay", pendingIntentOkay);

        Intent intentHelp = new Intent(this, NotificationActionReceiver.class);
        intentHelp.setAction(ACTION_I_NEED_HELP);
        PendingIntent pendingIntentHelp = PendingIntent.getBroadcast(this, 0, intentHelp, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        builder.addAction(R.drawable.ic_help, "I need help", pendingIntentHelp);

        // Add timer to notification
        Intent timerIntent = new Intent(this, NotificationTimerReceiver.class);
        timerIntent.putExtra("notificationId", NOTIFICATION_ID);
        PendingIntent pendingIntentTimer = PendingIntent.getBroadcast(this, 0, timerIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        long[] pattern = {0, 1000, 500, 1000, 500, 1000};
        builder.setVibrate(pattern);
        builder.setVibrate(pattern);

        builder.setLights(Color.RED, 500, 500);
        builder.setUsesChronometer(true);
        builder.setWhen(System.currentTimeMillis());
        builder.setShowWhen(true);
        builder.setUsesChronometer(true);
        builder.setChronometerCountDown(true);
        builder.setChronometerCountDown(true);
        builder.setUsesChronometer(true);
        builder.addAction(R.drawable.ic_help, "I need help", pendingIntentHelp);

        // Show notification
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }



    public class BluetoothDiscoveryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getName() != null && device.getName().equals("Smartwatch")) {
                    smartWatchDevice = device;
                    connectToBluetoothDevice();
                }
            }
        }
    }
}