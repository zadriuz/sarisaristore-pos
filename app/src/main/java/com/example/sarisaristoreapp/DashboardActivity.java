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

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(DashboardActivity.this, MainActivity.class));
                finish();
            }
        });

        getWindow().setStatusBarColor(getResources().getColor(R.color.kanto_primary));

        dbHelper = new DatabaseHelper(this);

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

                tvMessage.setText("This is the Admin Dashboard. Track today's live revenue, monitor top-selling items, review total outstanding Utang, and officially close your shift.");

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

        // --- DRAWER ENGINE START ---
        androidx.drawerlayout.widget.DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        com.google.android.material.navigation.NavigationView navView = findViewById(R.id.nav_view);

        navView.setCheckedItem(R.id.nav_dashboard);

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
                startActivity(new android.content.Intent(this, MainActivity.class));
                finish();
            } else if (id == R.id.nav_history) {
                startActivity(new android.content.Intent(this, HistoryActivity.class));
                finish();
            } else if (id == R.id.nav_utang) {
                startActivity(new android.content.Intent(this, UtangActivity.class));
                finish();
            } else if (id == R.id.nav_inventory) {
                startActivity(new android.content.Intent(this, InventoryActivity.class));
                finish();
            } else if (id == R.id.nav_gcash) {
                startActivity(new android.content.Intent(this, GCashActivity.class));
                finish();
            } else if (id == R.id.nav_dashboard) {
                drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START);
            } else if (id == R.id.nav_logout) {
                prefs.edit().clear().apply();
                android.widget.Toast.makeText(this, "Logged out successfully", android.widget.Toast.LENGTH_SHORT).show();
                startActivity(new android.content.Intent(this, LoginActivity.class));
                finish();
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

            tvCash.setText(todayRevenue);
            tvGCash.setText("₱0.00");
            tvUtang.setText(todayUtang);

            btnCancel.setOnClickListener(view -> customDialog.dismiss());

            btnConfirm.setOnClickListener(view -> {
                Toast.makeText(this, "Shift Closed. Great job today!", Toast.LENGTH_SHORT).show();
                prefs.edit().putBoolean("IsAdmin", false).apply();
                customDialog.dismiss();
                finishAffinity();
            });

            customDialog.show();
        });

        dbHelper.getDashboardStats((totalRevenue, totalUtang, topItem, topQty, dailySales) -> {
            tvDashRevenue.setText(String.format("₱%,.2f", totalRevenue));
            tvDashUtang.setText(String.format("₱%,.2f", totalUtang));
            tvDashTopItem.setText(topItem);
            tvDashTopQty.setText(topQty + " sold");

            double thisWeekTotal = 0.0;
            double lastWeekTotal = 0.0;
            ArrayList<Double> allSales = new ArrayList<>(dailySales.values());
            int totalDays = allSales.size();

            for (int i = 0; i < totalDays; i++) {
                if (i >= totalDays - 7) {
                    thisWeekTotal += allSales.get(i);
                } else if (i >= totalDays - 14) {
                    lastWeekTotal += allSales.get(i);
                }
            }

            if (lastWeekTotal == 0 && thisWeekTotal > 0) {
                tvDashTrend.setText("↗ +100% vs last week");
                tvDashTrend.setTextColor(android.graphics.Color.parseColor("#16A34A"));
            } else if (lastWeekTotal == 0 && thisWeekTotal == 0) {
                tvDashTrend.setText("0% vs last week");
                tvDashTrend.setTextColor(android.graphics.Color.parseColor("#64748B"));
            } else {
                double percentChange = ((thisWeekTotal - lastWeekTotal) / lastWeekTotal) * 100;
                if (percentChange >= 0) {
                    tvDashTrend.setText(String.format("↗ +%.1f%% vs last week", percentChange));
                    tvDashTrend.setTextColor(android.graphics.Color.parseColor("#16A34A"));
                } else {
                    tvDashTrend.setText(String.format("↘ %.1f%% vs last week", percentChange));
                    tvDashTrend.setTextColor(android.graphics.Color.parseColor("#DC2626"));
                }
            }

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
            int startIndex = Math.max(0, dailySales.size() - 7);
            int currentIndex = 0;
            float xPos = 0f;

            for (java.util.Map.Entry<String, Double> entry : dailySales.entrySet()) {
                if (currentIndex >= startIndex) {
                    entries.add(new Entry(xPos, entry.getValue().floatValue()));

                    String label = entry.getKey();
                    if (label.equals("Unknown")) {
                        label = "?";
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
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(android.graphics.Color.parseColor("#37533C"));
        dataSet.setCircleRadius(5f);

        dataSet.setDrawValues(false);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(android.graphics.Color.parseColor("#37533C"));

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

        salesLineChart.getAxisLeft().setSpaceTop(30f);
        salesLineChart.getAxisLeft().setSpaceBottom(20f);

        com.github.mikephil.charting.components.XAxis xAxis = salesLineChart.getXAxis();
        xAxis.setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(dateLabels));
        xAxis.setGranularity(1f);

        salesLineChart.animateX(1000);
        salesLineChart.invalidate();
    }
}