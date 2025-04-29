package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

public class RegisterActivity2 extends AppCompatActivity {

    EditText inputPassport, inputCodiceFiscale, inputPassword, inputConfirmPassword;
    Button btnRegister;
    boolean showPassword = false;
    boolean showConfirmPassword = false;

    FirebaseAuth mAuth;
    DatabaseReference databaseReference;

    String fullName, email, dob, gender, mobile;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register2);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        inputPassport = findViewById(R.id.inputPassport);
        inputCodiceFiscale = findViewById(R.id.inputCodiceFiscale);
        inputPassword = findViewById(R.id.inputPassword);
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        // Retrieve first screen data
        fullName = getIntent().getStringExtra("fullName");
        email = getIntent().getStringExtra("email");
        dob = getIntent().getStringExtra("dob");
        gender = getIntent().getStringExtra("gender");
        mobile = getIntent().getStringExtra("mobile");

        setupPasswordVisibility(inputPassword, true);
        setupPasswordVisibility(inputConfirmPassword, false);

        btnRegister.setOnClickListener(v -> validateAndRegisterUser());
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupPasswordVisibility(EditText input, boolean isMainPassword) {
        input.setOnTouchListener((v, event) -> {
            final int DRAWABLE_END = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (input.getRight() - input.getCompoundDrawables()[DRAWABLE_END].getBounds().width())) {
                    if (isMainPassword) {
                        showPassword = !showPassword;
                        togglePasswordVisibility(input, showPassword);
                    } else {
                        showConfirmPassword = !showConfirmPassword;
                        togglePasswordVisibility(input, showConfirmPassword);
                    }
                    return true;
                }
            }
            return false;
        });
    }

    private void togglePasswordVisibility(EditText input, boolean show) {
        if (show) {
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            input.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0);
        } else {
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            input.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_off, 0);
        }
        input.setSelection(input.getText().length());
    }

    private void validateAndRegisterUser() {
        String passport = inputPassport.getText().toString().trim();
        String codiceFiscale = inputCodiceFiscale.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        String confirmPassword = inputConfirmPassword.getText().toString().trim();

        boolean hasError = false;

        if (passport.isEmpty()) {
            inputPassport.setError("Please enter passport number");
            hasError = true;
        } else if (!passport.matches("^[A-Z]{1}\\d{5}$")) {
            inputPassport.setError("Must start with 1 capital letter followed by 5 digits (e.g., A12345)");
            hasError = true;
        }

        if (codiceFiscale.isEmpty()) {
            inputCodiceFiscale.setError("Please enter codice fiscale");
            hasError = true;
        } else if (!codiceFiscale.matches("^[A-Z0-9]{16,20}$")) {
            inputCodiceFiscale.setError("Must be 16–20 characters, only capital letters and digits");
            hasError = true;
        }

        if (password.isEmpty()) {
            inputPassword.setError("Please enter password");
            hasError = true;
        } else if (!password.matches("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@#$%^&+=!]).{6,8}$")) {
            inputPassword.setError("Password must be 6–8 characters, include uppercase, lowercase, number, special char");
            hasError = true;
        }

        if (confirmPassword.isEmpty()) {
            inputConfirmPassword.setError("Please confirm your password");
            hasError = true;
        } else if (!password.equals(confirmPassword)) {
            inputConfirmPassword.setError("Passwords do not match");
            hasError = true;
        }

        if (hasError) return;

        registerNewPatient(passport, codiceFiscale, password);
    }

    private void registerNewPatient(String passport, String codiceFiscale, String password) {
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                RegisteredUser user = new RegisteredUser(fullName, email, dob, gender, mobile, passport, codiceFiscale, "patient");

                databaseReference.child(uid).setValue(user)
                        .addOnCompleteListener(dbTask -> {
                            if (dbTask.isSuccessful()) {
                                Intent intent = new Intent(RegisterActivity2.this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                inputPassport.setError("Error saving user data: " + Objects.requireNonNull(dbTask.getException()).getMessage());
                            }
                        });
            } else {
                if (password.length() < 6) {
                    inputPassword.setError("Password must be at least 6 characters for Firebase");
                }
                inputPassword.setError("Registration failed: " + Objects.requireNonNull(task.getException()).getMessage());
            }
        });
    }

    // User Model
    public static class RegisteredUser {
        public String fullName, email, dob, gender, mobile;
        public String passport, codiceFiscale;
        public String role;

        public RegisteredUser() { }

        public RegisteredUser(String fullName, String email, String dob, String gender, String mobile,
                              String passport, String codiceFiscale, String role) {
            this.fullName = fullName;
            this.email = email;
            this.dob = dob;
            this.gender = gender;
            this.mobile = mobile;
            this.passport = passport;
            this.codiceFiscale = codiceFiscale;
            this.role = role;
        }
    }
}
