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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sarisaristoreapp.models.Product;

import java.util.ArrayList;

public class GCashActivity extends AppCompatActivity {

    ImageView btnBack;
    Spinner spinnerType;
    EditText etGcashAmount;
    EditText etGcashNumber;
    androidx.appcompat.widget.SwitchCompat switchFee;

    TextView tvFeeAmount, tvReceivesLabel, tvReceivesAmount, tvTotalCollect;
    Button btnProcessGcash;

    DatabaseHelper dbHelper;

    // Standard Sari-Sari Store logic: 1.5% fee


    double calculatedFee = 0.0;
    double finalReceives = 0.0;
    double finalTotal = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gcash);

        // TELEPORT BACK TO POS CHECKOUT
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(GCashActivity.this, MainActivity.class));
                finish();
            }
        });

        getWindow().setStatusBarColor(getResources().getColor(R.color.kanto_primary));

        dbHelper = new DatabaseHelper(this);

        // The Universal Info Trapdoor
        ImageView btnInfo = findViewById(R.id.btnInfo);

        if (btnInfo != null) {
            btnInfo.setOnClickListener(v -> {
                android.app.Dialog infoDialog = new android.app.Dialog(this);
                infoDialog.setContentView(R.layout.layout_info_dialog);

                if (infoDialog.getWindow() != null) {
                    infoDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                }

                android.widget.TextView tvMessage = infoDialog.findViewById(R.id.tvInfoMessage);
                android.widget.Button btnGotIt = infoDialog.findViewById(R.id.btnInfoGotIt);

                // --- CHANGE THIS TEXT FOR EACH ACTIVITY ---
                tvMessage.setText("This is the GCash service terminal. Process Cash In, Cash Out, and Buy Load transactions while automatically calculating standard store service fees.");
                // ------------------------------------------

                btnGotIt.setOnClickListener(view -> infoDialog.dismiss());

                infoDialog.show();
            });
        }

        btnBack = findViewById(R.id.btnBack);
        spinnerType = findViewById(R.id.spinnerType);
        etGcashAmount = findViewById(R.id.etGcashAmount);
        switchFee = findViewById(R.id.switchFee);
        etGcashNumber = findViewById(R.id.etGcashNumber);

        tvFeeAmount = findViewById(R.id.tvFeeAmount);
        tvReceivesLabel = findViewById(R.id.tvReceivesLabel);
        tvReceivesAmount = findViewById(R.id.tvReceivesAmount);
        tvTotalCollect = findViewById(R.id.tvTotalCollect);
        btnProcessGcash = findViewById(R.id.btnProcessGcash);

        btnBack.setOnClickListener(v -> showAdminMenu());

        // Setup Spinner using your custom layouts!
        String[] transactionTypes = {"Cash In", "Cash Out", "Buy Load"};

        // 1. Use the SLIM layout for the closed box so the text stays centered
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_selected, transactionTypes);

        // 2. Use the FAT default layout for the dropdown list so Edna can tap it easily
        spinnerAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);

        spinnerType.setAdapter(spinnerAdapter);
        // Force the spinner to open when the user taps ANYWHERE on the RelativeLayout box
        findViewById(R.id.spinnerType).getParent().requestDisallowInterceptTouchEvent(false);
        ((View) spinnerType.getParent()).setOnClickListener(v -> spinnerType.performClick());

        // Listeners to update math instantly
        etGcashAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                calculateMath();
            }
        });

        switchFee.setOnCheckedChangeListener((buttonView, isChecked) -> calculateMath());

        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Update the label dynamically
                String selected = transactionTypes[position];
                if (selected.equals("Buy Load")) {
                    tvReceivesLabel.setText("Load Amount");
                } else {
                    tvReceivesLabel.setText("Customer Receives");
                }
                calculateMath();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // The Checkout Button
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

            if (finalTotal <= 0) {
                Toast.makeText(this, "Enter a valid amount!", Toast.LENGTH_SHORT).show();
                return;
            }

            String type = spinnerType.getSelectedItem().toString();
            String deviceName = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;

            // Log it in the History! (Empty items list since it's a GCash service)
            // Inject the GCash number into the receipt details!
            ArrayList<Product> gcashDetails = new ArrayList<>();
            // We pass it as a fake Product so the SQLite database saves it in the receipt items list
            Product numberItem = new Product(0, 1, 0.0, finalTotal, "GCASH", "No: " + numberStr + " (Charge: ₱" + calculatedFee + ")", "GCash");

            // Now pass `gcashDetails` instead of `emptyItems` into your save method:
            // PASS THE RIGHT ARRAY!
            dbHelper.saveTransaction("GCash " + type, "GCash", finalTotal, finalTotal, 0.0, deviceName, gcashDetails);

            Toast.makeText(this, type + " Successful!", Toast.LENGTH_SHORT).show();

            startActivity(new Intent(this, MainActivity.class));

            finish(); // Close activity after success
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

        // Grab the current transaction type from the spinner
        String type = spinnerType.getSelectedItem().toString();

        if (type.equals("Buy Load")) {
            // FLAT FEE RULE: Always exactly 3 pesos regardless of amount
            calculatedFee = 3.0;
        } else {
            // --- AUTHENTIC SARI-SARI TIERED LOGIC (Cash In / Cash Out) ---
            // 1. Calculate how many full thousands there are (₱20 each)
            int thousands = (int) (baseAmount / 1000);
            double thousandsFee = thousands * 20.0;

            // 2. Calculate the remainder and apply the tier brackets
            double remainder = baseAmount % 1000;
            double remainderFee = 0.0;

            if (remainder > 0 && remainder <= 499) {
                remainderFee = 5.0;
            } else if (remainder >= 500 && remainder <= 599) {
                remainderFee = 10.0;
            } else if (remainder >= 600 && remainder <= 999) {
                remainderFee = 15.0;
            }

            // 3. Combine them for the total fee! (e.g., 1550 -> 20 + 10 = 30)
            calculatedFee = thousandsFee + remainderFee;
        }

        // The Toggle Logic
        if (switchFee.isChecked()) {
            // EXCLUSIVE FEE (Added on top)
            // Example: Enters 1000. Customer gets 1000. Ate Edna collects 1015.
            finalReceives = baseAmount;
            finalTotal = baseAmount + calculatedFee;
        } else {
            // INCLUSIVE FEE (Deducted from cash)
            // Example: Enters 1000. Customer gets 985. Ate Edna collects 1000.
            finalReceives = baseAmount - calculatedFee;
            finalTotal = baseAmount;
        }

        // Update the UI
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
    private void showAdminMenu() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View sheet = getLayoutInflater().inflate(R.layout.layout_bottom_sheet, null);

        // Define Buttons
        Button btnHistory = sheet.findViewById(R.id.btnMenuHistory);
        Button btnUtang = sheet.findViewById(R.id.btnMenuUtang);
        Button btnInventory = sheet.findViewById(R.id.btnMenuInventory);
        Button btnGcash = sheet.findViewById(R.id.btnMenuGcash);
        Button btnAdmin = sheet.findViewById(R.id.btnMenuAdmin);
        Button btnCheckout = sheet.findViewById(R.id.btnMenuCheckout);

        // SECURE THE GATE: Check Admin Status from SharedPreferences
        android.content.SharedPreferences prefs = getSharedPreferences("StoreSession", MODE_PRIVATE);
        boolean isAdmin = prefs.getBoolean("IsAdmin", false);

        // Hide admin-only buttons if not admin
        if (!isAdmin) {
            btnInventory.setVisibility(View.GONE);
            btnGcash.setVisibility(View.GONE);
            btnAdmin.setVisibility(View.GONE);
        }

        // Logic (Navigation)
        btnCheckout.setOnClickListener(v -> { dialog.dismiss(); startActivity(new Intent(this, MainActivity.class)); finish(); });
        btnHistory.setOnClickListener(v -> { dialog.dismiss(); startActivity(new Intent(this, HistoryActivity.class)); finish(); });
        btnUtang.setOnClickListener(v -> { dialog.dismiss(); startActivity(new Intent(this, UtangActivity.class)); finish(); });

        // Admin-only logic
        if (isAdmin) {
            btnInventory.setOnClickListener(v -> { dialog.dismiss(); startActivity(new Intent(this, InventoryActivity.class)); finish(); });
            btnGcash.setOnClickListener(v -> { dialog.dismiss(); startActivity(new Intent(this, GCashActivity.class)); finish(); });
            btnAdmin.setOnClickListener(v -> { dialog.dismiss(); startActivity(new Intent(this, DashboardActivity.class)); });
        }

        dialog.setContentView(sheet);
        // FORCE EXPAND & KILL DRAG (The Scroll Trap Fix)
        com.google.android.material.bottomsheet.BottomSheetBehavior behavior = dialog.getBehavior();
        behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);

        dialog.show();
    }
}