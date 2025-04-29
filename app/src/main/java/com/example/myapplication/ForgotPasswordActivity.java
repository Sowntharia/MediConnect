package com.example.myapplication;

import android.content.Intent;
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

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText inputEmail;
    private Button btnResetPassword;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.instruction), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        inputEmail = findViewById(R.id.inputEmail);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        firebaseAuth = FirebaseAuth.getInstance();

        btnResetPassword.setOnClickListener(v -> {
            String email = inputEmail.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }

            firebaseAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Reset link sent to your email", Toast.LENGTH_LONG).show();
                            startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
                            finish();
                        } else {
                            Toast.makeText(this, "Failed to send reset email", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}
