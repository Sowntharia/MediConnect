package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UserProfileActivity extends AppCompatActivity {

    private ImageView imgMenu;
    private LinearLayout appointmentContainer;
    private DatabaseReference ref;
    private ValueEventListener appointmentListener;
    private ActivityResultLauncher<Intent> rescheduleLauncher;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        imgMenu = findViewById(R.id.imgMenu);
        ImageView imgLogout = findViewById(R.id.imgLogout);
        TextView txtWelcome = findViewById(R.id.txtWelcome);
        appointmentContainer = findViewById(R.id.appointmentContainer);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String fullName = getIntent().getStringExtra("fullName");
        txtWelcome.setText("Welcome, " + (fullName != null ? fullName : "User"));

        rescheduleLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null &&
                            result.getData().getBooleanExtra("rescheduled", false)) {
                        loadAppointments();
                    }
                });

        imgLogout.setOnClickListener(v -> logout());

        imgMenu.setOnClickListener(v -> showPopupMenu());

        loadAppointments();
    }

    private void logout() {
        if (ref != null && appointmentListener != null) {
            ref.removeEventListener(appointmentListener);
        }
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showPopupMenu() {
        PopupMenu popup = new PopupMenu(this, imgMenu);
        popup.getMenuInflater().inflate(R.menu.user_profile_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_change_password) {
                startActivity(new Intent(this, ChangePasswordActivity.class));
                return true;
            } else if (item.getItemId() == R.id.menu_notification) {
                startActivity(new Intent(this, NotificationActivity.class));
                return true;
            } else if (item.getItemId() == R.id.menu_appointment) {
                startActivity(new Intent(this, BookAppointmentActivity.class));
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void loadAppointments() {
        String uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        ref = FirebaseDatabase.getInstance().getReference("Appointments").child(uid);

        appointmentListener = new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                appointmentContainer.removeAllViews();
                List<DataSnapshot> appointments = new ArrayList<>();

                for (DataSnapshot snap : snapshot.getChildren()) {
                    appointments.add(snap);
                }

                if (appointments.isEmpty()) {
                    TextView none = new TextView(UserProfileActivity.this);
                    none.setText("No appointments found.");
                    appointmentContainer.addView(none);
                } else {
                    appointments.sort((a, b) -> {
                        String timeA = a.child("datetime").getValue(String.class);
                        String timeB = b.child("datetime").getValue(String.class);
                        return (timeB != null && timeA != null) ? timeB.compareTo(timeA) : 0;
                    });

                    for (DataSnapshot snap : appointments) {
                        addAppointmentCard(snap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                    Toast.makeText(UserProfileActivity.this, "Failed to load appointments", Toast.LENGTH_SHORT).show();
                }
            }
        };

        ref.addValueEventListener(appointmentListener);
    }

    @SuppressLint("SetTextI18n")
    private void addAppointmentCard(DataSnapshot snap) {
        String appointmentId = snap.getKey();
        String department = snap.child("department").getValue(String.class);
        String datetime = snap.child("datetime").getValue(String.class);
        String doctorId = snap.child("doctorId").getValue(String.class);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.card_background);
        card.setPadding(30, 30, 30, 30);

        TextView txtInfo = new TextView(this);
        txtInfo.setText("Loading doctor info...");
        txtInfo.setTextSize(16f);
        card.addView(txtInfo);

        if (doctorId == null) {
            txtInfo.setText("Doctor ID missing\nDepartment: " + department + "\nDate and Time: " + datetime);
            appointmentContainer.addView(card);
            return;
        }

        FirebaseDatabase.getInstance().getReference("Doctors").child(doctorId).child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshotDoctor) {
                        String doctorName = snapshotDoctor.getValue(String.class);
                        String status = snap.child("status").getValue(String.class);

                        txtInfo.setText("\nDoctor: " + (doctorName != null ? doctorName : "Unknown") +
                                "\nDepartment: " + department +
                                "\nDate and Time: " + datetime +
                                "\nStatus: " + status);

                        if ("Pending".equalsIgnoreCase(status) || "Canceled by Doctor".equalsIgnoreCase(status)) {
                            Button btnReschedule = createRescheduleButton(appointmentId, doctorId, doctorName, department, datetime);
                            card.addView(btnReschedule);
                        }

                        if ("Pending".equalsIgnoreCase(status)) {
                            Button btnCancel = new Button(UserProfileActivity.this);
                            btnCancel.setText("Cancel Appointment");
                            btnCancel.setOnClickListener(v -> cancelAppointment(appointmentId, doctorId));
                            card.addView(btnCancel);
                        }

                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        params.setMargins(0, 0, 0, 30);
                        card.setLayoutParams(params);

                        appointmentContainer.addView(card);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        txtInfo.setText("Doctor info unavailable\nDepartment: " + department + "\nDate and Time: " + datetime);
                    }
                });
    }

    @SuppressLint("SetTextI18n")
    private Button createRescheduleButton(String appointmentId, String doctorId, String doctorName, String department, String datetime) {
        Button btnReschedule = new Button(this);
        btnReschedule.setText("Reschedule Appointment");
        btnReschedule.setOnClickListener(v -> {
            Intent intent = new Intent(this, RescheduleActivity.class);
            intent.putExtra("appointmentId", appointmentId);
            intent.putExtra("doctor", doctorId);
            intent.putExtra("doctorName", doctorName);
            intent.putExtra("department", department);
            intent.putExtra("datetime", datetime);
            rescheduleLauncher.launch(intent);
        });
        return btnReschedule;
    }

    private void cancelAppointment(String appointmentId, String doctorId) {
        String uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        DatabaseReference userAppRef = FirebaseDatabase.getInstance()
                .getReference("Appointments").child(uid).child(appointmentId).child("status");
        DatabaseReference doctorAppRef = FirebaseDatabase.getInstance()
                .getReference("doctorAppointments").child(doctorId).child(appointmentId).child("status");

        userAppRef.setValue("Canceled");
        doctorAppRef.setValue("Canceled");

        Toast.makeText(this, "Appointment canceled successfully", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ref != null && appointmentListener != null) {
            ref.removeEventListener(appointmentListener);
        }
    }
}
