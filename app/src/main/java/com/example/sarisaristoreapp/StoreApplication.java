package com.example.sarisaristoreapp;

import android.app.Application;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class StoreApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Target your exact Asia-Southeast cloud instance
        FirebaseDatabase dbInstance = FirebaseDatabase.getInstance("https://sarisaristoreapp-99b12-default-rtdb.asia-southeast1.firebasedatabase.app/");

        // 2. Turn on the offline hard drive cache for THIS specific instance
        dbInstance.setPersistenceEnabled(true);

        // 3. Build the bridge using the exact same variable
        DatabaseReference mDatabase = dbInstance.getReference();

        // 4. Drop the anchors so it downloads in the background
        mDatabase.child("Inventory").keepSynced(true);
        mDatabase.child("Transactions").keepSynced(true);
        mDatabase.child("TransactionItems").keepSynced(true);
        mDatabase.child("Debts").keepSynced(true);
    }
}