package com.example.myapplication;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat.Type;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class RescheduleActivity extends AppCompatActivity {

    private Button selectDateButton;
    private RadioGroup filterGroup;
    private ProgressBar slotLoading;
    private final Calendar selectedDate = Calendar.getInstance();
    private DatabaseReference dbRef;
    private ValueEventListener slotListener;

    private String appointmentId;
    private String doctorId;
    private String oldDatetime;
    private String selectedSlot = "";
    private SlotAdapter slotAdapter;
    private final List<String> availableSlots = new ArrayList<>();
    private final List<String> filteredSlots = new ArrayList<>();

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reschedule);

        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView currentAppointment = findViewById(R.id.currentAppointment);
        selectDateButton = findViewById(R.id.selectDateButton);
        GridView gridViewSlots = findViewById(R.id.gridViewSlots);
        filterGroup = findViewById(R.id.filterGroup);
        slotLoading = findViewById(R.id.slotLoading);
        Button rescheduleButton = findViewById(R.id.rescheduleButton);

        appointmentId = getIntent().getStringExtra("appointmentId");
        doctorId = getIntent().getStringExtra("doctor");
        String doctorName = getIntent().getStringExtra("doctorName");
        String department = getIntent().getStringExtra("department");
        oldDatetime = getIntent().getStringExtra("datetime");

        if (appointmentId == null || doctorId == null || department == null || oldDatetime == null || doctorName == null) {
            Toast.makeText(this, "Missing appointment data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        dbRef = FirebaseDatabase.getInstance().getReference();

        currentAppointment.setText(
                "Rescheduling:\nDoctor: " + doctorName +
                        "\nDepartment: " + department +
                        "\nDate and Time: " + oldDatetime
        );

        slotAdapter = new SlotAdapter(this, filteredSlots);
        gridViewSlots.setAdapter(slotAdapter);

        slotAdapter.setOnSlotSelectedListener(slot -> {
            selectedSlot = slot;
            Toast.makeText(this, "Selected Slot: " + slot, Toast.LENGTH_SHORT).show();
        });

        selectDateButton.setOnClickListener(v -> pickDate());

        rescheduleButton.setOnClickListener(v -> {
            if (selectedSlot.isEmpty()) {
                Toast.makeText(this, "Please select a time slot", Toast.LENGTH_SHORT).show();
                return;
            }
            checkSlotConflictAndConfirm(uid);
        });

        filterGroup.setOnCheckedChangeListener((group, checkedId) -> applyTimeOfDayFilter());
    }

    private void pickDate() {
        DatePickerDialog datePicker = new DatePickerDialog(this,
                (view, year, month, day) -> {
                    selectedDate.set(Calendar.YEAR, year);
                    selectedDate.set(Calendar.MONTH, month);
                    selectedDate.set(Calendar.DAY_OF_MONTH, day);
                    String formattedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.getTime());
                    selectDateButton.setText(formattedDate);
                    loadAvailableSlots(formattedDate);
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH));
        datePicker.getDatePicker().setMinDate(System.currentTimeMillis());
        datePicker.show();
    }

    private void loadAvailableSlots(String date) {
        slotLoading.setVisibility(View.VISIBLE);

        DatabaseReference slotRef = dbRef.child("Doctors").child(doctorId).child("availableSlots").child(date);

        if (slotListener != null) {
            slotRef.removeEventListener(slotListener);
        }

        slotListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                availableSlots.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String slot = snap.getValue(String.class);
                    if (slot != null) {
                        availableSlots.add(slot);
                    }
                }

                // Fetch booked slots for the same date
                dbRef.child("doctorAppointments").child(doctorId)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                Set<String> bookedSlots = new HashSet<>();
                                for (DataSnapshot snap : snapshot.getChildren()) {
                                    String datetime = snap.child("datetime").getValue(String.class);
                                    if (datetime != null && datetime.startsWith(date)) {
                                        bookedSlots.add(datetime.split(" ")[1]);
                                    }
                                }

                                applyTimeOfDayFilter(); // this filters visible slots
                                slotAdapter.setDisabledSlots(bookedSlots); // disable already booked
                                slotLoading.setVisibility(View.GONE);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                slotLoading.setVisibility(View.GONE);
                                Log.e("FirebaseError", "Failed to load booked slots: " + error.getMessage());
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                slotLoading.setVisibility(View.GONE);
                Log.e("FirebaseError", error.getMessage());
            }
        };

        slotRef.addListenerForSingleValueEvent(slotListener);
    }


    private void applyTimeOfDayFilter() {
        filteredSlots.clear();
        int checkedId = filterGroup.getCheckedRadioButtonId();

        for (String slot : availableSlots) {
            if (checkedId == R.id.filterMorning && isMorningSlot(slot)) {
                filteredSlots.add(slot);
            } else if (checkedId == R.id.filterEvening && !isMorningSlot(slot)) {
                filteredSlots.add(slot);
            } else if (checkedId == R.id.filterAll) {
                filteredSlots.add(slot);
            }
        }

        slotAdapter.notifyDataSetChanged();
    }

    private boolean isMorningSlot(String slot) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date slotTime = sdf.parse(slot);
            if (slotTime == null) return false;

            Calendar cal = Calendar.getInstance();
            cal.setTime(slotTime);
            return cal.get(Calendar.HOUR_OF_DAY) < 12;
        } catch (Exception e) {
            return false;
        }
    }

    private void checkSlotConflictAndConfirm(String uid) {
        String newDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.getTime());
        String newDatetime = newDate + " " + selectedSlot;

        if (newDatetime.equals(oldDatetime)) {
            Toast.makeText(this, "You selected the same time. Please choose a new slot.", Toast.LENGTH_SHORT).show();
            return;
        }

        dbRef.child("doctorAppointments").child(doctorId)
                .orderByChild("datetime").equalTo(newDatetime)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            new AlertDialog.Builder(RescheduleActivity.this)
                                    .setTitle("Conflict")
                                    .setMessage("This slot is already booked. Please select another.")
                                    .setPositiveButton("OK", null)
                                    .show();
                        } else {
                            confirmReschedule(uid, newDatetime);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(RescheduleActivity.this, "Failed to check slot", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void confirmReschedule(String uid, String newDatetime) {
        DatabaseReference userAppRef = dbRef.child("Appointments").child(uid).child(appointmentId).child("datetime");
        DatabaseReference doctorAppRef = dbRef.child("doctorAppointments").child(doctorId).child(appointmentId).child("datetime");

        userAppRef.setValue(newDatetime);
        doctorAppRef.setValue(newDatetime);

        Toast.makeText(this, "Appointment rescheduled", Toast.LENGTH_SHORT).show();

        Intent resultIntent = new Intent();
        resultIntent.putExtra("rescheduled", true);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (slotListener != null) {
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.getTime());
            dbRef.child("Doctors").child(doctorId).child("availableSlots").child(date)
                    .removeEventListener(slotListener);
        }
    }
}
