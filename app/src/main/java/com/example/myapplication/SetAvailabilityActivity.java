package com.example.myapplication;

import android.os.Bundle;
import android.widget.*;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class SetAvailabilityActivity extends AppCompatActivity {

    private Button btnPickDates, btnAddTimeSlot, btnSave;
    private TextView txtDateCount;
    private EditText inputTimeSlot;
    private ListView listViewDates, listViewTimeSlots;

    private final List<String> selectedDates = new ArrayList<>();
    private final Map<String, List<String>> dateToSlotsMap = new HashMap<>();
    private final List<String> timeSlots = new ArrayList<>();

    private ArrayAdapter<String> dateAdapter;
    private ArrayAdapter<String> slotAdapter;

    private String currentDate = "";
    private DatabaseReference availabilityRef;
    private String doctorUID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(com.example.myapplication.R.layout.activity_set_availability);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(com.example.myapplication.R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnPickDates = findViewById(com.example.myapplication.R.id.btnPickDates);
        btnAddTimeSlot = findViewById(com.example.myapplication.R.id.btnAddTimeSlot);
        btnSave = findViewById(com.example.myapplication.R.id.btnSaveAvailability);
        txtDateCount = findViewById(com.example.myapplication.R.id.txtDateCount);
        inputTimeSlot = findViewById(com.example.myapplication.R.id.inputTimeSlot);
        listViewDates = findViewById(com.example.myapplication.R.id.listViewDates);
        listViewTimeSlots = findViewById(R.id.listViewTimeSlots);

        doctorUID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        availabilityRef = FirebaseDatabase.getInstance().getReference("Doctors")
                .child(doctorUID).child("availableSlots");

        dateAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, selectedDates);
        slotAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, timeSlots);

        listViewDates.setAdapter(dateAdapter);
        listViewTimeSlots.setAdapter(slotAdapter);

        btnPickDates.setOnClickListener(v -> openMultiDatePicker());

        listViewDates.setOnItemClickListener((parent, view, position, id) -> {
            currentDate = selectedDates.get(position);
            timeSlots.clear();
            timeSlots.addAll(dateToSlotsMap.getOrDefault(currentDate, new ArrayList<>()));
            slotAdapter.notifyDataSetChanged();
            Toast.makeText(this, "Editing slots for " + currentDate, Toast.LENGTH_SHORT).show();
        });

        btnAddTimeSlot.setOnClickListener(v -> {
            if (currentDate.isEmpty()) {
                Toast.makeText(this, "Select a date from the list first", Toast.LENGTH_SHORT).show();
                return;
            }

            String time = inputTimeSlot.getText().toString().trim();
            if (!time.isEmpty()) {
                timeSlots.add(time);
                slotAdapter.notifyDataSetChanged();
                inputTimeSlot.setText("");

                dateToSlotsMap.put(currentDate, new ArrayList<>(timeSlots));
            } else {
                Toast.makeText(this, "Enter a valid time slot", Toast.LENGTH_SHORT).show();
            }
        });

        btnSave.setOnClickListener(v -> {
            int dateCount = dateToSlotsMap.size();
            if (dateCount < 2 || dateCount > 4) {
                Toast.makeText(this, "Please select between 2 and 4 different dates", Toast.LENGTH_SHORT).show();
                return;
            }


            for (Map.Entry<String, List<String>> entry : dateToSlotsMap.entrySet()) {
                int slotCount = entry.getValue().size();
                if (slotCount < 2 || slotCount > 4) {
                    Toast.makeText(this, "Each date must have between 2 and 4 time slots", Toast.LENGTH_SHORT).show();
                    return;
                }
            }


            availabilityRef.setValue(dateToSlotsMap).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Availability saved successfully!", Toast.LENGTH_SHORT).show();
                    resetAll();
                } else {
                    Toast.makeText(this, "Failed to save availability", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void openMultiDatePicker() {
        MaterialDatePicker.Builder<androidx.core.util.Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker();
        builder.setTitleText("Select Availability Range");

        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker = builder.build();
        picker.show(getSupportFragmentManager(), "DATE_PICKER");

        picker.addOnPositiveButtonClickListener(selection -> {
            selectedDates.clear();
            dateToSlotsMap.clear();
            currentDate = "";

            long start = selection.first;
            long end = selection.second;

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(start);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            while (calendar.getTimeInMillis() <= end) {
                String date = sdf.format(calendar.getTime());
                selectedDates.add(date);
                calendar.add(Calendar.DATE, 1);
            }

            txtDateCount.setText("Dates selected: " + selectedDates.size());
            dateAdapter.notifyDataSetChanged();
            slotAdapter.clear();
        });
    }

    private void resetAll() {
        selectedDates.clear();
        timeSlots.clear();
        dateToSlotsMap.clear();
        currentDate = "";
        dateAdapter.notifyDataSetChanged();
        slotAdapter.notifyDataSetChanged();
        txtDateCount.setText("Dates selected: 0");
    }
}
