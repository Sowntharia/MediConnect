package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    EditText inputEmail, inputPassword;
    Button btnLogin, btnForgotPassword;
    FirebaseAuth mAuth;
    DatabaseReference userRef;
    boolean isPasswordVisible = false;

    private int loginAttempts = 0;
    private static final int MAX_LOGIN_ATTEMPTS = 5;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(com.example.myapplication.R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(com.example.myapplication.R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance().getReference("Users");

        inputEmail = findViewById(com.example.myapplication.R.id.inputEmail);
        inputPassword = findViewById(com.example.myapplication.R.id.inputPassword);
        btnLogin = findViewById(com.example.myapplication.R.id.btnLogin);
        btnForgotPassword = findViewById(com.example.myapplication.R.id.btnForgotPassword);

        inputPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_END = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (inputPassword.getRight() - inputPassword.getCompoundDrawables()[DRAWABLE_END].getBounds().width())) {
                    if (isPasswordVisible) {
                        inputPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        inputPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, com.example.myapplication.R.drawable.ic_eye_off, 0);
                        isPasswordVisible = false;
                    } else {
                        inputPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        inputPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0);
                        isPasswordVisible = true;
                    }
                    inputPassword.setSelection(inputPassword.getText().length());
                    return true;
                }
            }
            return false;
        });

        // Login button
        btnLogin.setOnClickListener(v -> {
            String email = inputEmail.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                inputEmail.setError("Invalid email format");
                return;
            }

            if (password.isEmpty()) {
                inputPassword.setError("Password required");
                return;
            }

            if (loginAttempts >= MAX_LOGIN_ATTEMPTS) {
                Toast.makeText(this, "Too many failed attempts. Try again later.", Toast.LENGTH_LONG).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(loginTask -> {
                if (loginTask.isSuccessful()) {
                    loginAttempts = 0;
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        loadUserRoleAndRedirect(firebaseUser);
                    }
                } else {
                    loginAttempts++;

                    FirebaseAuth.getInstance().fetchSignInMethodsForEmail(email).addOnCompleteListener(checkTask -> {
                        if (checkTask.isSuccessful()) {
                            if (checkTask.getResult() != null && !Objects.requireNonNull(checkTask.getResult().getSignInMethods()).isEmpty()) {
                                Toast.makeText(LoginActivity.this, "Incorrect password. Attempts left: " + (MAX_LOGIN_ATTEMPTS - loginAttempts), Toast.LENGTH_LONG).show();
                            } else {
                                String key = email.replace(".", "_");
                                userRef.child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.exists()) {
                                            RegisterActivity2.RegisteredUser user = snapshot.getValue(RegisterActivity2.RegisteredUser.class);

                                            mAuth.createUserWithEmailAndPassword(email, password)
                                                    .addOnCompleteListener(registerTask -> {
                                                        if (registerTask.isSuccessful()) {
                                                            String uid = Objects.requireNonNull(registerTask.getResult().getUser()).getUid();
                                                            userRef.child(uid).setValue(user);
                                                            userRef.child(key).removeValue();
                                                            Toast.makeText(LoginActivity.this, "Migrated successfully. Please login again.", Toast.LENGTH_LONG).show();
                                                        } else {
                                                            Toast.makeText(LoginActivity.this, "Migration failed: " + Objects.requireNonNull(registerTask.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        } else {
                                            Toast.makeText(LoginActivity.this, "User not found.", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Toast.makeText(LoginActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Failed to verify email: " + Objects.requireNonNull(checkTask.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        });

        // Forgot Password
        btnForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }

    private void loadUserRoleAndRedirect(FirebaseUser firebaseUser) {
        String uid = firebaseUser.getUid();
        userRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String fullName = snapshot.child("fullName").getValue(String.class);
                String role = snapshot.child("role").getValue(String.class);

                if (role != null) {
                    if (role.equalsIgnoreCase("patient")) {
                        Intent intent = new Intent(LoginActivity.this, UserProfileActivity.class);
                        intent.putExtra("userEmail", firebaseUser.getEmail());
                        intent.putExtra("fullName", fullName != null ? fullName : "User");
                        startActivity(intent);
                        finish();
                    } else if (role.equalsIgnoreCase("doctor")) {
                        Intent intent = new Intent(LoginActivity.this, DoctorDashboardActivity.class);
                        intent.putExtra("userEmail", firebaseUser.getEmail());
                        intent.putExtra("fullName", fullName != null ? fullName : "User");
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Unknown role.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Role not found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LoginActivity.this, "Failed to load user role: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
