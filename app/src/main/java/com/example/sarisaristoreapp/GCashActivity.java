package com.example.sarisaristoreapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sarisaristoreapp.models.Product;

import java.util.ArrayList;

public class GCashActivity extends AppCompatActivity {

    Spinner spinnerType;
    EditText etGcashAmount;
    EditText etGcashNumber;
    androidx.appcompat.widget.SwitchCompat switchFee;

    TextView tvFeeAmount, tvReceivesLabel, tvReceivesAmount, tvTotalCollect;
    Button btnProcessGcash;

    DatabaseHelper dbHelper;

    double calculatedFee = 0.0;
    double finalReceives = 0.0;
    double finalTotal = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gcash);

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(GCashActivity.this, MainActivity.class));
                finish();
            }
        });

        getWindow().setStatusBarColor(getResources().getColor(R.color.kanto_primary));
        dbHelper = new DatabaseHelper(this);

        // --- DRAWER ENGINE START ---
        androidx.drawerlayout.widget.DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        com.google.android.material.navigation.NavigationView navView = findViewById(R.id.nav_view);

        navView.setCheckedItem(R.id.nav_gcash);

        android.content.SharedPreferences prefs = getSharedPreferences("StoreSession", MODE_PRIVATE);
        String activeCashier = prefs.getString("ActiveUser", "Unknown");
        boolean isAdmin = prefs.getBoolean("IsAdmin", false);

        android.view.View headerView = navView.getHeaderView(0);
        android.widget.TextView tvHeaderRole = headerView.findViewById(R.id.tvHeaderRole);
        tvHeaderRole.setText(isAdmin ? activeCashier + " / Admin" : activeCashier + " / Cashier");

        android.view.Menu navMenu = navView.getMenu();
        if (!isAdmin) {
            navMenu.findItem(R.id.nav_inventory).setVisible(false);
            navMenu.findItem(R.id.nav_gcash).setVisible(false);
            navMenu.findItem(R.id.nav_dashboard).setVisible(false);
        }

        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_checkout) {
                startActivity(new Intent(this, MainActivity.class)); finish();
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, HistoryActivity.class)); finish();
            } else if (id == R.id.nav_utang) {
                startActivity(new Intent(this, UtangActivity.class)); finish();
            } else if (id == R.id.nav_inventory) {
                startActivity(new Intent(this, InventoryActivity.class)); finish();
            } else if (id == R.id.nav_gcash) {
                drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START);
            } else if (id == R.id.nav_dashboard) {
                startActivity(new Intent(this, DashboardActivity.class)); finish();
            } else if (id == R.id.nav_logout) {
                prefs.edit().clear().apply();
                Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class)); finish();
            }
            drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START);
            return true;
        });

        ImageView btnMenu = findViewById(R.id.btnMenu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                if (!drawerLayout.isDrawerOpen(androidx.core.view.GravityCompat.START)) {
                    drawerLayout.openDrawer(androidx.core.view.GravityCompat.START);
                }
            });
        }
        // --- DRAWER ENGINE END ---

        ImageView btnInfo = findViewById(R.id.btnInfo);
        if (btnInfo != null) {
            btnInfo.setOnClickListener(v -> {
                android.app.Dialog infoDialog = new android.app.Dialog(this);
                infoDialog.setContentView(R.layout.layout_info_dialog);
                if (infoDialog.getWindow() != null) infoDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                TextView tvMessage = infoDialog.findViewById(R.id.tvInfoMessage);
                Button btnGotIt = infoDialog.findViewById(R.id.btnInfoGotIt);
                tvMessage.setText("This is the GCash service terminal. Process Cash In, Cash Out, and Buy Load transactions while automatically calculating standard store service fees.");
                btnGotIt.setOnClickListener(view -> infoDialog.dismiss());
                infoDialog.show();
            });
        }

        spinnerType = findViewById(R.id.spinnerType);
        etGcashAmount = findViewById(R.id.etGcashAmount);
        switchFee = findViewById(R.id.switchFee);
        etGcashNumber = findViewById(R.id.etGcashNumber);
        tvFeeAmount = findViewById(R.id.tvFeeAmount);
        tvReceivesLabel = findViewById(R.id.tvReceivesLabel);
        tvReceivesAmount = findViewById(R.id.tvReceivesAmount);
        tvTotalCollect = findViewById(R.id.tvTotalCollect);
        btnProcessGcash = findViewById(R.id.btnProcessGcash);

        String[] transactionTypes = {"Cash In", "Cash Out", "Buy Load"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_selected, transactionTypes);
        spinnerAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerType.setAdapter(spinnerAdapter);
        findViewById(R.id.spinnerType).getParent().requestDisallowInterceptTouchEvent(false);
        ((View) spinnerType.getParent()).setOnClickListener(v -> spinnerType.performClick());

        etGcashAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { calculateMath(); }
        });

        switchFee.setOnCheckedChangeListener((buttonView, isChecked) -> calculateMath());

        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = transactionTypes[position];
                if (selected.equals("Buy Load")) {
                    tvReceivesLabel.setText("Load Amount");
                } else {
                    tvReceivesLabel.setText("Customer Receives");
                }
                calculateMath();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnProcessGcash.setOnClickListener(v -> {
            String amountStr = etGcashAmount.getText().toString().trim();
            if (amountStr.isEmpty() || finalTotal <= 0) {
                Toast.makeText(this, "Please enter a valid amount!", Toast.LENGTH_SHORT).show();
                return;
            }

            String numberStr = etGcashNumber.getText().toString().trim();
            if (numberStr.isEmpty() || numberStr.length() < 11) {
                Toast.makeText(this, "Please enter a valid 11-digit GCash number!", Toast.LENGTH_SHORT).show();
                return;
            }

            String type = spinnerType.getSelectedItem().toString();
            String deviceName = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;

            ArrayList<Product> gcashDetails = new ArrayList<>();
            Product numberItem = new Product(0, 1, 0.0, finalTotal, "GCASH", "No: " + numberStr + " (Charge: ₱" + calculatedFee + ")", type);
            gcashDetails.add(numberItem);
            dbHelper.saveTransaction("GCash " + type, "GCash", finalTotal, finalTotal, 0.0, deviceName, gcashDetails);

            Toast.makeText(this, type + " Successful!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    private void calculateMath() {
        String amountStr = etGcashAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            resetUI();
            return;
        }

        double baseAmount = 0.0;
        try {
            baseAmount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            resetUI();
            return;
        }

        String type = spinnerType.getSelectedItem().toString();

        if (type.equals("Buy Load")) {
            calculatedFee = 3.0;
        } else {
            int thousands = (int) (baseAmount / 1000);
            double thousandsFee = thousands * 20.0;
            double remainder = baseAmount % 1000;
            double remainderFee = 0.0;

            if (remainder > 0 && remainder <= 499) {
                remainderFee = 5.0;
            } else if (remainder >= 500 && remainder <= 599) {
                remainderFee = 10.0;
            } else if (remainder >= 600 && remainder <= 999) {
                remainderFee = 15.0;
            }
            calculatedFee = thousandsFee + remainderFee;
        }

        if (switchFee.isChecked()) {
            finalReceives = baseAmount;
            finalTotal = baseAmount + calculatedFee;
        } else {
            finalReceives = baseAmount - calculatedFee;
            finalTotal = baseAmount;
        }

        tvFeeAmount.setText(String.format("₱%.2f", calculatedFee));
        tvReceivesAmount.setText(String.format("₱%.2f", finalReceives));
        tvTotalCollect.setText(String.format("₱%.2f", finalTotal));
    }

    private void resetUI() {
        calculatedFee = 0.0;
        finalReceives = 0.0;
        finalTotal = 0.0;
        tvFeeAmount.setText("₱0.00");
        tvReceivesAmount.setText("₱0.00");
        tvTotalCollect.setText("₱0.00");
    }
}