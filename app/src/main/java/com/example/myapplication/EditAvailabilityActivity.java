package com.example.myapplication;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class EditAvailabilityActivity extends AppCompatActivity {

    private EditText edtDate, edtTime;
    private DatabaseReference availabilityRef;

    private String oldDate, oldTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_availability);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        edtDate = findViewById(R.id.edtDate);
        edtTime = findViewById(R.id.edtTime);
        Button btnUpdate = findViewById(R.id.btnUpdate);

        oldDate = getIntent().getStringExtra("date");
        oldTime = getIntent().getStringExtra("time");

        edtDate.setText(oldDate);
        edtTime.setText(oldTime);

        String doctorId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        availabilityRef = FirebaseDatabase.getInstance().getReference("Doctors").child(doctorId).child("availableSlots");

        edtDate.setOnClickListener(v -> pickDate());
        edtTime.setOnClickListener(v -> pickTime());

        btnUpdate.setOnClickListener(v -> updateSlot());
    }

    private void pickDate() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            edtDate.setText(sdf.format(calendar.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(System.currentTimeMillis());
        dialog.show();
    }

    private void pickTime() {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            edtTime.setText(sdf.format(calendar.getTime()));
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
        dialog.show();
    }

    private void updateSlot() {
        String newDate = edtDate.getText().toString();
        String newTime = edtTime.getText().toString();

        if (newDate.isEmpty() || newTime.isEmpty()) {
            Toast.makeText(this, "Please select date and time", Toast.LENGTH_SHORT).show();
            return;
        }

        availabilityRef.child(oldDate).orderByValue().equalTo(oldTime)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DataSnapshot snap : snapshot.getChildren()) {
                        snap.getRef().removeValue(); // delete old slot
                    }
                    availabilityRef.child(newDate).push().setValue(newTime)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Availability updated!", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);  // Very important for auto-refresh!
                                finish(); // Close this activity and return
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete old slot: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
