package com.example.myapplication;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.MotionEvent;
import android.widget.*;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.*;

public class DoctorRegisterActivity extends AppCompatActivity {

    private EditText inputName, inputEmail, inputPassword, inputConfirmPassword;
    private Spinner spinnerDepartment;
    private Button btnRegister;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;

    private boolean showPassword = false;
    private boolean showConfirmPassword = false;

    private final String[] allDepartments = {"Cardiology", "Dermatology", "ENT", "General Physician", "Pediatric"};
    private List<String> availableDepartments = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(com.example.myapplication.R.layout.activity_doctor_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(com.example.myapplication.R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        inputName = findViewById(com.example.myapplication.R.id.inputName);
        inputEmail = findViewById(com.example.myapplication.R.id.inputEmail);
        inputPassword = findViewById(com.example.myapplication.R.id.inputPassword);
        inputConfirmPassword = findViewById(com.example.myapplication.R.id.inputConfirmPassword);
        spinnerDepartment = findViewById(com.example.myapplication.R.id.spinnerDepartment);
        btnRegister = findViewById(com.example.myapplication.R.id.btnRegister);
        progressBar = findViewById(com.example.myapplication.R.id.progressBar);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference();

        fetchAvailableDepartments();
        addPasswordToggle(inputPassword, true);
        addPasswordToggle(inputConfirmPassword, false);

        btnRegister.setOnClickListener(v -> {
            progressBar.setVisibility(ProgressBar.VISIBLE);
            btnRegister.setEnabled(false);
            registerDoctor();
        });
    }

    private void fetchAvailableDepartments() {
        DatabaseReference deptRef = dbRef.child("Departments");
        deptRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                availableDepartments = new ArrayList<>(Arrays.asList(allDepartments));

                for (DataSnapshot snap : snapshot.getChildren()) {
                    String taken = snap.getKey();
                    availableDepartments.remove(taken);
                }

                if (availableDepartments.isEmpty()) {
                    Toast.makeText(DoctorRegisterActivity.this, "All departments already assigned!", Toast.LENGTH_LONG).show();
                    btnRegister.setEnabled(false);
                } else {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(DoctorRegisterActivity.this,
                            android.R.layout.simple_spinner_item, availableDepartments);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerDepartment.setAdapter(adapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DoctorRegisterActivity.this, "Failed to load departments: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registerDoctor() {
        String name = inputName.getText().toString().trim();
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        String confirmPassword = inputConfirmPassword.getText().toString().trim();
        String department = spinnerDepartment.getSelectedItem() != null ? spinnerDepartment.getSelectedItem().toString() : "";

        if (validateInputs(name, email, password, confirmPassword, department)) {
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                resetUI();
                if (task.isSuccessful()) {
                    String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

                    Map<String, Object> doctorData = new HashMap<>();
                    doctorData.put("name", name);
                    doctorData.put("email", email);
                    doctorData.put("department", department);
                    doctorData.put("role", "doctor");

                    dbRef.child("Doctors").child(uid).setValue(doctorData);
                    dbRef.child("Users").child(uid).setValue(doctorData);
                    dbRef.child("Departments").child(department).setValue(uid);

                    Toast.makeText(this, "Doctor registered successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Registration Error: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            resetUI();
        }
    }

    private boolean validateInputs(String name, String email, String password, String confirmPassword, String department) {
        if (TextUtils.isEmpty(name)) {
            inputName.setError("Name is required");
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputEmail.setError("Invalid email format");
            return false;
        }

        if (password.length() < 6) {
            inputPassword.setError("Password must be at least 6 characters");
            return false;
        }

        if (!password.equals(confirmPassword)) {
            inputConfirmPassword.setError("Passwords do not match");
            return false;
        }

        if (TextUtils.isEmpty(department) || !availableDepartments.contains(department)) {
            Toast.makeText(this, "Please select a valid department", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void resetUI() {
        progressBar.setVisibility(ProgressBar.GONE);
        btnRegister.setEnabled(true);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addPasswordToggle(EditText field, boolean isMainPassword) {
        field.setOnTouchListener((v, event) -> {
            final int DRAWABLE_END = 2;
            if (event.getAction() == MotionEvent.ACTION_UP &&
                    event.getRawX() >= (field.getRight() - field.getCompoundDrawables()[DRAWABLE_END].getBounds().width())) {
                boolean show = isMainPassword ? showPassword : showConfirmPassword;
                if (show) {
                    field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    field.setCompoundDrawablesWithIntrinsicBounds(0, 0, com.example.myapplication.R.drawable.ic_eye_off, 0);
                } else {
                    field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    field.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0);
                }
                if (isMainPassword) showPassword = !showPassword; else showConfirmPassword = !showConfirmPassword;
                field.setSelection(field.getText().length());
                return true;
            }
            return false;
        });
    }
}
