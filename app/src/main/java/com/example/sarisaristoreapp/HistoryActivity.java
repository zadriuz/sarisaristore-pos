package com.example.sarisaristoreapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sarisaristoreapp.models.TransactionRecord;

import java.util.ArrayList;

public class HistoryActivity extends AppCompatActivity {

    DatabaseHelper dbHelper;
    androidx.recyclerview.widget.RecyclerView listHistory;

    // THE TWO LISTS: Master holds everything, allRecords is what the screen sees!
    ArrayList<TransactionRecord> masterRecords = new ArrayList<>();
    ArrayList<TransactionRecord> allRecords = new ArrayList<>();
    HistoryAdapter adapter;

    ImageView btnMenu;
    TextView tvTotalSales;
    TextView tvSalesLabel;
    Spinner spinnerDateFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_history);
        getWindow().setStatusBarColor(getResources().getColor(R.color.kanto_primary));

        dbHelper = new DatabaseHelper(this);
        listHistory = findViewById(R.id.listHistory);
        tvTotalSales = findViewById(R.id.tvTotalSales);
        tvSalesLabel = findViewById(R.id.tvSalesLabel);
        btnMenu = findViewById(R.id.btnMenu);
        spinnerDateFilter = findViewById(R.id.spinnerDateFilter);

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
                tvMessage.setText("This is the transaction history. Filter past sales by date and tap on any receipt to view the specific items sold and the payment method used.");
                // ------------------------------------------

                btnGotIt.setOnClickListener(view -> infoDialog.dismiss());

                infoDialog.show();
            });
        }

        // WAKE UP THE SECURE MENU
        btnMenu.setOnClickListener(v -> showAdminMenu());

        // --- WIRE THE SPINNER OPTIONS ---
        String[] filterOptions = {"Today", "Yesterday", "Last 7 Days", "All Time"};

        // THE FIX: Standardized Custom Spinner Layout!
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,R .layout.item_spinner_selected, filterOptions);
        spinnerAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerDateFilter.setAdapter(spinnerAdapter);

        spinnerDateFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyDateFilter(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        listHistory.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));

        adapter = new HistoryAdapter(allRecords, clickedRecord -> {
            String targetReceiptId = clickedRecord.getTransactionId();
            String receiptDate = clickedRecord.getTransactionDate() != null ? clickedRecord.getTransactionDate() : "Unknown Date";
            double receiptTotal = clickedRecord.getTotalAmount();

            String paymentMethod = clickedRecord.getPaymentMethod() != null ? clickedRecord.getPaymentMethod() : "";
            String purseType = clickedRecord.getPurseType() != null ? clickedRecord.getPurseType() : "";

            if (targetReceiptId == null) {
                Toast.makeText(HistoryActivity.this, "Error: Old or corrupted receipt data.", Toast.LENGTH_SHORT).show();
                return;
            }

            dbHelper.getTransactionItems(targetReceiptId, itemList -> {
                StringBuilder receiptMessage = new StringBuilder();

                if (paymentMethod.contains("Utang")) {
                    receiptMessage.append("Charge To: ").append(paymentMethod).append("\n-------------------\n");
                } else if (purseType.contains("Utang Payment")) {
                    receiptMessage.append("Payment From: ").append(purseType).append("\n-------------------\n");
                }

                for(String item : itemList){
                    receiptMessage.append(item).append("\n");
                }

                if(itemList.isEmpty()){
                    receiptMessage.append("No specific items recorded.");
                }

                android.app.Dialog receiptDialog = new android.app.Dialog(HistoryActivity.this);
                View sheetView = getLayoutInflater().inflate(R.layout.layout_receipt_sheet, null);

                TextView tvDate = sheetView.findViewById(R.id.tvReceiptDate);
                TextView tvItems = sheetView.findViewById(R.id.tvReceiptItems);
                TextView tvTotal = sheetView.findViewById(R.id.tvReceiptTotal);
                TextView tvTotalLabel = sheetView.findViewById(R.id.tvReceiptTotalLabel);
                ImageView btnClose = sheetView.findViewById(R.id.btnCloseReceipt);

                tvDate.setText(receiptDate.replace(" ", "\n"));
                tvItems.setText(receiptMessage.toString().trim());

                if (purseType.contains("Utang Payment")) {
                    tvTotalLabel.setText("Rem. Balance");
                    tvTotal.setText(String.format("₱%.2f", clickedRecord.getChangeAmount()));
                } else {
                    tvTotalLabel.setText("Total");
                    tvTotal.setText(String.format("₱%.2f", receiptTotal));
                }

                btnClose.setOnClickListener(v -> receiptDialog.dismiss());
                receiptDialog.setContentView(sheetView);

                if (receiptDialog.getWindow() != null) {
                    receiptDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                }

                receiptDialog.show();
            });
        });

        listHistory.setAdapter(adapter);

        // 1. Radio the Cloud for the overall History
        dbHelper.getAllTransactions(historyList -> {
            masterRecords.clear();
            masterRecords.addAll(historyList);

            // Force it to filter for "Today" as soon as the data arrives!
            applyDateFilter(spinnerDateFilter.getSelectedItemPosition());
        });
    }

    // --- THE TIME MACHINE ENGINE ---
    private void applyDateFilter(int filterIndex) {
        allRecords.clear();
        double grandTotal = 0.0;

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        java.util.Calendar cal = java.util.Calendar.getInstance();

        String todayStr = sdf.format(cal.getTime());

        cal.add(java.util.Calendar.DAY_OF_YEAR, -1);
        String yesterdayStr = sdf.format(cal.getTime());

        cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_YEAR, -7);
        java.util.Date sevenDaysAgo = cal.getTime();

        for (TransactionRecord record : masterRecords) {
            String fullDate = record.getTransactionDate();
            if (fullDate == null) continue;
            String justDate = fullDate.length() >= 10 ? fullDate.substring(0, 10) : fullDate;

            boolean match = false;
            if (filterIndex == 0) { // Today
                match = justDate.equals(todayStr);
                tvSalesLabel.setText("TOTAL SALES TODAY");
            } else if (filterIndex == 1) { // Yesterday
                match = justDate.equals(yesterdayStr);
                tvSalesLabel.setText("TOTAL SALES YESTERDAY");
            } else if (filterIndex == 2) { // Last 7 Days
                try {
                    java.util.Date recordDate = sdf.parse(justDate);
                    if (recordDate != null && !recordDate.before(sevenDaysAgo)) match = true;
                } catch (Exception e) { e.printStackTrace(); }
                tvSalesLabel.setText("SALES (LAST 7 DAYS)");
            } else if (filterIndex == 3) { // All Time
                match = true;
                tvSalesLabel.setText("ALL TIME SALES");
            }

            if (match) {
                allRecords.add(record);
                String method = record.getPaymentMethod() != null ? record.getPaymentMethod() : "";
                if (!method.contains("Utang")) {
                    grandTotal += record.getTotalAmount();
                }
            }
        }

        tvTotalSales.setText(String.format("₱%.2f", grandTotal));
        adapter.notifyDataSetChanged();
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