package com.example.sarisaristoreapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.HashMap;
import java.util.concurrent.Executor;

public class LoginActivity extends AppCompatActivity {

    // Logic State
    private String currentPin = "";
    private ImageView[] dots = new ImageView[4];

    // Vaults
    HashMap<String, String> pinVault = new HashMap<>();
    HashMap<String, Boolean> adminRoles = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });

        // --- THE NEW FAMILY PIN VAULT ---
        pinVault.put("0609", "Darius"); adminRoles.put("Darius", true);
        pinVault.put("0309", "Edna");   adminRoles.put("Edna", true);
        pinVault.put("1204", "Frank");  adminRoles.put("Frank", true);
        pinVault.put("0208", "Fourt");  adminRoles.put("Fourt", false);
        pinVault.put("1108", "Euni");   adminRoles.put("Euni", false);

        // Map Dots
        dots[0] = findViewById(R.id.dot1);
        dots[1] = findViewById(R.id.dot2);
        dots[2] = findViewById(R.id.dot3);
        dots[3] = findViewById(R.id.dot4);

        // Map Buttons
        int[] buttonIds = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9};
        for (int id : buttonIds) {
            findViewById(id).setOnClickListener(v -> {
                Button btn = (Button) v;
                appendNumber(btn.getText().toString());
            });
        }

        // Delete Button Logic
        ImageButton btnDelete = findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(v -> {
            if (currentPin.length() > 0) {
                currentPin = currentPin.substring(0, currentPin.length() - 1);
                updateDots();
            }
        });

        // Biometric Logic
        ImageButton btnBiometric = findViewById(R.id.btnBiometric);
        btnBiometric.setOnClickListener(v -> triggerBiometric());
    }

    // --- CORE ENGINE ---
    private void appendNumber(String num) {
        if (currentPin.length() < 4) {
            currentPin += num;
            updateDots();

            // Auto-Verify when 4 digits are hit
            if (currentPin.length() == 4) {
                verifyPin();
            }
        }
    }

    private void updateDots() {
        for (int i = 0; i < 4; i++) {
            if (i < currentPin.length()) {
                dots[i].setImageResource(R.drawable.bg_pin_dot_filled);
            } else {
                dots[i].setImageResource(R.drawable.bg_pin_dot_empty);
            }
        }
    }

    private void verifyPin() {
        if (pinVault.containsKey(currentPin)) {
            String activeUser = pinVault.get(currentPin);
            boolean isAdmin = adminRoles.get(activeUser);

            Toast.makeText(this, "Welcome, " + activeUser, Toast.LENGTH_SHORT).show();
            grantAccess(activeUser, isAdmin);
        } else {
            Toast.makeText(this, "Invalid PIN", Toast.LENGTH_SHORT).show();
            // Reset state on failure
            currentPin = "";
            updateDots();
        }
    }

    private void triggerBiometric() {
        // --- THE FINGERPRINT HARDWARE CHECKER ---
        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                // Hardware is good, proceed to prompt!
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Toast.makeText(this, "No fingerprint scanner on this device.", Toast.LENGTH_SHORT).show();
                return;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Toast.makeText(this, "Fingerprint scanner is currently unavailable.", Toast.LENGTH_SHORT).show();
                return;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Toast.makeText(this, "No fingerprints enrolled. Please add one in your phone settings.", Toast.LENGTH_LONG).show();
                return;
        }

        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                grantAccess("Edna", true);
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                // Ignores the error if the user just pressed 'Cancel' manually
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    Toast.makeText(LoginActivity.this, "Biometric Error: " + errString, Toast.LENGTH_SHORT).show();
                }
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Admin Authentication")
                .setSubtitle("Scan fingerprint to bypass PIN")
                .setNegativeButtonText("Cancel")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void grantAccess(String username, boolean isAdmin) {
        SharedPreferences prefs = getSharedPreferences("StoreSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("ActiveUser", username);
        editor.putBoolean("IsAdmin", isAdmin);
        editor.apply();

        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}