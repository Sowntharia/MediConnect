package com.example.myapplication;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.*;

public class BookAppointmentActivity extends AppCompatActivity {

    Spinner spinnerDepartment, spinnerDoctor;
    Button btnPickDate, btnConfirm;
    TextView txtSelectedDate;
    ListView listViewSlots;

    DatabaseReference dbRef;
    String selectedDepartment = "", selectedDoctorId = "", selectedDoctorName = "";
    String selectedDate = "", selectedSlot = "";
    List<String> departments = new ArrayList<>();
    List<String> doctorNames = new ArrayList<>();
    Map<String, String> doctorMap = new HashMap<>();
    List<String> timeSlots = new ArrayList<>();
    ArrayAdapter<String> slotAdapter;

    ValueEventListener departmentListener;
    ValueEventListener doctorListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_appointment);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        spinnerDepartment = findViewById(R.id.spinnerDepartment);
        spinnerDoctor = findViewById(R.id.spinnerDoctor);
        btnPickDate = findViewById(R.id.btnPickDate);
        btnConfirm = findViewById(R.id.btnConfirmAppointment);
        txtSelectedDate = findViewById(R.id.txtSelectedDate);
        listViewSlots = findViewById(R.id.listViewSlots);

        dbRef = FirebaseDatabase.getInstance().getReference();

        loadDepartmentsRealTime();

        spinnerDepartment.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != 0) {
                    selectedDepartment = departments.get(position);
                    loadDoctorsRealTime(selectedDepartment);
                } else {
                    doctorNames.clear();
                    doctorMap.clear();
                    doctorNames.add("Select Doctor");
                    spinnerDoctor.setAdapter(new ArrayAdapter<>(BookAppointmentActivity.this,
                            android.R.layout.simple_spinner_dropdown_item, doctorNames));
                }
            }

            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerDoctor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != 0) {
                    selectedDoctorName = doctorNames.get(position);
                    selectedDoctorId = doctorMap.get(selectedDoctorName);
                } else {
                    selectedDoctorName = "";
                    selectedDoctorId = "";
                }
            }

            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnPickDate.setOnClickListener(v -> openDatePicker());

        listViewSlots.setOnItemClickListener((parent, view, position, id) -> {
            selectedSlot = timeSlots.get(position);
            Toast.makeText(this, "Selected Slot: " + selectedSlot, Toast.LENGTH_SHORT).show();
        });

        btnConfirm.setOnClickListener(v -> confirmAppointment());
    }

    private void loadDepartmentsRealTime() {
        departmentListener = dbRef.child("Doctors").addValueEventListener(new ValueEventListener() {
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Set<String> deptSet = new HashSet<>();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String dep = snap.child("department").getValue(String.class);
                    if (dep != null && !dep.isEmpty()) {
                        deptSet.add(dep);
                    }
                }
                departments.clear();
                departments.addAll(deptSet);
                departments.add(0, "Select Department");

                spinnerDepartment.setAdapter(new ArrayAdapter<>(BookAppointmentActivity.this,
                        android.R.layout.simple_spinner_dropdown_item, departments));
            }

            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(BookAppointmentActivity.this, "Failed to load departments", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDoctorsRealTime(String department) {
        if (doctorListener != null) {
            dbRef.child("Doctors").removeEventListener(doctorListener);
        }

        doctorListener = dbRef.child("Doctors").addValueEventListener(new ValueEventListener() {
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                doctorNames.clear();
                doctorMap.clear();

                for (DataSnapshot snap : snapshot.getChildren()) {
                    String name = snap.child("name").getValue(String.class);
                    String dep = snap.child("department").getValue(String.class);
                    String doctorUID = snap.getKey();

                    if (name == null || dep == null || doctorUID == null) continue;

                    if (dep.equalsIgnoreCase(department)) {
                        doctorNames.add(name);
                        doctorMap.put(name, doctorUID);
                    }
                }

                doctorNames.add(0, "Select Doctor");
                spinnerDoctor.setAdapter(new ArrayAdapter<>(BookAppointmentActivity.this,
                        android.R.layout.simple_spinner_dropdown_item, doctorNames));
            }

            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(BookAppointmentActivity.this, "Failed to load doctors", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (departmentListener != null) {
            dbRef.child("Doctors").removeEventListener(departmentListener);
        }
        if (doctorListener != null) {
            dbRef.child("Doctors").removeEventListener(doctorListener);
        }
    }

    private void openDatePicker() {
        Calendar calendar = Calendar.getInstance();
        @SuppressLint("SetTextI18n")
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            selectedDate = sdf.format(calendar.getTime());
            txtSelectedDate.setText("Selected Date: " + selectedDate);
            loadSlotsForDate();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        dialog.getDatePicker().setMinDate(System.currentTimeMillis());
        dialog.show();
    }

    private void loadSlotsForDate() {
        if (selectedDoctorId.isEmpty() || selectedDate.isEmpty()) return;

        dbRef.child("Doctors").child(selectedDoctorId).child("availableSlots")
                .child(selectedDate).addListenerForSingleValueEvent(new ValueEventListener() {
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        timeSlots.clear();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            String slot = snap.getValue(String.class);
                            if (slot != null) {
                                timeSlots.add(slot);
                            }
                        }
                        slotAdapter = new ArrayAdapter<>(BookAppointmentActivity.this,
                                android.R.layout.simple_list_item_1, timeSlots);
                        listViewSlots.setAdapter(slotAdapter);
                    }

                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(BookAppointmentActivity.this, "Failed to load slots", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private Map<String, Object> buildAppointment(
            String userId,
            String doctorId,
            String doctorName,
            String department,
            String datetime,
            String patientName
    ) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("doctorId", doctorId);
        map.put("doctorName", doctorName);
        map.put("department", department);
        map.put("datetime", datetime);
        map.put("status", "Pending");
        map.put("patientName", patientName);
        return map;
    }

    private void confirmAppointment() {
        if (selectedDoctorId.isEmpty() || selectedDate.isEmpty() || selectedSlot.isEmpty()) {
            Toast.makeText(this, "Please select all fields (Department, Doctor, Date, Slot)", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in. Please login again.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String userId = currentUser.getUid();
        String datetime = selectedDate + " " + selectedSlot;

        Toast.makeText(this, "Checking availability...", Toast.LENGTH_SHORT).show();

        // Check if slot already booked
        dbRef.child("doctorAppointments").child(selectedDoctorId)
                .orderByChild("datetime").equalTo(datetime)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Toast.makeText(BookAppointmentActivity.this, "Slot already booked. Please select another slot.", Toast.LENGTH_LONG).show();
                        } else {
                            // Fetch Patient Name

                            dbRef.child("Users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot userSnap) {
                                    //Correct field name
                                    final String fullName = userSnap.child("fullName").getValue(String.class);
                                    final String patientName = (fullName == null || fullName.isEmpty()) ? "Unknown Patient" : fullName;

                                    String appointmentId = dbRef.push().getKey();
                                    if (appointmentId == null) {
                                        Toast.makeText(BookAppointmentActivity.this, "Failed to generate appointment ID", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    Map<String, Object> appointment = buildAppointment(
                                            userId, selectedDoctorId, selectedDoctorName,
                                            selectedDepartment, datetime, patientName
                                    );

                                    // Save appointment under both paths
                                    Map<String, Object> updates = new HashMap<>();
                                    updates.put("Appointments/" + userId + "/" + appointmentId, appointment);
                                    updates.put("doctorAppointments/" + selectedDoctorId + "/" + appointmentId, appointment);

                                    dbRef.updateChildren(updates)
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(BookAppointmentActivity.this, "Appointment booked successfully!", Toast.LENGTH_SHORT).show();
                                                finish();
                                            })
                                            .addOnFailureListener(e -> Toast.makeText(BookAppointmentActivity.this, "Failed to book appointment: " + e.getMessage(), Toast.LENGTH_LONG).show());
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(BookAppointmentActivity.this, "Failed to fetch patient name: " + error.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(BookAppointmentActivity.this, "Error checking slot availability: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }


}
