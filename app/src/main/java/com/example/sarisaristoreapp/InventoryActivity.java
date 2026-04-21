package com.example.sarisaristoreapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
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

    EditText etNewBarcode;
    EditText etNewName;
    EditText etNewBasePrice;
    EditText etNewSellingPrice;
    EditText etNewQty;
    Button btnSaveProduct;
    RecyclerView listInventory;

    DatabaseHelper dbHelper;

    ArrayList<Product> inventoryList = new ArrayList<>();
    InventoryAdapter inventoryAdapter;
    private EditText currentBarcodeTarget;

    // THE CATCHER'S MITT for Inventory
    private final androidx.activity.result.ActivityResultLauncher<com.journeyapps.barcodescanner.ScanOptions> inventoryBarcodeLauncher =
            registerForActivityResult(new com.journeyapps.barcodescanner.ScanContract(), result -> {
                if(result.getContents() == null) {
                    Toast.makeText(InventoryActivity.this, "Scan cancelled", Toast.LENGTH_SHORT).show();
                } else {
                    if (currentBarcodeTarget != null) {
                        currentBarcodeTarget.setText(result.getContents());
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_inventory);

        // TELEPORT BACK TO POS CHECKOUT
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(InventoryActivity.this, MainActivity.class));
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
                tvMessage.setText("This is your store's active inventory. Add new products, update prices, adjust stock levels, and delete discontinued items to keep your database accurate.");
                // ------------------------------------------

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
        btnScanInventory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                com.journeyapps.barcodescanner.ScanOptions options = new com.journeyapps.barcodescanner.ScanOptions();
                options.setPrompt("Scan item to Add/Update Stock");
                options.setBeepEnabled(true);
                options.setOrientationLocked(true);
                options.setCaptureActivity(PortraitCaptureActivity.class); // Using the custom portrait mode we built!

                inventoryBarcodeLauncher.launch(options);
            }
        });

        // --- 1. THE SEARCH ENGINE ---
        // --- 1. THE SEARCH ENGINE ---
        EditText etSearch = findViewById(R.id.etSearchInventory);
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                // THE FIX: Trigger the master engine!
                applyCurrentFilters();
            }
        });

        // --- 2. THE MENU WIRING ---
        ImageView btnMenu = findViewById(R.id.btnMenu);
        // WAKE UP THE SECURE MENU
        btnMenu.setOnClickListener(v -> showAdminMenu());

        // --- 4. THE CATEGORY FILTER ENGINE ---
        // --- 4. THE CATEGORY FILTER ENGINE ---
        android.widget.RadioGroup radioGroupCategory = findViewById(R.id.radioGroupCategory);
        radioGroupCategory.setOnCheckedChangeListener((group, checkedId) -> {
            // THE FIX: Trigger the master engine!
            applyCurrentFilters();
        });

        // --- 3. THE ADD ITEM BUTTON ---
        // --- 3. THE ADD ITEM BUTTON ---
        ImageView btnAdd = findViewById(R.id.btnAddNewItemUI);
        btnAdd.setOnClickListener(v -> {
            com.google.android.material.bottomsheet.BottomSheetDialog addDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(InventoryActivity.this);
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

            // 1. Use your custom SLIM layout for the closed box
            android.widget.ArrayAdapter<String> catAdapter = new android.widget.ArrayAdapter<>(this, R.layout.item_spinner_selected, categories);

            // 2. Use your custom FAT layout for the dropdown list
            catAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);

            spinnerCat.setAdapter(catAdapter);

            // Tell the scanner where to put the barcode when it returns!
            currentBarcodeTarget = etSheetBarcode;

            // Wire the specific scanner button inside the bottom sheet
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

                // Grab the category from the spinner
                String selectedCategory = spinnerCat.getSelectedItem().toString();

// Pass it as the 7th argument!
                Product newProduct = new Product(0, qty, cost, sell, bar, name, selectedCategory);

                if(dbHelper.addProduct(newProduct)){
                    Toast.makeText(InventoryActivity.this, "Product Saved!", Toast.LENGTH_SHORT).show();
                    refreshInventoryList();
                    addDialog.dismiss();
                } else {
                    Toast.makeText(InventoryActivity.this, "Error: Barcode already exists!", Toast.LENGTH_SHORT).show();
                }
            });

            addDialog.setContentView(sheetView);
            addDialog.show();
        });

        btnMenu.setOnClickListener(v -> showAdminMenu());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void refreshInventoryList(){
        dbHelper.getAllProducts(new DatabaseHelper.InventoryStatus() {
            @Override
            public void onDataLoaded(HashMap<String, Product> inventoryMap) {
                inventoryList.clear();

                for(Product p : inventoryMap.values()){
                    // THE GHOST FILTER: Only load items that have actual barcodes!
                    if (p.getBarcode() != null && !p.getBarcode().trim().isEmpty() && !p.getBarcode().equals("null")) {
                        inventoryList.add(p);
                    }
                }

                // THE FIX: Trigger the master engine instead of manually updating the adapter!
                applyCurrentFilters();
            }
        });
    }

    // This lets the Adapter borrow our scanner!
    public void triggerEditScanner(EditText targetBox) {
        currentBarcodeTarget = targetBox;
        com.journeyapps.barcodescanner.ScanOptions options = new com.journeyapps.barcodescanner.ScanOptions();
        options.setPrompt("Scan item barcode");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(PortraitCaptureActivity.class);
        inventoryBarcodeLauncher.launch(options);
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

    // --- THE MASTER FILTER ENGINE ---
    private void applyCurrentFilters() {
        // 1. Grab current search text
        android.widget.EditText etSearch = findViewById(R.id.etSearchInventory);
        String query = etSearch.getText().toString().toLowerCase().trim();

        // 2. Grab current selected category
        android.widget.RadioGroup radioGroupCategory = findViewById(R.id.radioGroupCategory);
        int checkedId = radioGroupCategory.getCheckedRadioButtonId();
        String selectedCategory = "All";
        if (checkedId == R.id.radioTindahan) selectedCategory = "Tindahan";
        else if (checkedId == R.id.radioSoftdrinks) selectedCategory = "Softdrinks";
        else if (checkedId == R.id.radioPrint) selectedCategory = "Print";

        // 3. Filter the master list!
        ArrayList<Product> filteredList = new ArrayList<>();
        for (Product item : inventoryList) {
            // Check Category
            boolean matchesCategory = selectedCategory.equals("All") ||
                    (item.getCategory() != null && item.getCategory().equals(selectedCategory));

            // Check Search
            String safeName = item.getProductName() != null ? item.getProductName().toLowerCase() : "";
            String safeBar = item.getBarcode() != null ? item.getBarcode().toLowerCase() : "";
            boolean matchesSearch = query.isEmpty() || safeName.contains(query) || safeBar.contains(query);

            // If it passes BOTH tests, show it!
            if (matchesCategory && matchesSearch) {
                filteredList.add(item);
            }
        }

        inventoryAdapter.filterList(filteredList);
    }
}