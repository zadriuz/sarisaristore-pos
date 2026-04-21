package com.example.sarisaristoreapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sarisaristoreapp.models.Product;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class UtangActivity extends AppCompatActivity {

    DatabaseHelper dbHelper;
    RecyclerView listUtang;
    TextView tvTotalUtang;
    ImageView btnMenu;
    EditText etSearchNeighbor;

    UtangAdapter adapter;

    // The Master Map holds ALL data. The Arrays hold only the SEARCHED data.
    LinkedHashMap<String, Double> masterDebtMap = new LinkedHashMap<>();
    ArrayList<String> neighborNames = new ArrayList<>();
    ArrayList<Double> neighborBalances = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_utang);

        // TELEPORT BACK TO POS CHECKOUT
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(UtangActivity.this, MainActivity.class));
                finish();
            }
        });

        getWindow().setStatusBarColor(getResources().getColor(R.color.kanto_primary));

        dbHelper = new DatabaseHelper(this);
        listUtang = findViewById(R.id.listUtang);
        tvTotalUtang = findViewById(R.id.tvTotalUtang);
        btnMenu = findViewById(R.id.btnMenu);
        etSearchNeighbor = findViewById(R.id.etSearchNeighbor);

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
                tvMessage.setText("This is the Utang ledger. Track neighbor debts, record partial or full payments, and view borrowing history to manage the store's credit.");
                // ------------------------------------------

                btnGotIt.setOnClickListener(view -> infoDialog.dismiss());

                infoDialog.show();
            });
        }

        listUtang.setLayoutManager(new LinearLayoutManager(this));

        adapter = new UtangAdapter(neighborNames, neighborBalances, new UtangAdapter.OnUtangActionListener() {
            @Override
            public void onViewDetails(String targetNeighbor, double currentBalance) {
                dbHelper.getNeighborHistory(targetNeighbor, historyLines -> {

                    // 1. Build the Detailed History String
                    StringBuilder historyMessage = new StringBuilder();
                    for (String line : historyLines) {
                        // Dawg: If your DB isn't returning specific items here,
                        // you will need to upgrade your SQL query inside DatabaseHelper!
                        // For now, it prints whatever detailed lines the DB gives it.
                        historyMessage.append("• ").append(line).append("\n\n");
                    }
                    if (historyLines.isEmpty()) {
                        historyMessage.append("No detailed timeline found.");
                    }

                    // 2. Summon the Custom Bottom Sheet
                    android.app.Dialog statementDialog = new android.app.Dialog(UtangActivity.this);
                    View sheetView = getLayoutInflater().inflate(R.layout.layout_statement_sheet, null);

                    // 3. Link the UI Elements
                    TextView tvName = sheetView.findViewById(R.id.tvStatementName);
                    TextView tvHistory = sheetView.findViewById(R.id.tvStatementHistory);
                    TextView tvBalance = sheetView.findViewById(R.id.tvStatementBalance);
                    ImageButton btnClose = sheetView.findViewById(R.id.btnCloseStatement);
                    Button btnSettle = sheetView.findViewById(R.id.btnSettleStatement);

                    // SECURE THE GATE: Hide the Settle button for cashiers!
                    android.content.SharedPreferences prefs = getSharedPreferences("StoreSession", MODE_PRIVATE);
                    boolean isAdmin = prefs.getBoolean("IsAdmin", false);

                    if (!isAdmin) {
                        btnSettle.setVisibility(View.GONE);
                    }

                    // 4. Fill the Data
                    tvName.setText(targetNeighbor);
                    tvHistory.setText(historyMessage.toString());
                    tvBalance.setText(String.format("₱%.2f", currentBalance));

                    // 5. Wire the Buttons
                    btnClose.setOnClickListener(v1 -> statementDialog.dismiss());

                    btnSettle.setOnClickListener(v1 -> {
                        statementDialog.dismiss();
                        showPaymentInput(targetNeighbor, currentBalance); // Directly opens the payment box!
                    });

                    // 1. Load the XML
                    statementDialog.setContentView(sheetView);

                    // 2. THE INVISIBLE CLOAK: Put this right here!
                    if (statementDialog.getWindow() != null) {
                        statementDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                    }

                    // 3. Show the dialog
                    statementDialog.show();
                });
            }

            @Override
            public void onSettleDebt(String targetNeighbor, double currentBalance) {
                // SECURE THE GATE: Hide the Settle button for cashiers!
                android.content.SharedPreferences prefs = getSharedPreferences("StoreSession", MODE_PRIVATE);
                boolean isAdmin = prefs.getBoolean("IsAdmin", false);

                if (!isAdmin) {
                    Toast.makeText(UtangActivity.this, "Action restricted to Admins", Toast.LENGTH_SHORT).show();
                    return;
                }

                showPaymentInput(targetNeighbor, currentBalance);
            }
        });

        listUtang.setAdapter(adapter);

        // Fetch live balances
        loadUtangData();

        // LIVE SEARCH ENGINE
        etSearchNeighbor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                filterList(s.toString());
            }
        });

        // WAKE UP THE SECURE MENU
        btnMenu.setOnClickListener(v -> showAdminMenu());

    }

    private void loadUtangData() {
        dbHelper.getCloudDebts(debtMap -> {
            masterDebtMap.clear();
            double grandTotal = 0.0;

            for(Map.Entry<String, Double> entry : debtMap.entrySet()){
                if (entry.getValue() > 0) {
                    masterDebtMap.put(entry.getKey(), entry.getValue());
                    grandTotal += entry.getValue();
                }
            }

            tvTotalUtang.setText(String.format("₱%.2f", grandTotal));
            filterList(etSearchNeighbor.getText().toString());
        });
    }

    private void filterList(String query) {
        neighborNames.clear();
        neighborBalances.clear();

        for (Map.Entry<String, Double> entry : masterDebtMap.entrySet()) {
            if (entry.getKey().toLowerCase().contains(query.toLowerCase())) {
                neighborNames.add(entry.getKey());
                neighborBalances.add(entry.getValue());
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showPaymentInput(String targetNeighbor, double currentBalance) {
        com.google.android.material.bottomsheet.BottomSheetDialog paymentDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(UtangActivity.this);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_payment_sheet, null);

        TextView tvTitle = sheetView.findViewById(R.id.tvPaymentTitle);
        TextView tvBalance = sheetView.findViewById(R.id.tvPaymentBalance);
        EditText etAmount = sheetView.findViewById(R.id.etPaymentAmount);
        TextView btnQuickFull = sheetView.findViewById(R.id.btnQuickFull);
        TextView btnQuick100 = sheetView.findViewById(R.id.btnQuick100);
        TextView btnQuick500 = sheetView.findViewById(R.id.btnQuick500);
        Button btnConfirm = sheetView.findViewById(R.id.btnConfirmPayment);

        // Populate the Headers
        tvTitle.setText("Receive Payment: " + targetNeighbor);
        tvBalance.setText(String.format("Current Balance: ₱%.2f", currentBalance));

        // Wire the Quick Select Buttons (Accumulator Mode!)
        btnQuickFull.setOnClickListener(v -> etAmount.setText(String.valueOf(currentBalance)));

        btnQuick100.setOnClickListener(v -> {
            String currentStr = etAmount.getText().toString().trim();
            // If the box is empty, treat it as 0. Otherwise, grab the number!
            double current = currentStr.isEmpty() ? 0.0 : Double.parseDouble(currentStr);
            etAmount.setText(String.valueOf(current + 100));
        });

        btnQuick500.setOnClickListener(v -> {
            String currentStr = etAmount.getText().toString().trim();
            double current = currentStr.isEmpty() ? 0.0 : Double.parseDouble(currentStr);
            etAmount.setText(String.valueOf(current + 500));
        });

        btnMenu.setOnClickListener(v -> showAdminMenu());

        // Wire the Checkout Button
        btnConfirm.setOnClickListener(v -> {
            String paymentStr = etAmount.getText().toString().trim();
            if (paymentStr.isEmpty()) {
                Toast.makeText(UtangActivity.this, "Enter an amount to settle!", Toast.LENGTH_SHORT).show();
                return;
            }

            double paymentAmount = Double.parseDouble(paymentStr);

            if (paymentAmount <= 0) {
                Toast.makeText(UtangActivity.this, "Payment must be greater than zero!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (paymentAmount > currentBalance) {
                Toast.makeText(UtangActivity.this, "Payment cannot exceed balance!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Trojan Horse Math
            double remainingBalance = currentBalance - paymentAmount;

            Product dummyPayment = new Product(0, 1, 0.0, paymentAmount, "UTANG_PAYMENT", "Payment from " + targetNeighbor, "Utang");

            ArrayList<Product> paymentDetails = new ArrayList<>();
            paymentDetails.add(dummyPayment);

            String deviceName = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;

            dbHelper.saveTransaction("Utang Payment (" + targetNeighbor + ")", "Cash", paymentAmount, paymentAmount, remainingBalance, deviceName, paymentDetails);
            dbHelper.logDebt(targetNeighbor, -paymentAmount);

            Toast.makeText(UtangActivity.this, "Payment of ₱" + String.format("%.2f", paymentAmount) + " logged!", Toast.LENGTH_SHORT).show();

            etSearchNeighbor.setText("");
            loadUtangData();
            paymentDialog.dismiss();
        });

        paymentDialog.setContentView(sheetView);
        paymentDialog.show();
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