package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String doctor = intent.getStringExtra("doctor");
        String time = intent.getStringExtra("datetime");

        Toast.makeText(context, "Reminder: Appointment with " + doctor + " at " + time, Toast.LENGTH_LONG).show();
    }
}
