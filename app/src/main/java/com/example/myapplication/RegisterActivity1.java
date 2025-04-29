package com.example.myapplication;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class RegisterActivity1 extends AppCompatActivity {

    EditText inputFullName, inputEmail, inputDOB, inputMobile;
    RadioGroup radioGroupGender;
    Button btnNextPage;

    Calendar dobCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register1);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        inputFullName = findViewById(R.id.inputFullName);
        inputEmail = findViewById(R.id.inputEmail);
        inputDOB = findViewById(R.id.inputDOB);
        inputMobile = findViewById(R.id.inputMobile);
        radioGroupGender = findViewById(R.id.radioGroupGender);
        btnNextPage = findViewById(R.id.btnNextPage);

        inputDOB.setOnClickListener(v -> openDatePicker());

        btnNextPage.setOnClickListener(v -> {
            String fullName = inputFullName.getText().toString().trim();
            String email = inputEmail.getText().toString().trim();
            String dob = inputDOB.getText().toString().trim();
            String mobile = inputMobile.getText().toString().trim();

            int selectedId = radioGroupGender.getCheckedRadioButtonId();
            RadioButton selectedGenderBtn = findViewById(selectedId);
            String gender = (selectedGenderBtn != null) ? selectedGenderBtn.getText().toString() : "";

            boolean hasError = false;

            if (fullName.isEmpty()) {
                inputFullName.setError("Please enter your full name");
                hasError = true;
            }

            if (email.isEmpty()) {
                inputEmail.setError("Please enter your email");
                hasError = true;
            } else if (!email.matches("^[a-z]+[a-z0-9._]*@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$")) {
                inputEmail.setError("Email must start with lowercase, contain one @, and valid domain");
                hasError = true;
            }

            if (dob.isEmpty()) {
                inputDOB.setError("Please select date of birth");
                hasError = true;
            }

            if (gender.isEmpty()) {
                RadioButton radioButton = new RadioButton(this);
                radioButton.setError("Select gender");
                hasError = true;
            }

            if (mobile.isEmpty()) {
                inputMobile.setError("Please enter mobile number");
                hasError = true;
            } else if (!mobile.matches("^\\d{10}$")) {
                inputMobile.setError("Mobile number must be exactly 10 digits");
                hasError = true;
            }

            if (hasError) return;

            // All validations passed, proceed to RegisterActivity2
            Intent intent = new Intent(RegisterActivity1.this, RegisterActivity2.class);
            intent.putExtra("fullName", fullName);
            intent.putExtra("email", email);
            intent.putExtra("dob", dob);
            intent.putExtra("gender", gender);
            intent.putExtra("mobile", mobile);
            startActivity(intent);
        });
    }

    private void openDatePicker() {
        int year = dobCalendar.get(Calendar.YEAR);
        int month = dobCalendar.get(Calendar.MONTH);
        int day = dobCalendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    dobCalendar.set(Calendar.YEAR, selectedYear);
                    dobCalendar.set(Calendar.MONTH, selectedMonth);
                    dobCalendar.set(Calendar.DAY_OF_MONTH, selectedDay);
                    updateDOBField();
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    private void updateDOBField() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        inputDOB.setText(sdf.format(dobCalendar.getTime()));
    }
}
