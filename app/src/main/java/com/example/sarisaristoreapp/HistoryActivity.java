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

    ArrayList<TransactionRecord> masterRecords = new ArrayList<>();
    ArrayList<TransactionRecord> allRecords = new ArrayList<>();
    HistoryAdapter adapter;

    TextView tvTotalSales;
    TextView tvSalesLabel;
    Spinner spinnerDateFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(HistoryActivity.this, MainActivity.class));
                finish();
            }
        });

        getWindow().setStatusBarColor(getResources().getColor(R.color.kanto_primary));
        dbHelper = new DatabaseHelper(this);

        // --- DRAWER ENGINE START ---
        androidx.drawerlayout.widget.DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        com.google.android.material.navigation.NavigationView navView = findViewById(R.id.nav_view);

        navView.setCheckedItem(R.id.nav_history);

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
                drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START);
            } else if (id == R.id.nav_utang) {
                startActivity(new Intent(this, UtangActivity.class)); finish();
            } else if (id == R.id.nav_inventory) {
                startActivity(new Intent(this, InventoryActivity.class)); finish();
            } else if (id == R.id.nav_gcash) {
                startActivity(new Intent(this, GCashActivity.class)); finish();
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

        listHistory = findViewById(R.id.listHistory);
        tvTotalSales = findViewById(R.id.tvTotalSales);
        tvSalesLabel = findViewById(R.id.tvSalesLabel);
        spinnerDateFilter = findViewById(R.id.spinnerDateFilter);

        ImageView btnInfo = findViewById(R.id.btnInfo);
        if (btnInfo != null) {
            btnInfo.setOnClickListener(v -> {
                android.app.Dialog infoDialog = new android.app.Dialog(this);
                infoDialog.setContentView(R.layout.layout_info_dialog);
                if (infoDialog.getWindow() != null) infoDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                TextView tvMessage = infoDialog.findViewById(R.id.tvInfoMessage);
                Button btnGotIt = infoDialog.findViewById(R.id.btnInfoGotIt);
                tvMessage.setText("This is the transaction history. Filter past sales by date and tap on any receipt to view the specific items sold and the payment method used.");
                btnGotIt.setOnClickListener(view -> infoDialog.dismiss());
                infoDialog.show();
            });
        }

        String[] filterOptions = {"Today", "Yesterday", "Last 7 Days", "All Time"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,R .layout.item_spinner_selected, filterOptions);
        spinnerAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerDateFilter.setAdapter(spinnerAdapter);

        Spinner spinnerHistoryCategory = findViewById(R.id.spinnerHistoryCategory);
        String[] categoryOptions = {"All", "GCash", "Tindahan", "Softdrinks", "Print", "Utang"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_selected, categoryOptions);
        catAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerHistoryCategory.setAdapter(catAdapter);

        spinnerHistoryCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { applyMasterFilter(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerDateFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { applyMasterFilter(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
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
                } else if (purseType.contains("Utang Payment") || paymentMethod.contains("Remitted")) {
                    double originalDebt = clickedRecord.getTotalAmount();
                    double cashHandedOver = clickedRecord.getCashGiven();
                    double remainingBal = clickedRecord.getChangeAmount();

                    if (cashHandedOver > originalDebt) {
                        receiptMessage.append(String.format("Paid ₱%.2f out of ₱%.2f [Overpaid]\n(Change Dispensed: ₱%.2f)", cashHandedOver, originalDebt, (cashHandedOver - originalDebt)))
                                .append("\n-------------------\n");
                    } else {
                        receiptMessage.append(String.format("Paid ₱%.2f out of ₱%.2f", cashHandedOver, originalDebt))
                                .append("\n-------------------\n");
                    }
                }

                for(String item : itemList){ receiptMessage.append(item).append("\n"); }
                if(itemList.isEmpty()){ receiptMessage.append("No specific items recorded."); }

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
                if (receiptDialog.getWindow() != null) receiptDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                receiptDialog.show();
            });
        });

        listHistory.setAdapter(adapter);

        dbHelper.getAllTransactions(historyList -> {
            masterRecords.clear();
            masterRecords.addAll(historyList);
            applyMasterFilter();
        });
    }

    private void applyMasterFilter() {
        allRecords.clear();
        double grandTotal = 0.0;

        int dateIndex = spinnerDateFilter.getSelectedItemPosition();
        String selectedCategory = "All";
        Spinner spinnerCategory = findViewById(R.id.spinnerHistoryCategory);
        if (spinnerCategory != null && spinnerCategory.getSelectedItem() != null) {
            selectedCategory = spinnerCategory.getSelectedItem().toString();
        }

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

            boolean dateMatch = false;
            if (dateIndex == 0) {
                dateMatch = justDate.equals(todayStr);
                tvSalesLabel.setText("SALES: TODAY");
            } else if (dateIndex == 1) {
                dateMatch = justDate.equals(yesterdayStr);
                tvSalesLabel.setText("SALES: YESTERDAY");
            } else if (dateIndex == 2) {
                try {
                    java.util.Date recordDate = sdf.parse(justDate);
                    if (recordDate != null && !recordDate.before(sevenDaysAgo)) dateMatch = true;
                } catch (Exception e) { e.printStackTrace(); }
                tvSalesLabel.setText("SALES: LAST 7 DAYS");
            } else if (dateIndex == 3) {
                dateMatch = true;
                tvSalesLabel.setText("SALES: ALL TIME");
            }

            boolean categoryMatch = false;
            String recordType = record.getPurseType() != null ? record.getPurseType() : "Unknown";
            String payMethod = record.getPaymentMethod() != null ? record.getPaymentMethod() : "Unknown";

            if (selectedCategory.equals("All")) {
                categoryMatch = true;
            } else if (selectedCategory.equals("Utang")) {
                if (payMethod.contains("Utang")) {
                    categoryMatch = true;
                }
            } else {
                if (recordType.contains(selectedCategory) || record.getTransactionId().contains(selectedCategory.toUpperCase())) {
                    categoryMatch = true;
                }
            }

            if (dateMatch && categoryMatch) {
                allRecords.add(record);
                if (!payMethod.contains("Utang") || selectedCategory.equals("Utang")) {
                    grandTotal += record.getTotalAmount();
                }
            }
        }

        tvTotalSales.setText(String.format("₱%.2f", grandTotal));
        adapter.notifyDataSetChanged();
    }
}