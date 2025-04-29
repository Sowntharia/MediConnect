package com.example.myapplication;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String REMINDER_KEY = "reminders_enabled";
    private static final String CHANNEL_ID = "appointment_notifications";

    private final List<String> notificationHistory = new ArrayList<>();
    private SharedPreferences preferences;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(com.example.myapplication.R.layout.activity_notification);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(com.example.myapplication.R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button buttonEnable = findViewById(com.example.myapplication.R.id.buttonEnable);
        Button buttonDisable = findViewById(com.example.myapplication.R.id.buttonDisable);
        Button buttonBookingConfirmed = findViewById(com.example.myapplication.R.id.buttonBookingConfirmed);
        Button buttonReminder = findViewById(com.example.myapplication.R.id.buttonReminder);
        Button buttonRescheduled = findViewById(com.example.myapplication.R.id.buttonRescheduled);
        Button buttonCanceled = findViewById(com.example.myapplication.R.id.buttonCanceled);
        Button buttonViewHistory = findViewById(com.example.myapplication.R.id.buttonViewHistory);
        statusText = findViewById(com.example.myapplication.R.id.statusTextView);

        preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        createNotificationChannel();
        requestNotificationPermission();
        updateStatusText();

        buttonEnable.setOnClickListener(v -> {
            preferences.edit().putBoolean(REMINDER_KEY, true).apply();
            Toast.makeText(this, "Reminders Enabled", Toast.LENGTH_SHORT).show();
            updateStatusText();
        });

        buttonDisable.setOnClickListener(v -> {
            preferences.edit().putBoolean(REMINDER_KEY, false).apply();
            Toast.makeText(this, "Reminders Disabled", Toast.LENGTH_SHORT).show();
            updateStatusText();
        });

        buttonBookingConfirmed.setOnClickListener(v ->
                sendNotification(" Booking Confirmed", "Your appointment with Dr. Smith is confirmed for May 5, 4:00 PM.")
        );

        buttonReminder.setOnClickListener(v -> {
            if (preferences.getBoolean(REMINDER_KEY, true)) {
                scheduleReminderNotification();
            } else {
                Toast.makeText(this, "Reminders are disabled. Enable them first.", Toast.LENGTH_SHORT).show();
            }
        });

        buttonRescheduled.setOnClickListener(v ->
                sendNotification(" Appointment Rescheduled", "Your appointment has been rescheduled to May 6, 2:00 PM.")
        );

        buttonCanceled.setOnClickListener(v ->
                sendNotification(" Appointment Canceled", "Your appointment on May 5, 4:00 PM has been canceled.")
        );

        buttonViewHistory.setOnClickListener(v -> {
            if (notificationHistory.isEmpty()) {
                Toast.makeText(this, "No notifications yet", Toast.LENGTH_SHORT).show();
            } else {
                StringBuilder history = new StringBuilder();
                for (String entry : notificationHistory) {
                    history.append("• ").append(entry).append("\n\n");
                }

                new AlertDialog.Builder(this)
                        .setTitle("Notification History")
                        .setMessage(history.toString())
                        .setPositiveButton("OK", null)
                        .show();
            }
        });

        listenToMyAppointmentUpdates();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateStatusText() {
        boolean isEnabled = preferences.getBoolean(REMINDER_KEY, true);
        statusText.setText("Reminders are currently: " + (isEnabled ? "ON" : "OFF"));
    }

    private void sendNotification(String title, String message) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications) // ensure you have this drawable
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        notificationHistory.add(title + " – " + message);
    }

    private void scheduleReminderNotification() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Toast.makeText(this, "Permission to schedule exact alarms is not granted!", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    startActivity(intent);
                    return;
                }
            }

            long triggerTime = System.currentTimeMillis() + (5 * 1000L);

            Intent intent = new Intent(this, ReminderReceiver.class);
            intent.putExtra("title", " Appointment Reminder");
            intent.putExtra("message", "You have an appointment with your doctor soon.");

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);

            Toast.makeText(this, "Reminder scheduled in 5 seconds", Toast.LENGTH_SHORT).show();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Appointment Notifications";
            String description = "Notifications for appointment updates";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void listenToMyAppointmentUpdates() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String currentUserId = currentUser.getUid();
        DatabaseReference userAppointments = FirebaseDatabase.getInstance()
                .getReference("Appointments")
                .child(currentUserId);

        userAppointments.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String doctorName = snapshot.child("doctorName").getValue(String.class);
                String datetime = snapshot.child("datetime").getValue(String.class);
                String status = snapshot.child("status").getValue(String.class);

                if (doctorName != null && datetime != null && "Pending".equalsIgnoreCase(status)) {
                    sendNotification(" Booking Confirmed", "Your appointment with Dr. " + doctorName + " on " + datetime + " is confirmed.");
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String status = snapshot.child("status").getValue(String.class);

                if (status != null) {
                    switch (status.toLowerCase()) {
                        case "rescheduled":
                            sendNotification(" Appointment Rescheduled", "Your appointment has been rescheduled.");
                            break;
                        case "canceled":
                            sendNotification(" Appointment Canceled", "Your appointment has been canceled.");
                            break;
                        case "completed":
                            sendNotification(" Appointment Completed", "Thanks for visiting your doctor!");
                            break;
                    }
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(NotificationActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
