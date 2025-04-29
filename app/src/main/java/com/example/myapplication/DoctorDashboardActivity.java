package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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

import java.util.Objects;

public class DoctorDashboardActivity extends AppCompatActivity {

    private LinearLayout appointmentListContainer;
    private ImageView imgMenu;
    private DatabaseReference doctorAppointmentsRef;
    private ValueEventListener appointmentsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_dashboard);
        EdgeToEdge.enable(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        appointmentListContainer = findViewById(R.id.appointmentListContainer);
        imgMenu = findViewById(R.id.imgMenu);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, DoctorLoginActivity.class));
            finish();
            return;
        }

        doctorAppointmentsRef = FirebaseDatabase.getInstance().getReference("doctorAppointments").child(currentUser.getUid());

        loadAppointments();
        setupMenu();
    }

    private void setupMenu() {
        imgMenu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, imgMenu);
            popup.getMenuInflater().inflate(R.menu.doctor_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.menu_set_availability) {
                    startActivity(new Intent(this, SetAvailabilityActivity.class));
                    return true;
                }else if (item.getItemId() == R.id.menu_view_availability) {
                    // 👇👇 Here you open DoctorAvailabilityActivity
                    Intent intent = new Intent(DoctorDashboardActivity.this, DoctorAvailabilityActivity.class);
                    startActivity(intent);
                    return true;
                }else if (item.getItemId() == R.id.menu_logout) {
                    if (appointmentsListener != null) {
                        doctorAppointmentsRef.removeEventListener(appointmentsListener);
                    }
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(this, DoctorLoginActivity.class));
                    finish();
                    return true;
                }
                return false;
            });

            popup.show();
        });
    }

    private void loadAppointments() {
        appointmentListContainer.removeAllViews();

        appointmentsListener = new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                appointmentListContainer.removeAllViews();

                if (snapshot.exists()) {
                    for (DataSnapshot snap : snapshot.getChildren()) {
                        String datetime = snap.child("datetime").getValue(String.class);
                        String status = snap.child("status").getValue(String.class);
                        String patientName = snap.child("patientName").getValue(String.class);
                        String userEmail = snap.child("userEmail").getValue(String.class);
                        String userId = snap.child("userId").getValue(String.class);
                        final String appointmentId = snap.getKey();

                        View card = LayoutInflater.from(DoctorDashboardActivity.this)
                                .inflate(R.layout.card_doctor_appointment, appointmentListContainer, false);

                        TextView txtPatient = card.findViewById(R.id.txtPatientName);
                        TextView txtTime = card.findViewById(R.id.txtAppointmentTime);
                        TextView txtStatus = card.findViewById(R.id.txtAppointmentStatus);
                        Button btnComplete = card.findViewById(R.id.btnMarkComplete);
                        Button btnCancel = card.findViewById(R.id.btnCancel);

                        txtTime.setText("Time: " + formatDateTime(datetime));
                        txtStatus.setText("Status: " + (status != null ? status : "Pending"));
                        styleStatusText(txtStatus, status);

                        String displayText = "";
                        if (patientName != null) {
                            displayText += "Patient: " + patientName;
                        }
                        if (userEmail != null) {
                            displayText += "\nEmail: " + userEmail;
                        }
                        txtPatient.setText(displayText);

                        btnComplete.setOnClickListener(v -> {
                            if (userId != null && appointmentId != null) {
                                updateAppointmentStatus(userId, appointmentId, "Completed");
                            } else {
                                Toast.makeText(DoctorDashboardActivity.this, "Invalid appointment details.", Toast.LENGTH_SHORT).show();
                            }
                        });

                        btnCancel.setOnClickListener(v -> {
                            if (userId != null && appointmentId != null) {
                                updateAppointmentStatus(userId, appointmentId, "Canceled by Doctor");
                            } else {
                                Toast.makeText(DoctorDashboardActivity.this, "Invalid appointment details.", Toast.LENGTH_SHORT).show();
                            }
                        });

                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        params.setMargins(0, 0, 0, 32);
                        card.setLayoutParams(params);

                        appointmentListContainer.addView(card);
                    }
                } else {
                    Toast.makeText(DoctorDashboardActivity.this, "No appointments found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DoctorDashboardActivity.this, "Failed to load appointments: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        doctorAppointmentsRef.addValueEventListener(appointmentsListener);
    }

    private void styleStatusText(TextView textView, String status) {
        if (status == null) return;
        if (status.equalsIgnoreCase("Completed")) {
            textView.setTextColor(Color.GREEN);
        } else if (status.toLowerCase().contains("canceled")) {
            textView.setTextColor(Color.RED);
        } else {
            textView.setTextColor(Color.parseColor("#FFA500")); // Orange
        }
    }

    private void updateAppointmentStatus(String userId, String appointmentId, String newStatus) {
        DatabaseReference doctorStatusRef = doctorAppointmentsRef.child(appointmentId).child("status");
        DatabaseReference userStatusRef = FirebaseDatabase.getInstance()
                .getReference("Appointments")
                .child(userId)
                .child(appointmentId)
                .child("status");

        doctorStatusRef.setValue(newStatus).addOnCompleteListener(task1 -> {
            if (task1.isSuccessful()) {
                userStatusRef.setValue(newStatus).addOnCompleteListener(task2 -> {
                    if (task2.isSuccessful()) {
                        Toast.makeText(DoctorDashboardActivity.this, "Status updated: " + newStatus, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(DoctorDashboardActivity.this, "User update failed: " + Objects.requireNonNull(task2.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(DoctorDashboardActivity.this, "Doctor update failed: " + Objects.requireNonNull(task1.getException()).getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String formatDateTime(String raw) {
        return raw != null ? raw.replace(" ", " at ") : "";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (appointmentsListener != null) {
            doctorAppointmentsRef.removeEventListener(appointmentsListener);
        }
    }
}
