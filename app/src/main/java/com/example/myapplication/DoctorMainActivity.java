package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class DoctorMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_doctor_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize buttons
        Button btnDoctorLogin = findViewById(R.id.btnDoctorLogin);
        Button btnDoctorRegister = findViewById(R.id.btnDoctorRegister);

        // Handle button clicks
        btnDoctorLogin.setOnClickListener(v -> startActivity(new Intent(DoctorMainActivity.this, DoctorLoginActivity.class)));

        btnDoctorRegister.setOnClickListener(v -> startActivity(new Intent(DoctorMainActivity.this, DoctorRegisterActivity.class)));
    }
}
