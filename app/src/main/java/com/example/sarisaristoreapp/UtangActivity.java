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

                tvMessage.setText("This is the Utang ledger. Track neighbor debts, record partial or full payments, and view borrowing history to manage the store's credit.");

                btnGotIt.setOnClickListener(view -> infoDialog.dismiss());

                infoDialog.show();
            });
        }

        listUtang.setLayoutManager(new LinearLayoutManager(this));

        adapter = new UtangAdapter(neighborNames, neighborBalances, new UtangAdapter.OnUtangActionListener() {
            @Override
            public void onViewDetails(String targetNeighbor, double currentBalance) {
                dbHelper.getNeighborHistory(targetNeighbor, historyLines -> {

                    StringBuilder historyMessage = new StringBuilder();
                    for (String line : historyLines) {
                        historyMessage.append("• ").append(line).append("\n\n");
                    }
                    if (historyLines.isEmpty()) {
                        historyMessage.append("No detailed timeline found.");
                    }

                    android.app.Dialog statementDialog = new android.app.Dialog(UtangActivity.this);
                    View sheetView = getLayoutInflater().inflate(R.layout.layout_statement_sheet, null);

                    TextView tvName = sheetView.findViewById(R.id.tvStatementName);
                    TextView tvHistory = sheetView.findViewById(R.id.tvStatementHistory);
                    TextView tvBalance = sheetView.findViewById(R.id.tvStatementBalance);
                    ImageButton btnClose = sheetView.findViewById(R.id.btnCloseStatement);
                    Button btnSettle = sheetView.findViewById(R.id.btnSettleStatement);

                    android.content.SharedPreferences prefs = getSharedPreferences("StoreSession", MODE_PRIVATE);
                    boolean isAdmin = prefs.getBoolean("IsAdmin", false);

                    if (!isAdmin) {
                        btnSettle.setVisibility(View.GONE);
                    }

                    tvName.setText(targetNeighbor);
                    tvHistory.setText(historyMessage.toString());
                    tvBalance.setText(String.format("₱%.2f", currentBalance));

                    btnClose.setOnClickListener(v1 -> statementDialog.dismiss());

                    btnSettle.setOnClickListener(v1 -> {
                        statementDialog.dismiss();
                        showPaymentInput(targetNeighbor, currentBalance);
                    });

                    statementDialog.setContentView(sheetView);

                    if (statementDialog.getWindow() != null) {
                        statementDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                    }

                    statementDialog.show();
                });
            }

            @Override
            public void onSettleDebt(String targetNeighbor, double currentBalance) {
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

        loadUtangData();

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

        tvTitle.setText("Receive Payment: " + targetNeighbor);
        tvBalance.setText(String.format("Current Balance: ₱%.2f", currentBalance));

        btnQuickFull.setOnClickListener(v -> etAmount.setText(String.valueOf(currentBalance)));

        btnQuick100.setOnClickListener(v -> {
            String currentStr = etAmount.getText().toString().trim();
            double current = currentStr.isEmpty() ? 0.0 : Double.parseDouble(currentStr);
            etAmount.setText(String.valueOf(current + 100));
        });

        btnQuick500.setOnClickListener(v -> {
            String currentStr = etAmount.getText().toString().trim();
            double current = currentStr.isEmpty() ? 0.0 : Double.parseDouble(currentStr);
            etAmount.setText(String.valueOf(current + 500));
        });

        btnMenu.setOnClickListener(v -> showAdminMenu());

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

            // --- GEMINI'S REVISED OVERPAYMENT ENGINE ---
            double settleAmount;
            double change = 0.0;
            String transactionTitle = "Utang Payment (" + targetNeighbor + ")";

            if (paymentAmount > currentBalance) {
                settleAmount = currentBalance;
                change = paymentAmount - currentBalance;
                transactionTitle += " [Overpaid]";
            } else {
                settleAmount = paymentAmount;
            }

            // THE FIX 1: This ensures remaining balance hits exactly 0.00 on overpayments
            double remainingBalance = currentBalance - settleAmount;

            ArrayList<Product> paymentDetails = new ArrayList<>();

            // Log the actual debt being erased
            Product dummyPayment = new Product(0, 1, 0.0, settleAmount, "UTANG_PAYMENT", "Payment from " + targetNeighbor, "Utang");
            paymentDetails.add(dummyPayment);

            // Log the change dynamically deducted from the Tindahan purse
            if (change > 0) {
                Product changeDeduction = new Product(0, 1, 0.0, -change, "CHANGE_DISPENSED", "Change deducted from Tindahan", "Cash");
                paymentDetails.add(changeDeduction);
            }

            String deviceName = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;

            // THE FIX 2: Pass paymentAmount (Cash), currentBalance (Total Debt), and remainingBalance (0.00)
            // THE PERFECT MAPPING: Original Debt | Cash Given | Remaining Balance
            dbHelper.saveTransaction(transactionTitle, "Cash", currentBalance, paymentAmount, remainingBalance, deviceName, paymentDetails);

            // Only reduce the Utang by the exact debt amount, preventing negative balances
            dbHelper.logDebt(targetNeighbor, -settleAmount);

            if (change > 0) {
                Toast.makeText(UtangActivity.this, "Debt cleared! Give Change: ₱" + String.format("%.2f", change), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(UtangActivity.this, "Payment of ₱" + String.format("%.2f", settleAmount) + " logged!", Toast.LENGTH_SHORT).show();
            }
            // --- END OF ENGINE ---

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

        Button btnHistory = sheet.findViewById(R.id.btnMenuHistory);
        Button btnUtang = sheet.findViewById(R.id.btnMenuUtang);
        Button btnInventory = sheet.findViewById(R.id.btnMenuInventory);
        Button btnGcash = sheet.findViewById(R.id.btnMenuGcash);
        Button btnAdmin = sheet.findViewById(R.id.btnMenuAdmin);
        Button btnCheckout = sheet.findViewById(R.id.btnMenuCheckout);

        android.content.SharedPreferences prefs = getSharedPreferences("StoreSession", MODE_PRIVATE);
        boolean isAdmin = prefs.getBoolean("IsAdmin", false);

        if (!isAdmin) {
            btnInventory.setVisibility(View.GONE);
            btnGcash.setVisibility(View.GONE);
            btnAdmin.setVisibility(View.GONE);
        }

        btnCheckout.setOnClickListener(v -> { dialog.dismiss(); startActivity(new Intent(this, MainActivity.class)); finish(); });
        btnHistory.setOnClickListener(v -> { dialog.dismiss(); startActivity(new Intent(this, HistoryActivity.class)); finish(); });
        btnUtang.setOnClickListener(v -> { dialog.dismiss(); startActivity(new Intent(this, UtangActivity.class)); finish(); });

        if (isAdmin) {
            btnInventory.setOnClickListener(v -> { dialog.dismiss(); startActivity(new Intent(this, InventoryActivity.class)); finish(); });
            btnGcash.setOnClickListener(v -> { dialog.dismiss(); startActivity(new Intent(this, GCashActivity.class)); finish(); });
            btnAdmin.setOnClickListener(v -> { dialog.dismiss(); startActivity(new Intent(this, DashboardActivity.class)); });
        }

        dialog.setContentView(sheet);
        com.google.android.material.bottomsheet.BottomSheetBehavior behavior = dialog.getBehavior();
        behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
        dialog.show();
    }
}