package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class DoctorLoginActivity extends AppCompatActivity {

    private EditText inputEmail, inputPassword;
    private Button btnLogin;
    private FirebaseAuth mAuth;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_doctor_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        btnLogin = findViewById(R.id.btnLogin);
        mAuth = FirebaseAuth.getInstance();
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> loginDoctor());
        setupPasswordToggle();
    }

    private void loginDoctor() {
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputEmail.setError("Enter a valid email");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            inputPassword.setError("Enter your password");
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);

                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String role = snapshot.child("role").getValue(String.class);

                        if ("doctor".equalsIgnoreCase(role)) {
                            Toast.makeText(DoctorLoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(DoctorLoginActivity.this, DoctorDashboardActivity.class));
                            finish();
                        } else {
                            Toast.makeText(DoctorLoginActivity.this, "Access denied. You are not a doctor.", Toast.LENGTH_SHORT).show();
                            mAuth.signOut();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(DoctorLoginActivity.this, "Failed to load profile: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(DoctorLoginActivity.this, "Login failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupPasswordToggle() {
        inputPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_END = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (inputPassword.getRight() - inputPassword.getCompoundDrawables()[DRAWABLE_END].getBounds().width())) {
                    if (isPasswordVisible) {
                        inputPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        inputPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_off, 0);
                    } else {
                        inputPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        inputPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0);
                    }
                    isPasswordVisible = !isPasswordVisible;
                    inputPassword.setSelection(inputPassword.getText().length());
                    return true;
                }
            }
            return false;
        });
    }
}
