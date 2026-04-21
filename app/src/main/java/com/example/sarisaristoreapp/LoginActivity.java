package com.example.sarisaristoreapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.HashMap;
import java.util.concurrent.Executor;

public class LoginActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    Button btnLogin, btnBiometric;

    // The Credentials Vault (Username -> Password)
    HashMap<String, String> passwords = new HashMap<>();
    // The Clearance Vault (Username -> Is Admin?)
    HashMap<String, Boolean> adminRoles = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // THE CONCRETE FLOOR: Prevent going back into the app after logout
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity(); // This completely kills the app
            }
        });

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnBiometric = findViewById(R.id.btnBiometric);

        // Load Users
        passwords.put("Fourt", "021413"); adminRoles.put("Fourt", false);
        passwords.put("Euni", "110814");  adminRoles.put("Euni", false);
        passwords.put("Edna", "030979");  adminRoles.put("Edna", true);
        passwords.put("Frank", "120487"); adminRoles.put("Frank", true);
        passwords.put("Darius", "060907"); adminRoles.put("Darius", true);

        // Standard Password Login
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = etUsername.getText().toString().trim();
                String pass = etPassword.getText().toString().trim();

                if (passwords.containsKey(user) && passwords.get(user).equals(pass)) {
                    grantAccess(user, adminRoles.get(user));
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid Credentials", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Biometric Login Engine
        btnBiometric.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = etUsername.getText().toString().trim();

                // Ensure they typed a valid admin username first
                if (!adminRoles.containsKey(user) || !adminRoles.get(user)) {
                    Toast.makeText(LoginActivity.this, "Type an Admin username first to use Fingerprint!", Toast.LENGTH_SHORT).show();
                    return;
                }

                Executor executor = ContextCompat.getMainExecutor(LoginActivity.this);
                BiometricPrompt biometricPrompt = new BiometricPrompt(LoginActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        Toast.makeText(LoginActivity.this, "Fingerprint Accepted", Toast.LENGTH_SHORT).show();
                        grantAccess(user, true); // Bypass password!
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        Toast.makeText(LoginActivity.this, "Biometric Error: " + errString, Toast.LENGTH_SHORT).show();
                    }
                });

                BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Admin Authentication")
                        .setSubtitle("Scan fingerprint to log in as " + user)
                        .setNegativeButtonText("Cancel")
                        .build();

                biometricPrompt.authenticate(promptInfo);
            }
        });
    }

    private void grantAccess(String username, boolean isAdmin) {
        // Save the active session to Android's memory
        SharedPreferences prefs = getSharedPreferences("StoreSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("ActiveUser", username);
        editor.putBoolean("IsAdmin", isAdmin);
        editor.apply();

        // Jump to the Cash Register
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);

        // Kill the login screen so they can't press 'Back' to get to it
        finish();
    }
}