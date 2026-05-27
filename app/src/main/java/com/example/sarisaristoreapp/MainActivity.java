package com.example.sarisaristoreapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sarisaristoreapp.models.Product;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private long backPressedTime;

    private DrawerLayout drawerLayout;

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        idleHandler.removeCallbacks(idleRunnable);
        idleHandler.postDelayed(idleRunnable, 15 * 60 * 1000);
    }

    android.os.Handler idleHandler = new android.os.Handler();
    Runnable idleRunnable = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(MainActivity.this, "Session expired due to inactivity.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    };

    HashMap<String, Product> globalInventory = new HashMap<>();
    ArrayList<String> searchList = new ArrayList<>();
    ContainsArrayAdapter searchAdapter;

    Button btnCheckout;
    EditText etBarcode, etCashRendered;
    Spinner spinnerPurse;
    TextView tvTotal, tvChange;
    ImageView btnMenu;

    DatabaseHelper dbHelper;
    public static ArrayList<Product> checkoutCart = new ArrayList<>();
    public static double cartTotal = 0.0;
    RecyclerView listCart;
    CartAdapter cartAdapter;
    String scannedCode;

    String activeCashier;
    boolean isAdmin;

    private final androidx.activity.result.ActivityResultLauncher<com.journeyapps.barcodescanner.ScanOptions> barcodeLauncher =
            registerForActivityResult(new com.journeyapps.barcodescanner.ScanContract(), result -> {
                if(result.getContents() == null) {
                    Toast.makeText(MainActivity.this, "Scan cancelled", Toast.LENGTH_SHORT).show();
                } else {
                    scannedCode = result.getContents();
                    if(globalInventory.containsKey(scannedCode)){
                        Product foundProduct = globalInventory.get(scannedCode);

                        int currentCartQty = 0;
                        Product existingCartItem = null;

                        for (Product p : checkoutCart) {
                            if (p.getBarcode().equals(scannedCode)) {
                                existingCartItem = p;
                                currentCartQty = p.getCurrentQuantity();
                                break;
                            }
                        }

                        if (currentCartQty + 1 > foundProduct.getCurrentQuantity()) {
                            Toast.makeText(MainActivity.this, "Max stock reached!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (existingCartItem != null) {
                            existingCartItem.setCurrentQuantity(currentCartQty + 1);
                        } else {
                            Product receiptItem = new Product(0, 1, foundProduct.getCostPrice(), foundProduct.getSellingPrice(), scannedCode, foundProduct.getProductName(), foundProduct.getCategory());
                            checkoutCart.add(receiptItem);
                        }

                        cartAdapter.notifyDataSetChanged();
                        recalculateGrandTotal();
                    }else{
                        showItemNotFoundDialog(scannedCode);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    android.app.Dialog customDialog = new android.app.Dialog(MainActivity.this);
                    customDialog.setContentView(R.layout.layout_alert_dialog);

                    if (customDialog.getWindow() != null) {
                        customDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                    }

                    android.widget.TextView tvTitle = customDialog.findViewById(R.id.tvAlertTitle);
                    android.widget.TextView tvMessage = customDialog.findViewById(R.id.tvAlertMessage);
                    android.widget.Button btnCancel = customDialog.findViewById(R.id.btnAlertCancel);
                    android.widget.Button btnConfirm = customDialog.findViewById(R.id.btnAlertConfirm);

                    tvTitle.setText("Log Out?");
                    tvMessage.setText("Are you sure you want to log out?");
                    btnConfirm.setText("Log Out");

                    btnCancel.setOnClickListener(view -> customDialog.dismiss());

                    btnConfirm.setOnClickListener(view -> {
                        customDialog.dismiss();
                        performLogout();
                    });

                    customDialog.show();
                } else {
                    Toast.makeText(getBaseContext(), "Press back again to log out", Toast.LENGTH_SHORT).show();
                }
                backPressedTime = System.currentTimeMillis();
            }
        });

        SharedPreferences prefs = getSharedPreferences("StoreSession", MODE_PRIVATE);
        activeCashier = prefs.getString("ActiveUser", "Unknown");
        isAdmin = prefs.getBoolean("IsAdmin", false);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navView = findViewById(R.id.nav_view);

        navView.setCheckedItem(R.id.nav_checkout);
        View headerView = navView.getHeaderView(0);
        TextView tvHeaderRole = headerView.findViewById(R.id.tvHeaderRole);

        if (isAdmin) {
            tvHeaderRole.setText(activeCashier + " / Admin");
        } else {
            tvHeaderRole.setText(activeCashier + " / Cashier");
        }

        android.view.Menu navMenu = navView.getMenu();
        if (!isAdmin) {
            navMenu.findItem(R.id.nav_inventory).setVisible(false);
            navMenu.findItem(R.id.nav_gcash).setVisible(false);
            navMenu.findItem(R.id.nav_dashboard).setVisible(false);
        }

        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_checkout) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(MainActivity.this, HistoryActivity.class));
                finish();
            } else if (id == R.id.nav_utang) {
                startActivity(new Intent(MainActivity.this, UtangActivity.class));
                finish();
            } else if (id == R.id.nav_inventory) {
                startActivity(new Intent(MainActivity.this, InventoryActivity.class));
                finish();
            } else if (id == R.id.nav_gcash) {
                startActivity(new Intent(MainActivity.this, GCashActivity.class));
                finish();
            } else if (id == R.id.nav_dashboard) {
                startActivity(new Intent(MainActivity.this, DashboardActivity.class));
            } else if (id == R.id.nav_logout) {
                drawerLayout.closeDrawer(GravityCompat.START);
                performLogout();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

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

                tvMessage.setText("This is the main checkout screen. Scan barcodes, select payment methods, and process customer orders.");

                btnGotIt.setOnClickListener(view -> infoDialog.dismiss());
                infoDialog.show();
            });
        }

        dbHelper = new DatabaseHelper(this);

        listCart = findViewById(R.id.ListCart);
        etBarcode = findViewById(R.id.etBarcode);
        spinnerPurse = findViewById(R.id.spinnerPurse);
        tvTotal = findViewById(R.id.tvTotal);
        btnCheckout = findViewById(R.id.btnCheckout);
        etCashRendered = findViewById(R.id.etCashRendered);
        tvChange = findViewById(R.id.tvChange);

        btnMenu = findViewById(R.id.btnMenu);

        btnMenu.setOnClickListener(v -> {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        AutoCompleteTextView etSmartSearch = findViewById(R.id.etBarcode);
        searchAdapter = new ContainsArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, searchList);
        etSmartSearch.setAdapter(searchAdapter);

        // --- THE INSTANT AUTO-ADD ENGINE ---
        etSmartSearch.setOnItemClickListener((parent, view, position, id) -> {
            String input = (String) parent.getItemAtPosition(position);
            String targetBarcode = input;
            if(input.contains(" | ")){
                targetBarcode = input.split(" \\| ")[1];
            }

            if(globalInventory.containsKey(targetBarcode)){
                Product foundProduct = globalInventory.get(targetBarcode);

                int currentCartQty = 0;
                Product existingCartItem = null;

                for (Product p : checkoutCart) {
                    if (p.getBarcode().equals(foundProduct.getBarcode())) {
                        existingCartItem = p;
                        currentCartQty = p.getCurrentQuantity();
                        break;
                    }
                }

                if (currentCartQty + 1 > foundProduct.getCurrentQuantity()) {
                    Toast.makeText(MainActivity.this, "Max stock reached!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (existingCartItem != null) {
                    existingCartItem.setCurrentQuantity(currentCartQty + 1);
                } else {
                    Product receiptItem = new Product(0, 1, foundProduct.getCostPrice(), foundProduct.getSellingPrice(), foundProduct.getBarcode(), foundProduct.getProductName(), foundProduct.getCategory());
                    checkoutCart.add(receiptItem);
                }

                recalculateGrandTotal();
                cartAdapter.notifyDataSetChanged();

                // Clear the chamber and snap focus back instantly
                etBarcode.setText("");
                etBarcode.requestFocus();
            }
        });

        String[] purses = {"Tindahan", "Softdrinks", "Print"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_spinner, purses);
        spinnerPurse.setAdapter(adapter);

        listCart.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        cartAdapter = new CartAdapter(checkoutCart, new CartAdapter.CartUpdateListener() {
            @Override
            public void onCartUpdated() {
                recalculateGrandTotal();
            }

            @Override
            public int getMaxStock(String barcode) {
                if (globalInventory.containsKey(barcode)) {
                    return globalInventory.get(barcode).getCurrentQuantity();
                }
                return 0;
            }
        });
        listCart.setAdapter(cartAdapter);

        recalculateGrandTotal();
        cartAdapter.notifyDataSetChanged();

        etCashRendered.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                String cashInput = s.toString();
                if(!cashInput.isEmpty()){
                    double cashGiven = Double.parseDouble(cashInput);
                    double change = cashGiven - cartTotal;
                    if(change < 0) change = 0.0;
                    tvChange.setText(String.format("Change: Php %.2f", change));
                } else {
                    tvChange.setText("Change: Php 0.00");
                }
            }
        });

        ImageButton btnScanBarcode = findViewById(R.id.btnScanBarcode);
        btnScanBarcode.setOnClickListener(v -> {
            com.journeyapps.barcodescanner.ScanOptions options = new com.journeyapps.barcodescanner.ScanOptions();
            options.setPrompt("Scan a barcode (Use Volume Up for Flashlight)");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            options.setCaptureActivity(PortraitCaptureActivity.class);
            barcodeLauncher.launch(options);
        });

        btnCheckout.setOnClickListener(v -> {
            if (cartTotal == 0.0) {
                Toast.makeText(MainActivity.this, "Cart is empty!", Toast.LENGTH_SHORT).show();
                return;
            }

            String selectedPurse = spinnerPurse.getSelectedItem().toString();
            android.widget.RadioGroup radioGroupPayment = findViewById(R.id.radioGroupPayment);
            int selectedRadioId = radioGroupPayment.getCheckedRadioButtonId();

            String paymentMethod = "Cash";
            if (selectedRadioId == R.id.radioGCash) {
                paymentMethod = "GCash Transfer";
            } else if (selectedRadioId == R.id.radioUtang) {
                paymentMethod = "Utang";
            }

            double cashGiven = 0.0;
            String cashInput = etCashRendered.getText().toString();
            if (!cashInput.isEmpty()) {
                cashGiven = Double.parseDouble(cashInput);
            }

            double changeAmount = cashGiven - cartTotal;
            if (changeAmount < 0) changeAmount = 0.0;

            if (paymentMethod.equals("Cash") && cashGiven < cartTotal) {
                Toast.makeText(MainActivity.this, "Not enough cash given!", Toast.LENGTH_SHORT).show();
                return;
            }

            String deviceName = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;

            if (paymentMethod.equals("Utang")) {
                com.google.android.material.bottomsheet.BottomSheetDialog utangDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(MainActivity.this);
                View sheetView = getLayoutInflater().inflate(R.layout.layout_checkout_utang_sheet, null);

                android.widget.AutoCompleteTextView etNameInput = sheetView.findViewById(R.id.etUtangNameInput);
                Button btnConfirmUtang = sheetView.findViewById(R.id.btnConfirmUtangCheckout);

                dbHelper.getCloudDebts(debtMap -> {
                    ArrayList<String> existingNames = new ArrayList<>(debtMap.keySet());
                    android.widget.ArrayAdapter<String> adapter2 = new android.widget.ArrayAdapter<>(MainActivity.this, android.R.layout.simple_dropdown_item_1line, existingNames);
                    etNameInput.setAdapter(adapter2);
                });

                btnConfirmUtang.setOnClickListener(v1 -> {
                    String debtorName = etNameInput.getText().toString().trim();
                    if (debtorName.isEmpty()) {
                        Toast.makeText(MainActivity.this, "Name required for Utang!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    dbHelper.checkNeighborDebt(debtorName, currentDebt -> {
                        double projectedDebt = currentDebt + cartTotal;

                        Runnable executeCheckout = () -> {
                            String precisePayment = "Utang (" + debtorName + ")";
                            boolean isSaved = dbHelper.saveTransaction(selectedPurse, precisePayment, cartTotal, 0.0, 0.0, deviceName, checkoutCart);
                            boolean isDebtLogged = dbHelper.logDebt(debtorName, cartTotal);

                            if (isSaved && isDebtLogged) {
                                Toast.makeText(MainActivity.this, "Items charged to " + debtorName + "'s Utang!", Toast.LENGTH_LONG).show();
                                wipeCartClean();
                                utangDialog.dismiss();
                            }
                        };

                        if (projectedDebt >= 1000.0) {
                            android.app.Dialog warningDialog = new android.app.Dialog(MainActivity.this);
                            warningDialog.setContentView(R.layout.layout_dialog_credit_warning);
                            warningDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

                            android.widget.TextView tvMessage = warningDialog.findViewById(R.id.tvWarningMessage);
                            android.widget.Button btnApprove = warningDialog.findViewById(R.id.btnApproveAnyway);
                            android.widget.TextView btnReject = warningDialog.findViewById(R.id.btnRejectUtang);

                            String htmlMessage = String.format("%s's current debt is <b>₱%.2f</b>.<br>Adding this transaction of <b>₱%.2f</b> will exceed their limit of <font color='#DC2626'><b>₱1000.00</b></font>.", debtorName, currentDebt, cartTotal);
                            tvMessage.setText(android.text.Html.fromHtml(htmlMessage, android.text.Html.FROM_HTML_MODE_LEGACY));

                            btnApprove.setOnClickListener(warnView -> {
                                executeCheckout.run();
                                warningDialog.dismiss();
                            });

                            btnReject.setOnClickListener(warnView -> warningDialog.dismiss());

                            warningDialog.show();
                        } else {
                            executeCheckout.run();
                        }
                    });
                });

                utangDialog.setContentView(sheetView);
                utangDialog.show();
            }else {
                boolean isSaved = dbHelper.saveTransaction(selectedPurse, paymentMethod, cartTotal, cashGiven, changeAmount, deviceName, checkoutCart);
                if(isSaved){
                    Toast.makeText(MainActivity.this, "Checkout complete!", Toast.LENGTH_LONG).show();
                    wipeCartClean();
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        idleHandler.removeCallbacks(idleRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        idleHandler.removeCallbacks(idleRunnable);
        idleHandler.postDelayed(idleRunnable, 15 * 60 * 1000);

        dbHelper.getAllProducts(currentInventory -> {
            globalInventory = currentInventory;
            searchList.clear();
            for(Product p : currentInventory.values()){
                searchList.add(p.getProductName() + " | " + p.getBarcode());
            }

            if (searchAdapter != null) {
                searchAdapter.updateData(searchList);
            }
        });
    }

    private void wipeCartClean() {
        checkoutCart.clear();
        cartTotal = 0.0;
        tvTotal.setText("₱0.00");
        etCashRendered.setText("");
        tvChange.setText("Change: ₱0.00");
        cartAdapter.notifyDataSetChanged();
    }

    private void performLogout() {
        wipeCartClean();

        android.content.SharedPreferences prefs = getSharedPreferences("StoreSession", MODE_PRIVATE);
        prefs.edit().clear().apply();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void recalculateGrandTotal() {
        cartTotal = 0.0;
        for (Product p : checkoutCart) {
            cartTotal += (p.getSellingPrice() * p.getCurrentQuantity());
        }
        tvTotal.setText(String.format("₱%.2f", cartTotal));
        etCashRendered.setText(etCashRendered.getText().toString());
    }

    private void showItemNotFoundDialog(String missingBarcode) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_kanto_alert, null);

        ImageView imgIcon = sheetView.findViewById(R.id.imgAlertIcon);
        TextView tvTitle = sheetView.findViewById(R.id.tvAlertTitle);
        TextView tvMessage = sheetView.findViewById(R.id.tvAlertMessage);
        Button btnCancel = sheetView.findViewById(R.id.btnAlertLeft);
        Button btnCheckInventory = sheetView.findViewById(R.id.btnAlertRight);

        imgIcon.setImageResource(R.drawable.ic_warning);
        imgIcon.setColorFilter(android.graphics.Color.parseColor("#DC2626"));

        tvTitle.setText("Item Not Found");
        tvMessage.setText("This barcode is not in your system. Check Inventory or Add New Item?");

        btnCancel.setText("Cancel");
        btnCheckInventory.setText("Add to Inventory");

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnCheckInventory.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(MainActivity.this, InventoryActivity.class);
            intent.putExtra("NEW_BARCODE", missingBarcode);
            startActivity(intent);
            finish();
        });

        dialog.setContentView(sheetView);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        dialog.show();
    }
}