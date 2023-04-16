package com.example.heartwatch;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import android.os.Handler;


public class HeartRateThread extends Thread {
    private InputStream inputStream;
    private Handler mHandler;
    private volatile boolean running = true;

    private static final int MSG_HEART_RATE = 0;


    public HeartRateThread(InputStream inputStream, Handler handler) {
        this.inputStream = inputStream;
        this.mHandler = handler;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        int bytes;

        while (running) {
            try {
                // Read from the InputStream
                bytes = inputStream.read(buffer);
                String heartRate = new String(buffer, 0, bytes, Charset.forName("UTF_8"));

                // Update the UI with the heart rate
                mHandler.obtainMessage(MSG_HEART_RATE, heartRate).sendToTarget();

            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }
    public void stopReading(){
        running=false;
    }
}
