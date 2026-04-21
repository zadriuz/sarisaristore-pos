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
import androidx.recyclerview.widget.RecyclerView;

import com.example.sarisaristoreapp.models.Product;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private long backPressedTime;

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
    ArrayAdapter<String> searchAdapter;

    Button btnAddItem, btnCheckout;
    EditText etBarcode, etQty, etCashRendered;
    Spinner spinnerPurse;
    TextView tvTotal, tvChange;
    ImageView btnMenu; // Declaration ONLY - fixes the crash
    Button btnMenuCheckout;


    DatabaseHelper dbHelper;
    public static ArrayList<Product> checkoutCart = new ArrayList<>();
    public static double cartTotal = 0.0;
    RecyclerView listCart;
    CartAdapter cartAdapter;
    String scannedCode;

    String activeCashier;
    boolean isAdmin;

    // THE CATCHER'S MITT
    private final androidx.activity.result.ActivityResultLauncher<com.journeyapps.barcodescanner.ScanOptions> barcodeLauncher =
            registerForActivityResult(new com.journeyapps.barcodescanner.ScanContract(), result -> {
                if(result.getContents() == null) {
                    Toast.makeText(MainActivity.this, "Scan cancelled", Toast.LENGTH_SHORT).show();
                } else {
                    scannedCode = result.getContents();
                    if(globalInventory.containsKey(scannedCode)){
                        Product foundProduct = globalInventory.get(scannedCode);

                        // --- THE CUMULATIVE SCANNER CHECK ---
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
                        Toast.makeText(MainActivity.this, "Item not found in inventory!", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // EdgeToEdge removed to fix UI overlap bugs
        setContentView(R.layout.activity_main);

        // THE DOUBLE-TAP BACK BUTTON TRAPDOOR
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    // SECOND TAP: Summon the custom dialog!
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
                        performLogout(); // Triggers your actual logout sequence!
                    });

                    customDialog.show();
                } else {
                    // FIRST TAP: Show the warning toast
                    Toast.makeText(getBaseContext(), "Press back again to log out", Toast.LENGTH_SHORT).show();
                }
                backPressedTime = System.currentTimeMillis();
            }
        });

        SharedPreferences prefs = getSharedPreferences("StoreSession", MODE_PRIVATE);
        activeCashier = prefs.getString("ActiveUser", "Unknown");
        isAdmin = prefs.getBoolean("IsAdmin", false);

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
                tvMessage.setText("This is the main checkout screen. Scan barcodes, adjust quantities, select payment methods (Cash, GCash, or Utang), and process customer orders.");
                // ------------------------------------------

                btnGotIt.setOnClickListener(view -> infoDialog.dismiss());

                infoDialog.show();
            });
        }

        dbHelper = new DatabaseHelper(this);

        btnAddItem = findViewById(R.id.btnAddItem);
        listCart = findViewById(R.id.ListCart);
        etBarcode = findViewById(R.id.etBarcode);
        etQty = findViewById(R.id.etQty);
        spinnerPurse = findViewById(R.id.spinnerPurse);
        tvTotal = findViewById(R.id.tvTotal);
        btnCheckout = findViewById(R.id.btnCheckout);
        etCashRendered = findViewById(R.id.etCashRendered);
        tvChange = findViewById(R.id.tvChange);

        btnMenu = findViewById(R.id.btnMenu); // Properly initialized after setContentView

        AutoCompleteTextView etSmartSearch = findViewById(R.id.etBarcode);
        searchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, searchList);
        etSmartSearch.setAdapter(searchAdapter);

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
                // Return the real-time stock directly from the Master Inventory
                if (globalInventory.containsKey(barcode)) {
                    return globalInventory.get(barcode).getCurrentQuantity();
                }
                return 0; // Failsafe if the item somehow doesn't exist
            }
        });
        listCart.setAdapter(cartAdapter);

        // WAKE UP THE STATIC CART UI
        recalculateGrandTotal();
        cartAdapter.notifyDataSetChanged();

        // WAKE UP THE SECURE MENU
        btnMenu.setOnClickListener(v -> showAdminMenu());
        // --- END BOTTOM SHEET WIRING ---

        // CHANGE CALCULATOR
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

        btnAddItem.setOnClickListener(v -> {
            String input = etBarcode.getText().toString();
            String scannedQty = etQty.getText().toString();

            if(input.isEmpty() || scannedQty.isEmpty()){
                Toast.makeText(MainActivity.this, "Please enter item and quantity!", Toast.LENGTH_SHORT).show();
                return;
            }

            String targetBarcode = input;
            if(input.contains(" | ")){
                targetBarcode = input.split(" \\| ")[1];
            }

            if(globalInventory.containsKey(targetBarcode)){
                Product foundProduct = globalInventory.get(targetBarcode);
                int parsedQty = Integer.parseInt(scannedQty);

                // --- THE ZERO/NEGATIVE BOUNCER ---
                if (parsedQty <= 0) {
                    Toast.makeText(MainActivity.this, "Quantity must be at least 1!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // --- THE CUMULATIVE STOCK CHECKER ---
                int currentCartQty = 0;
                Product existingCartItem = null;

                // 1. Scan the cart to see if this item is already in there
                for (Product p : checkoutCart) {
                    if (p.getBarcode().equals(foundProduct.getBarcode())) {
                        existingCartItem = p;
                        currentCartQty = p.getCurrentQuantity();
                        break;
                    }
                }

                // 2. Add the cart quantity + the new quantity, and check the absolute limit
                if (currentCartQty + parsedQty > foundProduct.getCurrentQuantity()) {
                    int remainingAllowed = foundProduct.getCurrentQuantity() - currentCartQty;
                    Toast.makeText(MainActivity.this, "Not enough stock! You can only add " + remainingAllowed + " more.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 3. Group the items or add a new row!
                if (existingCartItem != null) {
                    existingCartItem.setCurrentQuantity(currentCartQty + parsedQty);
                } else {
                    Product receiptItem = new Product(0, parsedQty, foundProduct.getCostPrice(), foundProduct.getSellingPrice(), foundProduct.getBarcode(), foundProduct.getProductName(), foundProduct.getCategory());
                    checkoutCart.add(receiptItem);
                }

                recalculateGrandTotal();
                cartAdapter.notifyDataSetChanged();

                etBarcode.setText("");
                etQty.setText("");
            } else {
                Toast.makeText(MainActivity.this, "Item not found in inventory!", Toast.LENGTH_SHORT).show();
            }
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
                // 1. Summon the new Custom Bottom Sheet!
                com.google.android.material.bottomsheet.BottomSheetDialog utangDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(MainActivity.this);
                View sheetView = getLayoutInflater().inflate(R.layout.layout_checkout_utang_sheet, null);

                android.widget.AutoCompleteTextView etNameInput = sheetView.findViewById(R.id.etUtangNameInput);
                Button btnConfirmUtang = sheetView.findViewById(R.id.btnConfirmUtangCheckout);

                // 2. Feed the Smart Search with existing debtors!
                dbHelper.getCloudDebts(debtMap -> {
                    ArrayList<String> existingNames = new ArrayList<>(debtMap.keySet());
                    android.widget.ArrayAdapter<String> adapter2 = new android.widget.ArrayAdapter<>(MainActivity.this, android.R.layout.simple_dropdown_item_1line, existingNames);
                    etNameInput.setAdapter(adapter2);
                });

                // 3. The Confirm Button (with the Bouncer completely intact!)
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
                                utangDialog.dismiss(); // Close the sheet!
                            }
                        };

                        // 3. THE BOUNCER LOGIC
                        if (projectedDebt >= 1000.0) {
                            // Summon the Custom Warning Dialog!
                            android.app.Dialog warningDialog = new android.app.Dialog(MainActivity.this);
                            warningDialog.setContentView(R.layout.layout_dialog_credit_warning);
                            warningDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

                            android.widget.TextView tvMessage = warningDialog.findViewById(R.id.tvWarningMessage);
                            android.widget.Button btnApprove = warningDialog.findViewById(R.id.btnApproveAnyway);
                            android.widget.TextView btnReject = warningDialog.findViewById(R.id.btnRejectUtang);

                            // The HTML String Parser (Matches Stitch's exact coloring!)
                            String htmlMessage = String.format("%s's current debt is <b>₱%.2f</b>.<br>Adding this transaction of <b>₱%.2f</b> will exceed their limit of <font color='#DC2626'><b>₱1000.00</b></font>.", debtorName, currentDebt, cartTotal);
                            tvMessage.setText(android.text.Html.fromHtml(htmlMessage, android.text.Html.FROM_HTML_MODE_LEGACY));

                            btnApprove.setOnClickListener(warnView -> {
                                executeCheckout.run();
                                warningDialog.dismiss();
                            });

                            btnReject.setOnClickListener(warnView -> warningDialog.dismiss());

                            warningDialog.show();
                        } else {
                            // Safe! Under ₱1000. Process it normally.
                            executeCheckout.run();
                        }
                    });
                });

                utangDialog.setContentView(sheetView);
                utangDialog.show();
            }else {
                // THE MISSING LOGIC: Process normal Cash and GCash payments!
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
        // KILL THE TIMER WHEN APP IS IN BACKGROUND
        idleHandler.removeCallbacks(idleRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // RESTART THE TIMER WHEN APP IS OPENED
        idleHandler.removeCallbacks(idleRunnable);
        idleHandler.postDelayed(idleRunnable, 15 * 60 * 1000);

        dbHelper.getAllProducts(currentInventory -> {
            globalInventory = currentInventory;
            searchList.clear();
            for(Product p : currentInventory.values()){
                searchList.add(p.getProductName() + " | " + p.getBarcode());
            }
            searchAdapter.notifyDataSetChanged();
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