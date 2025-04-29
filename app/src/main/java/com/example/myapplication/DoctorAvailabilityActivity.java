package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DoctorAvailabilityActivity extends AppCompatActivity {

    private AvailabilityAdapter adapter;
    private final List<String> availabilityList = new ArrayList<>();
    private DatabaseReference availabilityRef;

    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadAvailability();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_doctor_availability);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            v.setPadding(
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            );
            return insets;
        });

        RecyclerView recyclerView = findViewById(R.id.recyclerViewAvailability);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AvailabilityAdapter(availabilityList);
        recyclerView.setAdapter(adapter);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, DoctorLoginActivity.class));
            finish();
            return;
        }

        String doctorId = auth.getCurrentUser().getUid();
        availabilityRef = FirebaseDatabase.getInstance().getReference("Doctors")
                .child(doctorId)
                .child("availableSlots");

        loadAvailability();
    }

    private void loadAvailability() {
        availabilityRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                availabilityList.clear();
                for (DataSnapshot dateSnap : snapshot.getChildren()) {
                    for (DataSnapshot slotSnap : dateSnap.getChildren()) {
                        String slot = dateSnap.getKey() + " " + slotSnap.getValue(String.class);
                        availabilityList.add(slot);
                    }
                }
                Collections.sort(availabilityList);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DoctorAvailabilityActivity.this, "Failed to load availability.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class AvailabilityAdapter extends RecyclerView.Adapter<AvailabilityAdapter.ViewHolder> {

        private final List<String> slots;

        AvailabilityAdapter(List<String> slots) {
            this.slots = slots;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.availability_slot_item, parent, false);
            return new ViewHolder(view);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String slot = slots.get(position);
            holder.txtSlot.setText(formatReadableDate(slot));

            holder.btnEdit.setOnClickListener(v -> {
                if (slot.contains(" ")) {
                    String[] parts = slot.split(" ", 2);
                    String oldDate = parts[0];
                    String oldTime = parts[1];

                    Intent intent = new Intent(DoctorAvailabilityActivity.this, EditAvailabilityActivity.class);
                    intent.putExtra("date", oldDate);
                    intent.putExtra("time", oldTime);
                    launcher.launch(intent);
                }
            });

            holder.btnDelete.setOnClickListener(v -> deleteSlot(slot));
        }

        @Override
        public int getItemCount() {
            return slots.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtSlot;
            ImageButton btnEdit, btnDelete;

            ViewHolder(View itemView) {
                super(itemView);
                txtSlot = itemView.findViewById(R.id.txtSlotTime);
                btnEdit = itemView.findViewById(R.id.btnEditSlot);
                btnDelete = itemView.findViewById(R.id.btnDeleteSlot);
            }
        }
    }

    private void deleteSlot(String slot) {
        if (slot.contains(" ")) {
            String[] parts = slot.split(" ", 2);
            String date = parts[0];
            String time = parts[1];

            availabilityRef.child(date).orderByValue().equalTo(time)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot slotSnap : snapshot.getChildren()) {
                                slotSnap.getRef().removeValue();
                            }
                            Toast.makeText(DoctorAvailabilityActivity.this, "Slot deleted.", Toast.LENGTH_SHORT).show();
                            loadAvailability();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(DoctorAvailabilityActivity.this, "Delete failed.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private String formatReadableDate(String raw) {
        try {
            SimpleDateFormat original = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            SimpleDateFormat formatted = new SimpleDateFormat("EEE, MMM dd yyyy 'at' hh:mm a", Locale.getDefault());
            Date date = original.parse(raw);
            assert date != null;
            return formatted.format(date);
        } catch (ParseException e) {
            return raw;
        }
    }
}
