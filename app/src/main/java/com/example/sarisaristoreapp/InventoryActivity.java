package com.example.sarisaristoreapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sarisaristoreapp.models.Product;

import java.util.ArrayList;
import java.util.HashMap;

public class InventoryActivity extends AppCompatActivity {

    EditText etNewBarcode, etNewName, etNewBasePrice, etNewSellingPrice, etNewQty;
    Button btnSaveProduct;
    RecyclerView listInventory;

    DatabaseHelper dbHelper;
    ArrayList<Product> inventoryList = new ArrayList<>();
    InventoryAdapter inventoryAdapter;

    private EditText currentBarcodeTarget;
    private com.google.android.material.bottomsheet.BottomSheetDialog activeAddSheet = null;

    private final androidx.activity.result.ActivityResultLauncher<com.journeyapps.barcodescanner.ScanOptions> inventoryBarcodeLauncher =
            registerForActivityResult(new com.journeyapps.barcodescanner.ScanContract(), result -> {
                if(result.getContents() == null) {
                    Toast.makeText(InventoryActivity.this, "Scan cancelled", Toast.LENGTH_SHORT).show();
                } else {
                    String scannedCode = result.getContents();
                    Product existingProduct = null;
                    for (Product p : inventoryList) {
                        if (p.getBarcode() != null && p.getBarcode().equals(scannedCode)) {
                            existingProduct = p;
                            break;
                        }
                    }

                    if (existingProduct != null) {
                        showProductExistsDialog(existingProduct);
                    }
                    else if (currentBarcodeTarget != null) {
                        currentBarcodeTarget.setText(scannedCode);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_inventory);

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(InventoryActivity.this, MainActivity.class));
                finish();
            }
        });

        getWindow().setStatusBarColor(getResources().getColor(R.color.kanto_primary));
        dbHelper = new DatabaseHelper(this);

        // --- DRAWER ENGINE START ---
        androidx.drawerlayout.widget.DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        com.google.android.material.navigation.NavigationView navView = findViewById(R.id.nav_view);

        navView.setCheckedItem(R.id.nav_inventory);

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
                drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START);
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

        ImageView btnInfo = findViewById(R.id.btnInfo);
        if (btnInfo != null) {
            btnInfo.setOnClickListener(v -> {
                android.app.Dialog infoDialog = new android.app.Dialog(this);
                infoDialog.setContentView(R.layout.layout_info_dialog);
                if (infoDialog.getWindow() != null) infoDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                TextView tvMessage = infoDialog.findViewById(R.id.tvInfoMessage);
                Button btnGotIt = infoDialog.findViewById(R.id.btnInfoGotIt);
                tvMessage.setText("This is your store's active inventory. Add new products, update prices, adjust stock levels, and delete discontinued items to keep your database accurate.");
                btnGotIt.setOnClickListener(view -> infoDialog.dismiss());
                infoDialog.show();
            });
        }

        etNewBarcode = findViewById(R.id.etNewBarcode);
        etNewName = findViewById(R.id.etNewName);
        etNewBasePrice = findViewById(R.id.etNewBasePrice);
        etNewSellingPrice = findViewById(R.id.etNewSellingPrice);
        etNewQty = findViewById(R.id.etNewQty);
        btnSaveProduct = findViewById(R.id.btnSaveProduct);
        listInventory = findViewById(R.id.listInventory);

        listInventory.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        inventoryAdapter = new InventoryAdapter(inventoryList);
        listInventory.setAdapter(inventoryAdapter);
        refreshInventoryList();

        Button btnScanInventory = findViewById(R.id.btnScanInventory);
        btnScanInventory.setOnClickListener(v -> {
            com.journeyapps.barcodescanner.ScanOptions options = new com.journeyapps.barcodescanner.ScanOptions();
            options.setPrompt("Scan item to Add/Update Stock");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            options.setCaptureActivity(PortraitCaptureActivity.class);
            inventoryBarcodeLauncher.launch(options);
        });

        EditText etSearch = findViewById(R.id.etSearchInventory);
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) { applyCurrentFilters(); }
        });

        android.widget.RadioGroup radioGroupCategory = findViewById(R.id.radioGroupCategory);
        radioGroupCategory.setOnCheckedChangeListener((group, checkedId) -> applyCurrentFilters());

        ImageView btnAdd = findViewById(R.id.btnAddNewItemUI);
        btnAdd.setOnClickListener(v -> showAddProductSheet(null));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String incomingBarcode = getIntent().getStringExtra("NEW_BARCODE");
        if (incomingBarcode != null && !incomingBarcode.isEmpty()) {
            showAddProductSheet(incomingBarcode);
        }
    }

    private void showAddProductSheet(String prefillBarcode) {
        activeAddSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_add_product_sheet, null);

        EditText etSheetBarcode = sheetView.findViewById(R.id.etSheetBarcode);
        ImageView btnSheetScan = sheetView.findViewById(R.id.btnSheetScan);
        EditText etSheetName = sheetView.findViewById(R.id.etSheetName);
        EditText etSheetCost = sheetView.findViewById(R.id.etSheetCost);
        EditText etSheetSell = sheetView.findViewById(R.id.etSheetSell);
        EditText etSheetQty = sheetView.findViewById(R.id.etSheetQty);
        Button btnSheetSave = sheetView.findViewById(R.id.btnSheetSave);
        Spinner spinnerCat = sheetView.findViewById(R.id.spinnerCategory);

        String[] categories = {"Tindahan", "Softdrinks", "Print"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_selected, categories);
        catAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerCat.setAdapter(catAdapter);

        if (prefillBarcode != null && !prefillBarcode.isEmpty()) {
            etSheetBarcode.setText(prefillBarcode);
        }

        currentBarcodeTarget = etSheetBarcode;

        btnSheetScan.setOnClickListener(v1 -> {
            com.journeyapps.barcodescanner.ScanOptions options = new com.journeyapps.barcodescanner.ScanOptions();
            options.setPrompt("Scan item barcode");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            options.setCaptureActivity(PortraitCaptureActivity.class);
            inventoryBarcodeLauncher.launch(options);
        });

        btnSheetSave.setOnClickListener(v1 -> {
            String bar = etSheetBarcode.getText().toString().trim();
            String name = etSheetName.getText().toString().trim();
            String costStr = etSheetCost.getText().toString().trim();
            String sellStr = etSheetSell.getText().toString().trim();
            String qtyStr = etSheetQty.getText().toString().trim();

            if(name.isEmpty() || costStr.isEmpty() || sellStr.isEmpty() || qtyStr.isEmpty()){
                Toast.makeText(InventoryActivity.this, "Please fill all required fields!", Toast.LENGTH_SHORT).show();
                return;
            }

            if(bar.isEmpty()) bar = "MANUAL_" + System.currentTimeMillis();

            double cost = Double.parseDouble(costStr);
            double sell = Double.parseDouble(sellStr);
            int qty = Integer.parseInt(qtyStr);
            String selectedCategory = spinnerCat.getSelectedItem().toString();

            Product newProduct = new Product(0, qty, cost, sell, bar, name, selectedCategory);

            if(dbHelper.addProduct(newProduct)){
                Toast.makeText(InventoryActivity.this, "Product Saved!", Toast.LENGTH_SHORT).show();
                refreshInventoryList();
                activeAddSheet.dismiss();
            } else {
                Toast.makeText(InventoryActivity.this, "Error: Barcode already exists!", Toast.LENGTH_SHORT).show();
            }
        });

        activeAddSheet.setContentView(sheetView);
        activeAddSheet.show();

        com.google.android.material.bottomsheet.BottomSheetBehavior behavior = activeAddSheet.getBehavior();
        behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
    }

    public void openEditProductPanel(Product currentItem) {
        com.google.android.material.bottomsheet.BottomSheetDialog editDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);

        if (activeAddSheet != null && activeAddSheet.getWindow() != null) {
            activeAddSheet.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        editDialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_edit_product_sheet, null);

        EditText etBarcode = sheetView.findViewById(R.id.etEditBarcode);
        EditText etName = sheetView.findViewById(R.id.etEditName);
        EditText etCost = sheetView.findViewById(R.id.etEditCost);
        EditText etSell = sheetView.findViewById(R.id.etEditSell);
        EditText etQty = sheetView.findViewById(R.id.etEditQty);
        ImageView btnScan = sheetView.findViewById(R.id.btnEditScan);
        Button btnDelete = sheetView.findViewById(R.id.btnEditDelete);
        Button btnSave = sheetView.findViewById(R.id.btnEditSave);
        Spinner spinnerCat = sheetView.findViewById(R.id.spinnerCategory);

        String[] categories = {"Tindahan", "Softdrinks", "Print"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_selected, categories);
        catAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerCat.setAdapter(catAdapter);

        etBarcode.setText(currentItem.getBarcode());
        etName.setText(currentItem.getProductName());
        etCost.setText(String.valueOf(currentItem.getCostPrice()));
        etSell.setText(String.valueOf(currentItem.getSellingPrice()));
        etQty.setText(String.valueOf(currentItem.getCurrentQuantity()));

        if (currentItem.getCategory() != null) {
            int spinnerPosition = catAdapter.getPosition(currentItem.getCategory());
            if (spinnerPosition >= 0) {
                spinnerCat.setSelection(spinnerPosition);
            }
        }

        btnScan.setOnClickListener(v1 -> triggerEditScanner(etBarcode));

        btnDelete.setOnClickListener(v1 -> {
            android.app.Dialog customDialog = new android.app.Dialog(this);
            customDialog.setContentView(R.layout.layout_alert_dialog);
            if (customDialog.getWindow() != null) customDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

            TextView tvTitle = customDialog.findViewById(R.id.tvAlertTitle);
            TextView tvMessage = customDialog.findViewById(R.id.tvAlertMessage);
            Button btnCancel = customDialog.findViewById(R.id.btnAlertCancel);
            Button btnConfirm = customDialog.findViewById(R.id.btnAlertConfirm);

            tvTitle.setText("Delete Item?");
            tvMessage.setText("Are you sure you want to permanently delete " + currentItem.getProductName() + "? This cannot be undone.");
            btnConfirm.setText("Delete");

            btnCancel.setOnClickListener(view -> customDialog.dismiss());
            btnConfirm.setOnClickListener(view -> {
                dbHelper.deleteProduct(currentItem.getBarcode());
                Toast.makeText(this, "Item Deleted!", Toast.LENGTH_SHORT).show();
                refreshInventoryList();
                customDialog.dismiss();
                editDialog.dismiss();
            });
            customDialog.show();
        });

        btnSave.setOnClickListener(v1 -> {
            String newName = etName.getText().toString();
            String costText = etCost.getText().toString();
            String sellText = etSell.getText().toString();
            String qtyText = etQty.getText().toString();
            String newBarcode = etBarcode.getText().toString();

            if(newName.isEmpty() || costText.isEmpty() || sellText.isEmpty() || qtyText.isEmpty()){
                Toast.makeText(this, "Please fill all fields!", Toast.LENGTH_SHORT).show();
                return;
            }

            currentItem.setProductName(newName);
            currentItem.setCostPrice(Double.parseDouble(costText));
            currentItem.setSellingPrice(Double.parseDouble(sellText));
            currentItem.setCurrentQuantity(Integer.parseInt(qtyText));
            currentItem.setBarcode(newBarcode);
            currentItem.setCategory(spinnerCat.getSelectedItem().toString());

            dbHelper.addProduct(currentItem);
            Toast.makeText(this, "Item Updated!", Toast.LENGTH_SHORT).show();
            refreshInventoryList();
            editDialog.dismiss();
        });

        editDialog.setContentView(sheetView);
        editDialog.show();

        etQty.requestFocus();
        etQty.setSelection(etQty.getText().length());

        com.google.android.material.bottomsheet.BottomSheetBehavior behavior = editDialog.getBehavior();
        behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
    }

    private void showProductExistsDialog(Product existingProduct) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_kanto_alert, null);

        ImageView imgIcon = sheetView.findViewById(R.id.imgAlertIcon);
        TextView tvTitle = sheetView.findViewById(R.id.tvAlertTitle);
        TextView tvMessage = sheetView.findViewById(R.id.tvAlertMessage);
        Button btnCancel = sheetView.findViewById(R.id.btnAlertLeft);
        Button btnUpdate = sheetView.findViewById(R.id.btnAlertRight);

        imgIcon.setImageResource(R.drawable.ic_info);
        imgIcon.setColorFilter(android.graphics.Color.parseColor("#2E7D32"));

        tvTitle.setText("Product Exists");
        tvMessage.setText("This item (" + existingProduct.getProductName() + ") is already in your inventory. Do you want to pull its existing information to update it?");

        btnCancel.setText("Cancel");
        btnUpdate.setText("Update Item");

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnUpdate.setOnClickListener(v -> {
            dialog.dismiss();
            if (activeAddSheet != null && activeAddSheet.isShowing()) {
                activeAddSheet.dismiss();
            }
            openEditProductPanel(existingProduct);
        });

        dialog.setContentView(sheetView);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.show();
    }

    private void refreshInventoryList(){
        dbHelper.getAllProducts(new DatabaseHelper.InventoryStatus() {
            @Override
            public void onDataLoaded(HashMap<String, Product> inventoryMap) {
                inventoryList.clear();
                for(java.util.Map.Entry<String, Product> entry : inventoryMap.entrySet()){
                    Product p = entry.getValue();
                    String nodeKey = entry.getKey();
                    if (p.getBarcode() == null || p.getBarcode().trim().isEmpty() || p.getBarcode().equals("null")) {
                        p.setBarcode(nodeKey);
                    }
                    inventoryList.add(p);
                }
                applyCurrentFilters();

                // --- THE UPDATE TELEPORTER CATCHER ---
                String updateBarcode = getIntent().getStringExtra("UPDATE_BARCODE");
                if (updateBarcode != null && !updateBarcode.isEmpty()) {
                    for (Product p : inventoryList) {
                        if (updateBarcode.equals(p.getBarcode())) {
                            openEditProductPanel(p);
                            // Disarm the teleporter instantly
                            getIntent().removeExtra("UPDATE_BARCODE");
                            break;
                        }
                    }
                }
                // --- END CATCHER ---
            }
        });
    }

    public void triggerEditScanner(EditText targetBox) {
        currentBarcodeTarget = targetBox;
        com.journeyapps.barcodescanner.ScanOptions options = new com.journeyapps.barcodescanner.ScanOptions();
        options.setPrompt("Scan item barcode");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(PortraitCaptureActivity.class);
        inventoryBarcodeLauncher.launch(options);
    }

    private void applyCurrentFilters() {
        android.widget.EditText etSearch = findViewById(R.id.etSearchInventory);
        String query = etSearch.getText().toString().toLowerCase().trim();

        android.widget.RadioGroup radioGroupCategory = findViewById(R.id.radioGroupCategory);
        int checkedId = radioGroupCategory.getCheckedRadioButtonId();
        String selectedCategory = "All";
        if (checkedId == R.id.radioTindahan) selectedCategory = "Tindahan";
        else if (checkedId == R.id.radioSoftdrinks) selectedCategory = "Softdrinks";
        else if (checkedId == R.id.radioPrint) selectedCategory = "Print";

        ArrayList<Product> filteredList = new ArrayList<>();
        String[] keywords = query.split("\\s+");

        for (Product item : inventoryList) {
            boolean matchesCategory = selectedCategory.equals("All") ||
                    (item.getCategory() != null && item.getCategory().equals(selectedCategory));

            String safeName = item.getProductName() != null ? item.getProductName().toLowerCase() : "";
            String safeBar = item.getBarcode() != null ? item.getBarcode().toLowerCase() : "";

            boolean matchesSearch = true;
            if (!query.isEmpty()) {
                for (String word : keywords) {
                    if (!safeName.contains(word) && !safeBar.contains(word)) {
                        matchesSearch = false;
                        break;
                    }
                }
            }

            if (matchesCategory && matchesSearch) {
                filteredList.add(item);
            }
        }
        inventoryAdapter.filterList(filteredList);
    }
}