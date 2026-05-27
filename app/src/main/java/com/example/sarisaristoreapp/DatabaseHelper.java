package com.example.sarisaristoreapp;

import android.content.Context;

import com.example.sarisaristoreapp.models.Product;
import com.example.sarisaristoreapp.models.TransactionRecord;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;

public class DatabaseHelper {

    private DatabaseReference mDatabase;

    public DatabaseHelper(Context context) {
        mDatabase = FirebaseDatabase.getInstance("https://sarisaristoreapp-99b12-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();
    }

    public boolean addProduct(Product product){
        mDatabase.child("Inventory").child(product.getBarcode()).setValue(product);
        return true;
    }

    public interface DashboardStatus {
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
                        if ("Tindahan".equals(record.getPurseType())) {
                            totalCash += record.getTotalAmount();
                            Double profitObj = child.child("totalProfit").getValue(Double.class);
                            if (profitObj != null) totalProfit += profitObj;
                        }
                        String fullDate = record.getTransactionDate();
                        if (fullDate != null && fullDate.length() >= 10) {
                            String justDate = fullDate.substring(0, 10);
                            float currentDayTotal = dailySales.getOrDefault(justDate, 0f);
                            dailySales.put(justDate, currentDayTotal + (float) record.getTotalAmount());
                        }
                    }
                }
                status.onDashboardLoaded(totalCash, totalProfit, dailySales);
            }
            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {
                status.onDashboardLoaded(0.0, 0.0, new java.util.LinkedHashMap<>());
            }
        });
    }

    public interface HistoryStatus {
        void onHistoryLoaded(ArrayList<TransactionRecord> historyList);
    }

    public interface ReceiptStatus {
        void onReceiptLoaded(ArrayList<String> itemList);
    }

    public void getAllTransactions(final HistoryStatus status){
        mDatabase.child("Transactions").addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                ArrayList<TransactionRecord> historyList = new ArrayList<>();
                for (com.google.firebase.database.DataSnapshot childNode : snapshot.getChildren()) {
                    TransactionRecord record = childNode.getValue(TransactionRecord.class);
                    if (record != null) {
                        record.setTransactionId(childNode.getKey());
                        historyList.add(0, record);
                    }
                }
                status.onHistoryLoaded(historyList);
            }
            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {
                status.onHistoryLoaded(new ArrayList<>());
            }
        });
    }

    public void getTransactionItems(String transactionId, final ReceiptStatus status){
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
                        double subTotal = p.getSellingPrice() * p.getCurrentQuantity();

                        if ("UTANG_PAYMENT".equals(p.getBarcode())) {
                            itemList.add(p.getProductName());
                        } else if ("GCASH".equals(p.getBarcode())) {
                            String itemLine = "GCash " + p.getCategory() + " | " + p.getProductName() + " | Total: ₱" + String.format("%.2f", p.getSellingPrice());
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
            public void onCancelled(com.google.firebase.database.DatabaseError error) {
                status.onReceiptLoaded(new ArrayList<>());
            }
        });
    }

    public boolean logDebt(String debtorName, double amount) {
        mDatabase.child("Debts").child(debtorName).child("current_balance").setValue(com.google.firebase.database.ServerValue.increment(amount));
        return true;
    }

    public interface InventoryStatus {
        void onDataLoaded(HashMap<String, Product> inventoryMap);
    }

    public void deleteProduct(String barcode) {
        mDatabase.child("Inventory").child(barcode).removeValue();
    }

    public void getAllProducts(final InventoryStatus status) {
        mDatabase.child("Inventory").addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                HashMap<String, Product> inventoryMap = new HashMap<>();

                for (com.google.firebase.database.DataSnapshot childNode : snapshot.getChildren()) {
                    Product p = childNode.getValue(Product.class);
                    inventoryMap.put(childNode.getKey(), p);
                }

                status.onDataLoaded(inventoryMap);
            }

            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {
                System.out.println("Firebase Error: " + error.getMessage());
                status.onDataLoaded(new HashMap<>());
            }
        });
    }

    public interface UtangStatus {
        void onUtangLoaded(java.util.LinkedHashMap<String, Double> debtMap);
    }

    public void getCloudDebts(final UtangStatus status) {
        mDatabase.child("Debts").addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                java.util.LinkedHashMap<String, Double> debtMap = new java.util.LinkedHashMap<>();
                for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                    Double balance = child.child("current_balance").getValue(Double.class);
                    if (balance != null && balance > 0) {
                        debtMap.put(child.getKey(), balance);
                    }
                }
                status.onUtangLoaded(debtMap);
            }
            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {
                status.onUtangLoaded(new java.util.LinkedHashMap<>());
            }
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

        double totalProfit = 0.0;
        for(Product p : soldItems){
            double itemProfit = (p.getSellingPrice() - p.getCostPrice()) * p.getCurrentQuantity();
            totalProfit += itemProfit;
        }
        receiptData.put("totalProfit", totalProfit);

        mDatabase.child("Transactions").child(receiptId).setValue(receiptData);

        for(Product currentItem : soldItems){
            mDatabase.child("TransactionItems").child(receiptId).push().setValue(currentItem);
            String barcode = currentItem.getBarcode();
            if (barcode != null && !barcode.equals("GCASH") && !barcode.equals("UTANG_PAYMENT")) {
                mDatabase.child("Inventory").child(barcode).child("currentQuantity").setValue(com.google.firebase.database.ServerValue.increment(-currentItem.getCurrentQuantity()));
            }
        }
        return true;
    }

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
                                            "\n     Date: " + record.getTransactionDate() + "\n     Items:\n" + itemsList.toString());

                                } else if (isPay) {
                                    historyLines.add("🟢 PAID: Php " + String.format("%.2f", record.getTotalAmount()) +
                                            "\n     Date: " + record.getTransactionDate());
                                }
                            }
                        }
                        status.onHistoryLoaded(historyLines);
                    }
                    @Override
                    public void onCancelled(com.google.firebase.database.DatabaseError error) {
                        status.onHistoryLoaded(new ArrayList<>());
                    }
                });
            }
            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {
                status.onHistoryLoaded(new ArrayList<>());
            }
        });
    }

    public interface DebtCheckStatus {
        void onCheck(double currentDebt);
    }

    public void checkNeighborDebt(String debtorName, final DebtCheckStatus status) {
        mDatabase.child("Debts").child(debtorName).child("current_balance").addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                double currentDebt = 0.0;
                if(snapshot.exists() && snapshot.getValue(Double.class) != null) {
                    currentDebt = snapshot.getValue(Double.class);
                }
                status.onCheck(currentDebt);
            }
            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {
                status.onCheck(0.0);
            }
        });
    }

    public interface DashboardCallback {
        void onDataLoaded(double totalRevenue, double totalUtang, String topItem, int topQty, java.util.TreeMap<String, Double> dailySales);
    }

    public void getDashboardStats(final DashboardCallback callback) {
        mDatabase.child("Debts").addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot debtSnapshot) {
                double totalUtang = 0.0;
                for (com.google.firebase.database.DataSnapshot d : debtSnapshot.getChildren()) {
                    if (d.child("current_balance").exists()) totalUtang += d.child("current_balance").getValue(Double.class);
                }
                final double finalUtang = totalUtang;

                mDatabase.child("Transactions").addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(com.google.firebase.database.DataSnapshot txSnapshot) {
                        final double[] totalRevenue = {0.0};
                        final java.util.TreeMap<String, Double> dailySales = new java.util.TreeMap<>();

                        extractReceipts(txSnapshot, totalRevenue, dailySales);

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
                                            if (entry.getValue() > topQty && !entry.getKey().contains("GCASH") && !entry.getKey().contains("UTANG") && activeItems.contains(entry.getKey())) {
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

    private void extractReceipts(com.google.firebase.database.DataSnapshot node, double[] totalRevenue, java.util.TreeMap<String, Double> dailySales) {
        if (node.hasChild("totalAmount")) {
            Double amount = node.child("totalAmount").getValue(Double.class);
            if (amount != null) {
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

                dailySales.put(dateKey, dailySales.getOrDefault(dateKey, 0.0) + amount);

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

    public void deleteTransaction(String transactionId) {
        mDatabase.child("TransactionItems").child(transactionId).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                for (com.google.firebase.database.DataSnapshot itemNode : snapshot.getChildren()) {
                    Product p = itemNode.getValue(Product.class);
                    if (p != null && p.getBarcode() != null && !p.getBarcode().equals("GCASH") && !p.getBarcode().equals("UTANG_PAYMENT")) {
                        mDatabase.child("Inventory").child(p.getBarcode()).child("currentQuantity")
                                .setValue(com.google.firebase.database.ServerValue.increment(p.getCurrentQuantity()));
                    }
                }
                mDatabase.child("Transactions").child(transactionId).removeValue();
                mDatabase.child("TransactionItems").child(transactionId).removeValue();
            }

            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) { }
        });
    }
}