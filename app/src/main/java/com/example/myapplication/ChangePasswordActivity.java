package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ChangePasswordActivity extends AppCompatActivity {

    EditText editCurrentPassword, editNewPassword, editConfirmPassword;
    Button btnChangePassword;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    DatabaseReference userRef;

    boolean[] showCurrent = {false};
    boolean[] showNew = {false};
    boolean[] showConfirm = {false};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_password);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Change Password");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Firebase setup
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        editCurrentPassword = findViewById(R.id.editCurrentPassword);
        editNewPassword = findViewById(R.id.editNewPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        addVisibilityToggle(editCurrentPassword, showCurrent);
        addVisibilityToggle(editNewPassword, showNew);
        addVisibilityToggle(editConfirmPassword, showConfirm);

        btnChangePassword.setOnClickListener(view -> changePassword());
    }

    private void changePassword() {
        String currentPass = editCurrentPassword.getText().toString().trim();
        String newPass = editNewPassword.getText().toString().trim();
        String confirmPass = editConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(currentPass) || TextUtils.isEmpty(newPass) || TextUtils.isEmpty(confirmPass)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPass.equals(confirmPass)) {
            Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Re-authenticate
        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPass);
        currentUser.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                currentUser.updatePassword(newPass).addOnCompleteListener(updateTask -> {
                    if (updateTask.isSuccessful()) {
                        //Remove if you no longer store password in DB
                        userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
                        userRef.child("password").setValue(null); // Avoid storing new password

                        Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                        Intent intent = new Intent(ChangePasswordActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Password update failed", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "Incorrect current password", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addVisibilityToggle(EditText field, boolean[] flag) {
        field.setOnTouchListener((v, event) -> {
            final int DRAWABLE_END = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (field.getRight() - field.getCompoundDrawables()[DRAWABLE_END].getBounds().width())) {
                    toggleVisibility(field, flag);
                    return true;
                }
            }
            return false;
        });
    }

    private void toggleVisibility(EditText field, boolean[] flag) {
        if (flag[0]) {
            field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            field.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_off, 0);
        } else {
            field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            field.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0);
        }
        field.setSelection(field.getText().length());
        flag[0] = !flag[0];
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
