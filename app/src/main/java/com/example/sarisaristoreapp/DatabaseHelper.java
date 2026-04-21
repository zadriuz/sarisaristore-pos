package com.example.sarisaristoreapp;

import android.content.Context;

import com.example.sarisaristoreapp.models.Product;
import com.example.sarisaristoreapp.models.TransactionRecord;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;

public class DatabaseHelper {

    // 1. The Firebase Cloud Reference
    private DatabaseReference mDatabase;

    public DatabaseHelper(Context context) {
        // 2. Initialize the connection to your specific database URL
        mDatabase = FirebaseDatabase.getInstance("https://sarisaristoreapp-99b12-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();
    }

    // 3. The Cloud Upsert Method
    public boolean addProduct(Product product){
        // Tell Firebase to go to the "Inventory" folder, create a folder for the barcode, and drop the product object inside.
        mDatabase.child("Inventory").child(product.getBarcode()).setValue(product);

        // Firebase is asynchronous, so we just return true immediately to keep the UI moving fast.
        return true;
    }

    // --- DASHBOARD WALKIE-TALKIE ---
    public interface DashboardStatus {
        // We added totalProfit to the radio call!
        void onDashboardLoaded(double totalCash, double totalProfit, java.util.LinkedHashMap<String, Float> dailySales);
    }

    public void getDashboardData(final DashboardStatus status) {
        mDatabase.child("Transactions").addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                double totalCash = 0.0;
                double totalProfit = 0.0;
                java.util.LinkedHashMap<String, Float> dailySales = new java.util.LinkedHashMap<>();

                for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                    TransactionRecord record = child.getValue(TransactionRecord.class);
                    if (record != null) {

                        // 1. Calculate Expected Drawer Cash & Profit (Only count 'Tindahan')
                        if ("Tindahan".equals(record.getPurseType())) {
                            totalCash += record.getTotalAmount();

                            // Grab the profit we saved earlier (if it exists)
                            Double profitObj = child.child("totalProfit").getValue(Double.class);
                            if (profitObj != null) {
                                totalProfit += profitObj;
                            }
                        }

                        // 2. Calculate Daily Graph Data
                        String fullDate = record.getTransactionDate();
                        if (fullDate != null && fullDate.length() >= 10) {
                            String justDate = fullDate.substring(0, 10);
                            float currentDayTotal = dailySales.getOrDefault(justDate, 0f);
                            dailySales.put(justDate, currentDayTotal + (float) record.getTotalAmount());
                        }
                    }
                }
                // Send BOTH numbers back to the Activity
                status.onDashboardLoaded(totalCash, totalProfit, dailySales);
            }

            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {}
        });
    }

    // --- HISTORY WALKIE-TALKIES ---
    public interface HistoryStatus {
        void onHistoryLoaded(ArrayList<TransactionRecord> historyList);
    }

    public interface ReceiptStatus {
        void onReceiptLoaded(ArrayList<String> itemList);
    }

    // 1. Fetching all past receipts
    public void getAllTransactions(final HistoryStatus status){
        mDatabase.child("Transactions").addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                ArrayList<TransactionRecord> historyList = new ArrayList<>();
                for (com.google.firebase.database.DataSnapshot childNode : snapshot.getChildren()) {
                    TransactionRecord record = childNode.getValue(TransactionRecord.class);
                    if (record != null) {
                        record.setTransactionId(childNode.getKey()); // Force the push key into the object
                        historyList.add(0, record); // Add to index 0 so newest is at the top!
                    }
                }
                status.onHistoryLoaded(historyList);
            }

            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {}
        });
    }

    // 2. Fetching specific items for a single receipt
    public void getTransactionItems(String transactionId, final ReceiptStatus status){
        // THE CRASH SHIELD: If the ID is null, abort immediately!
        if (transactionId == null) {
            status.onReceiptLoaded(new ArrayList<>());
            return;
        }

        mDatabase.child("TransactionItems").child(transactionId).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                ArrayList<String> itemList = new ArrayList<>();
                for (com.google.firebase.database.DataSnapshot childNode : snapshot.getChildren()) {
                    Product p = childNode.getValue(Product.class);
                    if(p != null){
                        // THE REAL MATH: Multiply price by quantity!
                        double subTotal = p.getSellingPrice() * p.getCurrentQuantity();

                        if ("UTANG_PAYMENT".equals(p.getBarcode())) {
                            // Print exactly what we built in UtangActivity! No extra "Total" attached.
                            itemList.add(p.getProductName());
                        } else if ("GCASH".equals(p.getBarcode())) {
                            String itemLine = p.getProductName() + " | Total: ₱" + String.format("%.2f", p.getSellingPrice());
                            itemList.add(itemLine);
                        } else {
                            String itemLine = p.getProductName() + " | Qty: " + p.getCurrentQuantity() + " | Total: ₱" + String.format("%.2f", subTotal);
                            itemList.add(itemLine);
                        }
                    }
                }
                status.onReceiptLoaded(itemList);
            }
            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {}
        });
    }

    public boolean logDebt(String debtorName, double amount) {
        // Dig into Debts -> Tita Baby -> current_balance, and tell the server to add the math.
        mDatabase.child("Debts")
                .child(debtorName)
                .child("current_balance")
                .setValue(com.google.firebase.database.ServerValue.increment(amount));

        return true;
    }

    public ArrayList<String> getAllDebts() {
        // TODO: Rewrite for Firebase
        return new ArrayList<>();
    }

    // 1. The Walkie-Talkie Interface
    public interface InventoryStatus {
        void onDataLoaded(HashMap<String, Product> inventoryMap);
    }

    // THE ERASER: Permanently deletes an item from the cloud
    public void deleteProduct(String barcode) {
        mDatabase.child("Inventory").child(barcode).removeValue();
    }

    // 2. The Asynchronous Cloud Listener
    public void getAllProducts(final InventoryStatus status) {
        // Point to the Inventory folder and leave a listener there
        mDatabase.child("Inventory").addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                HashMap<String, Product> inventoryMap = new HashMap<>();

                // Loop through every barcode folder inside "Inventory"
                for (com.google.firebase.database.DataSnapshot childNode : snapshot.getChildren()) {
                    // Magically convert the JSON back into a Java Product object
                    Product p = childNode.getValue(Product.class);
                    inventoryMap.put(childNode.getKey(), p);
                }

                // Use the walkie-talkie to send the data back to the Activity
                status.onDataLoaded(inventoryMap);
            }

            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {
                // If the internet completely fails or security rules block us
                System.out.println("Firebase Error: " + error.getMessage());
            }
        });
    }

    // --- UTANG WALKIE-TALKIE ---
    public interface UtangStatus {
        void onUtangLoaded(java.util.LinkedHashMap<String, Double> debtMap);
    }

    public void getCloudDebts(final UtangStatus status) {
        mDatabase.child("Debts").addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                java.util.LinkedHashMap<String, Double> debtMap = new java.util.LinkedHashMap<>();
                for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {

                    // THE FIX: Dig one level deeper to grab the actual number inside "current_balance"
                    Double balance = child.child("current_balance").getValue(Double.class);

                    // Only show people who actually owe money
                    if (balance != null && balance > 0) {
                        debtMap.put(child.getKey(), balance);
                    }
                }
                status.onUtangLoaded(debtMap);
            }

            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {}
        });
    }

    public boolean saveTransaction(String purseType, String paymentMethod, double totalAmount, double cashGiven, double changeAmount, String deviceInfo, ArrayList<Product> soldItems){
        String receiptId = mDatabase.child("Transactions").push().getKey();
        if (receiptId == null) return false;

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
        String currentDate = sdf.format(new java.util.Date());

        HashMap<String, Object> receiptData = new HashMap<>();
        receiptData.put("purseType", purseType);
        receiptData.put("transactionDate", currentDate);
        receiptData.put("totalAmount", totalAmount);
        receiptData.put("paymentMethod", paymentMethod);
        receiptData.put("cashGiven", cashGiven);
        receiptData.put("changeAmount", changeAmount);
        receiptData.put("deviceInfo", deviceInfo);

        // --- THE PROFIT ENGINE ---
        double totalProfit = 0.0;
        for(Product p : soldItems){
            // Profit = (Selling Price - Supplier Cost) * Quantity Bought
            double itemProfit = (p.getSellingPrice() - p.getCostPrice()) * p.getCurrentQuantity();
            totalProfit += itemProfit;
        }
        receiptData.put("totalProfit", totalProfit);

        mDatabase.child("Transactions").child(receiptId).setValue(receiptData);

        for(Product currentItem : soldItems){
            // 1. Save it to the receipt
            mDatabase.child("TransactionItems").child(receiptId).push().setValue(currentItem);

            // 2. THE DEDUCTION ENGINE: Only deduct if it's a REAL physical item!
            String barcode = currentItem.getBarcode();
            if (barcode != null && !barcode.equals("GCASH") && !barcode.equals("UTANG_PAYMENT")) {
                mDatabase.child("Inventory")
                        .child(barcode)
                        .child("currentQuantity")
                        .setValue(com.google.firebase.database.ServerValue.increment(-currentItem.getCurrentQuantity()));
            }
        }
        return true;
    }

    // --- NEIGHBOR HISTORY WALKIE-TALKIE ---
    public interface NeighborHistoryStatus {
        void onHistoryLoaded(ArrayList<String> historyLines);
    }

    public void getNeighborHistory(final String neighborName, final NeighborHistoryStatus status) {
        mDatabase.child("Transactions").addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot txSnapshot) {

                mDatabase.child("TransactionItems").addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(com.google.firebase.database.DataSnapshot itemsSnapshot) {
                        ArrayList<String> historyLines = new ArrayList<>();
                        String borrowTag = "Utang (" + neighborName + ")";
                        String payTag = "Utang Payment (" + neighborName + ")";

                        for (com.google.firebase.database.DataSnapshot txChild : txSnapshot.getChildren()) {
                            TransactionRecord record = txChild.getValue(TransactionRecord.class);
                            if (record != null) {

                                // THE FIX: Check both the Purse AND the Payment Method slots!
                                boolean isBorrow = borrowTag.equals(record.getPaymentMethod()) || borrowTag.equals(record.getPurseType());
                                boolean isPay = payTag.equals(record.getPurseType()) || payTag.equals(record.getPaymentMethod());

                                if (isBorrow) {
                                    String txId = txChild.getKey();
                                    StringBuilder itemsList = new StringBuilder();

                                    if (itemsSnapshot.hasChild(txId)) {
                                        for (com.google.firebase.database.DataSnapshot itemNode : itemsSnapshot.child(txId).getChildren()) {
                                            Product p = itemNode.getValue(Product.class);
                                            if (p != null) {
                                                int qty = p.getCurrentQuantity();
                                                double price = p.getSellingPrice();
                                                double subTotal = qty * price;

                                                itemsList.append("   - ").append(qty).append("x ").append(p.getProductName())
                                                        .append(" (@ Php ").append(String.format("%.2f", price))
                                                        .append(") = Php ").append(String.format("%.2f", subTotal)).append("\n");
                                            }
                                        }
                                    }

                                    if (itemsList.length() == 0) itemsList.append("   - (Just Money / No Items Recorded)\n");

                                    historyLines.add("🔴 BORROWED: Php " + String.format("%.2f", record.getTotalAmount()) +
                                            "\n     Date: " + record.getTransactionDate() +
                                            "\n     Items:\n" + itemsList.toString());

                                } else if (isPay) {
                                    historyLines.add("🟢 PAID: Php " + String.format("%.2f", record.getTotalAmount()) +
                                            "\n     Date: " + record.getTransactionDate());
                                }
                            }
                        }
                        status.onHistoryLoaded(historyLines);
                    }

                    @Override
                    public void onCancelled(com.google.firebase.database.DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {}
        });
    }

    // --- THE BOUNCER WALKIE-TALKIE ---
    public interface DebtCheckStatus {
        void onCheck(double currentDebt);
    }

    public void checkNeighborDebt(String debtorName, final DebtCheckStatus status) {
        mDatabase.child("Debts").child(debtorName).child("current_balance").addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                double currentDebt = 0.0;
                // If they exist and have a balance, grab it!
                if(snapshot.exists() && snapshot.getValue(Double.class) != null) {
                    currentDebt = snapshot.getValue(Double.class);
                }
                status.onCheck(currentDebt);
            }

            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {
                status.onCheck(0.0); // If internet fails, assume 0 so the app doesn't crash
            }
        });
    }


    // --- THE DASHBOARD ANALYTICS ENGINE (DYNAMIC V2) ---
    // --- THE DASHBOARD ANALYTICS ENGINE (PERFECTED) ---
    // --- THE DASHBOARD ANALYTICS ENGINE (PERFECTED) ---
    public interface DashboardCallback {
        void onDataLoaded(double totalRevenue, double totalUtang, String topItem, int topQty, java.util.TreeMap<String, Double> dailySales);
    }

    public void getDashboardStats(final DashboardCallback callback) {
        // 1. Fetch Utang
        mDatabase.child("Debts").addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot debtSnapshot) {
                double totalUtang = 0.0;
                for (com.google.firebase.database.DataSnapshot d : debtSnapshot.getChildren()) {
                    if (d.child("current_balance").exists()) totalUtang += d.child("current_balance").getValue(Double.class);
                }
                final double finalUtang = totalUtang;

                // 2. Fetch Money & Dates
                mDatabase.child("Transactions").addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(com.google.firebase.database.DataSnapshot txSnapshot) {
                        // Added 'final' here to prevent Java scope panics!
                        final double[] totalRevenue = {0.0};
                        final java.util.TreeMap<String, Double> dailySales = new java.util.TreeMap<>();

                        extractReceipts(txSnapshot, totalRevenue, dailySales);

                        // 3. Fetch Items
                        mDatabase.child("TransactionItems").addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                            @Override
                            public void onDataChange(com.google.firebase.database.DataSnapshot itemsSnapshot) {
                                java.util.HashMap<String, Integer> itemCounts = new java.util.HashMap<>();

                                for (com.google.firebase.database.DataSnapshot receiptNode : itemsSnapshot.getChildren()) {
                                    for (com.google.firebase.database.DataSnapshot itemNode : receiptNode.getChildren()) {
                                        String name = itemNode.child("productName").getValue(String.class);
                                        Integer qty = 1;
                                        if (itemNode.hasChild("currentQuantity")) qty = itemNode.child("currentQuantity").getValue(Integer.class);
                                        else if (itemNode.hasChild("qty")) qty = itemNode.child("qty").getValue(Integer.class);

                                        if (name != null) itemCounts.put(name, itemCounts.getOrDefault(name, 0) + qty);
                                    }
                                }

                                // 4. THE EXORCISM: Cross-reference with Active Inventory
                                mDatabase.child("Inventory").addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                                    @Override
                                    public void onDataChange(com.google.firebase.database.DataSnapshot inventorySnapshot) {
                                        java.util.HashSet<String> activeItems = new java.util.HashSet<>();

                                        for (com.google.firebase.database.DataSnapshot invNode : inventorySnapshot.getChildren()) {
                                            String activeName = invNode.child("productName").getValue(String.class);
                                            if (activeName != null) {
                                                activeItems.add(activeName);
                                            }
                                        }

                                        String topItem = "No Sales Yet";
                                        int topQty = 0;
                                        for (java.util.Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
                                            if (entry.getValue() > topQty
                                                    && !entry.getKey().contains("GCASH")
                                                    && !entry.getKey().contains("UTANG")
                                                    && activeItems.contains(entry.getKey())) {

                                                topItem = entry.getKey();
                                                topQty = entry.getValue();
                                            }
                                        }

                                        callback.onDataLoaded(totalRevenue[0], finalUtang, topItem, topQty, dailySales);
                                    }

                                    @Override
                                    public void onCancelled(com.google.firebase.database.DatabaseError error) {
                                        callback.onDataLoaded(totalRevenue[0], finalUtang, "Error", 0, dailySales);
                                    }
                                });
                            }
                            @Override
                            public void onCancelled(com.google.firebase.database.DatabaseError error) {
                                callback.onDataLoaded(totalRevenue[0], finalUtang, "Error", 0, dailySales);
                            }
                        });
                    }
                    @Override
                    public void onCancelled(com.google.firebase.database.DatabaseError error) {
                        callback.onDataLoaded(0.0, finalUtang, "Error", 0, new java.util.TreeMap<>());
                    }
                });
            }
            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {
                callback.onDataLoaded(0.0, 0.0, "Error", 0, new java.util.TreeMap<>());
            }
        });
    }

    // THE RECURSIVE DIGGER (Now strictly for Money and Dates)
    private void extractReceipts(com.google.firebase.database.DataSnapshot node, double[] totalRevenue, java.util.TreeMap<String, Double> dailySales) {
        if (node.hasChild("totalAmount")) {
            Double amount = node.child("totalAmount").getValue(Double.class);
            if (amount != null) {
                // 1. We removed the math from here!

                String dateKey = "Unknown";
                if (node.hasChild("date")) dateKey = node.child("date").getValue(String.class);
                else if (node.hasChild("transactionDate")) {
                    String fullDate = node.child("transactionDate").getValue(String.class);
                    if (fullDate != null && fullDate.length() >= 10) dateKey = fullDate.substring(0, 10);
                } else if (node.hasChild("timestamp")) {
                    Object ts = node.child("timestamp").getValue();
                    if (ts instanceof Long) dateKey = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date((Long) ts));
                    else if (ts != null) dateKey = ts.toString();
                } else {
                    com.google.firebase.database.DatabaseReference parentRef = node.getRef().getParent();
                    while (parentRef != null && !parentRef.getKey().equals("Transactions")) {
                        String pKey = parentRef.getKey();
                        if (pKey.matches(".*\\d+.*")) { dateKey = pKey; break; }
                        parentRef = parentRef.getParent();
                    }
                }

                // 2. We STILL add it to the graph regardless of the day
                dailySales.put(dateKey, dailySales.getOrDefault(dateKey, 0.0) + amount);

                // 3. THE FIX: We check if it's today, and ONLY THEN add it to the Dashboard Total!
                String todayStr = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
                if (dateKey.equals(todayStr)) {
                    totalRevenue[0] += amount;
                }
            }
        } else {
            for (com.google.firebase.database.DataSnapshot child : node.getChildren()) {
                extractReceipts(child, totalRevenue, dailySales);
            }
        }
    }


}