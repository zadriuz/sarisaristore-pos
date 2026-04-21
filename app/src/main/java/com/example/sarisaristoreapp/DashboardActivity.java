package com.example.sarisaristoreapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

// MPAndroidChart Library Imports
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

public class DashboardActivity extends AppCompatActivity {

    TextView tvDashRevenue, tvDashTopItem, tvDashTopQty, tvDashUtang;
    DatabaseHelper dbHelper;
    LineChart salesLineChart;
    TextView tvDashTrend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // TELEPORT BACK TO POS CHECKOUT
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(DashboardActivity.this, MainActivity.class));
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
                tvMessage.setText("This is the Admin Dashboard. Track today's live revenue, monitor top-selling items, review total outstanding Utang, and officially close your shift.");
                // ------------------------------------------

                btnGotIt.setOnClickListener(view -> infoDialog.dismiss());

                infoDialog.show();
            });
        }

        tvDashRevenue = findViewById(R.id.tvDashRevenue);
        tvDashTopItem = findViewById(R.id.tvDashTopItem);
        tvDashTopQty = findViewById(R.id.tvDashTopQty);
        tvDashUtang = findViewById(R.id.tvDashUtang);
        salesLineChart = findViewById(R.id.salesLineChart);

        tvDashTrend = findViewById(R.id.tvDashTrend);

        // 1. Menu Engine
        ImageView btnMenu = findViewById(R.id.btnMenu);

        btnMenu.setOnClickListener(v -> showAdminMenu());

        // 2. Action Buttons (Reconciliation!)
        Button btnReconciliation = findViewById(R.id.btnZReading);

        btnReconciliation.setOnClickListener(v -> {
            String todayRevenue = tvDashRevenue.getText().toString();
            String todayUtang = tvDashUtang.getText().toString();

            android.app.Dialog customDialog = new android.app.Dialog(this);
            customDialog.setContentView(R.layout.layout_end_shift_dialog);

            if (customDialog.getWindow() != null) {
                customDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            }

            android.widget.TextView tvCash = customDialog.findViewById(R.id.tvEndShiftCash);
            android.widget.TextView tvGCash = customDialog.findViewById(R.id.tvEndShiftGCash);
            android.widget.TextView tvUtang = customDialog.findViewById(R.id.tvEndShiftUtang);
            android.widget.Button btnCancel = customDialog.findViewById(R.id.btnEndShiftCancel);
            android.widget.Button btnConfirm = customDialog.findViewById(R.id.btnEndShiftConfirm);

            // Stamp the live math onto the custom UI
            tvCash.setText(todayRevenue);
            tvGCash.setText("₱0.00"); // Set to 0.00 unless you separate GCash in your Dashboard math later
            tvUtang.setText(todayUtang);

            btnCancel.setOnClickListener(view -> customDialog.dismiss());

            btnConfirm.setOnClickListener(view -> {
                Toast.makeText(this, "Shift Closed. Great job today!", Toast.LENGTH_SHORT).show();

                android.content.SharedPreferences prefs = getSharedPreferences("StoreSession", MODE_PRIVATE);
                prefs.edit().putBoolean("IsAdmin", false).apply();

                customDialog.dismiss();
                finishAffinity();
            });

            customDialog.show();
        });

        // 3. Fetch the Live Analytics!
        // 3. Fetch the Live Analytics!
        dbHelper.getDashboardStats((totalRevenue, totalUtang, topItem, topQty, dailySales) -> {
            tvDashRevenue.setText(String.format("₱%,.2f", totalRevenue));
            tvDashUtang.setText(String.format("₱%,.2f", totalUtang));
            tvDashTopItem.setText(topItem);
            tvDashTopQty.setText(topQty + " sold");

            // --- THE TREND MATH ENGINE ---
            double thisWeekTotal = 0.0;
            double lastWeekTotal = 0.0;
            ArrayList<Double> allSales = new ArrayList<>(dailySales.values());
            int totalDays = allSales.size();

            for (int i = 0; i < totalDays; i++) {
                if (i >= totalDays - 7) {
                    thisWeekTotal += allSales.get(i); // Last 7 days
                } else if (i >= totalDays - 14) {
                    lastWeekTotal += allSales.get(i); // The 7 days before that
                }
            }

            if (lastWeekTotal == 0 && thisWeekTotal > 0) {
                tvDashTrend.setText("↗ +100% vs last week");
                tvDashTrend.setTextColor(android.graphics.Color.parseColor("#16A34A")); // Green
            } else if (lastWeekTotal == 0 && thisWeekTotal == 0) {
                tvDashTrend.setText("0% vs last week");
                tvDashTrend.setTextColor(android.graphics.Color.parseColor("#64748B")); // Gray
            } else {
                double percentChange = ((thisWeekTotal - lastWeekTotal) / lastWeekTotal) * 100;
                if (percentChange >= 0) {
                    tvDashTrend.setText(String.format("↗ +%.1f%% vs last week", percentChange));
                    tvDashTrend.setTextColor(android.graphics.Color.parseColor("#16A34A")); // Green
                } else {
                    tvDashTrend.setText(String.format("↘ %.1f%% vs last week", percentChange));
                    tvDashTrend.setTextColor(android.graphics.Color.parseColor("#DC2626")); // Red
                }
            }

            // Pass the live data directly into the graph!
            setupLineChart(dailySales);
        });
    }

    private void setupLineChart(java.util.TreeMap<String, Double> dailySales) {
        ArrayList<Entry> entries = new ArrayList<>();
        final ArrayList<String> dateLabels = new ArrayList<>();

        if (dailySales.isEmpty()) {
            entries.add(new Entry(0f, 0f));
            dateLabels.add("No Data");
        } else {
            // We only want the last 7 days maximum!
            int startIndex = Math.max(0, dailySales.size() - 7);
            int currentIndex = 0;
            float xPos = 0f;

            for (java.util.Map.Entry<String, Double> entry : dailySales.entrySet()) {
                if (currentIndex >= startIndex) {
                    entries.add(new Entry(xPos, entry.getValue().floatValue()));

                    // BULLETPROOF LABEL PARSER
                    String label = entry.getKey();
                    if (label.equals("Unknown")) {
                        label = "?"; // If no date exists, show a question mark instead of "known"
                    } else if (label.length() > 5 && label.contains("-")) {
                        String[] parts = label.split("-");
                        if(parts.length >= 2) {
                            label = parts[parts.length - 2] + "/" + parts[parts.length - 1];
                        }
                    } else if (label.length() > 5) {
                        label = label.substring(0, 5);
                    }
                    dateLabels.add(label);
                    xPos++;
                }
                currentIndex++;
            }
        }

        LineDataSet dataSet = new LineDataSet(entries, "Daily Revenue");
        dataSet.setColor(android.graphics.Color.parseColor("#37533C"));
        dataSet.setLineWidth(4f);
        dataSet.setDrawCircles(true); // Let's turn circles ON so she can see the exact days!
        dataSet.setCircleColor(android.graphics.Color.parseColor("#37533C"));
        // Push the text slightly further away from the line
        dataSet.setCircleRadius(5f);

        // Turn the floating numbers ON!
        dataSet.setDrawValues(false);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(android.graphics.Color.parseColor("#37533C"));

        // Format the numbers to look like money with commas!
        dataSet.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getPointLabel(Entry entry) {
                return String.format("₱%,.0f", entry.getY());
            }
        });


        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(android.graphics.Color.parseColor("#D1FAE5"));
        dataSet.setFillAlpha(80);

        LineData lineData = new LineData(dataSet);
        salesLineChart.setData(lineData);

        salesLineChart.getDescription().setEnabled(false);
        salesLineChart.getLegend().setEnabled(false);
        salesLineChart.getAxisRight().setEnabled(false);

        // Add vertical breathing room to the chart!
        salesLineChart.getAxisLeft().setSpaceTop(30f);
        salesLineChart.getAxisLeft().setSpaceBottom(20f);

        // Inject the custom date labels onto the X-Axis!
        com.github.mikephil.charting.components.XAxis xAxis = salesLineChart.getXAxis();
        xAxis.setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(dateLabels));
        xAxis.setGranularity(1f);

        salesLineChart.animateX(1000);
        salesLineChart.invalidate();
    }

    private void showAdminMenu() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View sheet = getLayoutInflater().inflate(R.layout.layout_bottom_sheet, null);

        // Define buttons
        Button btnHistory = sheet.findViewById(R.id.btnMenuHistory);
        Button btnUtang = sheet.findViewById(R.id.btnMenuUtang);
        Button btnInventory = sheet.findViewById(R.id.btnMenuInventory);
        Button btnGcash = sheet.findViewById(R.id.btnMenuGcash);
        Button btnAdmin = sheet.findViewById(R.id.btnMenuAdmin);
        Button btnCheckout = sheet.findViewById(R.id.btnMenuCheckout);

        // SECURE THE GATE: Check if the user is an admin
        android.content.SharedPreferences prefs = getSharedPreferences("StoreSession", MODE_PRIVATE);
        boolean isAdmin = prefs.getBoolean("IsAdmin", false);

        if (!isAdmin) {
            btnInventory.setVisibility(View.GONE);
            btnAdmin.setVisibility(View.GONE);
            btnGcash.setVisibility(View.GONE);
        }

        // Logic (Navigation)
        btnCheckout.setOnClickListener(v -> { dialog.dismiss(); startActivity(new Intent(this, MainActivity.class)); finish(); });
        btnHistory.setOnClickListener(v -> { dialog.dismiss(); startActivity(new Intent(this, HistoryActivity.class)); finish(); });
        btnUtang.setOnClickListener(v -> { dialog.dismiss(); startActivity(new Intent(this, UtangActivity.class)); finish(); });

        if (isAdmin) {
            btnInventory.setOnClickListener(v -> { dialog.dismiss(); startActivity(new Intent(this, InventoryActivity.class)); finish(); });
            btnGcash.setOnClickListener(v -> { dialog.dismiss(); startActivity(new Intent(this, GCashActivity.class)); finish(); });
            btnAdmin.setOnClickListener(v -> { dialog.dismiss(); startActivity(new Intent(this, DashboardActivity.class)); finish(); });
        }

        dialog.setContentView(sheet);
        dialog.getBehavior().setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
        dialog.getBehavior().setDraggable(false);
        dialog.show();
    }
}