package com.example.sarisaristoreapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.sarisaristoreapp.models.Product;
import java.util.ArrayList;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {

    private ArrayList<Product> productList;

    public InventoryAdapter(ArrayList<Product> productList) {
        this.productList = productList;
    }

    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inventory, parent, false);
        return new InventoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {
        Product currentItem = productList.get(position);

        holder.tvName.setText(currentItem.getProductName());
        holder.tvBarcode.setText("BARCODE: " + currentItem.getBarcode());
        holder.tvPrice.setText(String.format("₱%.2f", currentItem.getSellingPrice()));

        int stock = currentItem.getCurrentQuantity();

        // --- LOW STOCK ALERT ENGINE ---
        if (stock <= 5) {
            // Force Pale Red (#DC2626) for both stock AND price!
            holder.tvStock.setTextColor(android.graphics.Color.parseColor("#DC2626"));
            holder.tvPrice.setTextColor(android.graphics.Color.parseColor("#DC2626"));

            if (stock <= 0) {
                holder.tvStock.setText("OUT OF STOCK");
            } else {
                holder.tvStock.setText("LOW STOCK: " + stock);
            }
        } else {
            // Default Kanto Mint colors
            holder.tvStock.setTextColor(android.graphics.Color.parseColor("#64748B"));
            holder.tvPrice.setTextColor(holder.btnEdit.getContext().getResources().getColor(R.color.kanto_primary));
            holder.tvStock.setText("Stock: " + stock);
        }

        // --- THE MASTER EDIT TRAPDOOR ---
        // --- THE MASTER EDIT TRAPDOOR ---
        holder.btnEdit.setOnClickListener(v -> {
            com.google.android.material.bottomsheet.BottomSheetDialog editDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(v.getContext());
            View sheetView = LayoutInflater.from(v.getContext()).inflate(R.layout.layout_edit_product_sheet, null);

            android.widget.EditText etBarcode = sheetView.findViewById(R.id.etEditBarcode);
            android.widget.EditText etName = sheetView.findViewById(R.id.etEditName);
            android.widget.EditText etCost = sheetView.findViewById(R.id.etEditCost);
            android.widget.EditText etSell = sheetView.findViewById(R.id.etEditSell);
            android.widget.EditText etQty = sheetView.findViewById(R.id.etEditQty);
            ImageView btnScan = sheetView.findViewById(R.id.btnEditScan);
            android.widget.Button btnDelete = sheetView.findViewById(R.id.btnEditDelete);
            android.widget.Button btnSave = sheetView.findViewById(R.id.btnEditSave);

            // Pre-fill existing data!
            etBarcode.setText(currentItem.getBarcode());
            etName.setText(currentItem.getProductName());
            etCost.setText(String.valueOf(currentItem.getCostPrice()));
            etSell.setText(String.valueOf(currentItem.getSellingPrice()));
            etQty.setText(String.valueOf(currentItem.getCurrentQuantity()));

            // Wire the Scanner Hook!
            btnScan.setOnClickListener(v1 -> {
                // We ask the main Activity to launch the scanner for us
                if (v.getContext() instanceof InventoryActivity) {
                    ((InventoryActivity) v.getContext()).triggerEditScanner(etBarcode);
                }
            });

            // Delete Logic with Custom UI Trapdoor
            btnDelete.setOnClickListener(v1 -> {
                android.app.Dialog customDialog = new android.app.Dialog(v.getContext());
                customDialog.setContentView(R.layout.layout_alert_dialog);

                // CRITICAL: Makes the background transparent so your rounded corners actually show!
                if (customDialog.getWindow() != null) {
                    customDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                }

                android.widget.TextView tvTitle = customDialog.findViewById(R.id.tvAlertTitle);
                android.widget.TextView tvMessage = customDialog.findViewById(R.id.tvAlertMessage);
                android.widget.Button btnCancel = customDialog.findViewById(R.id.btnAlertCancel);
                android.widget.Button btnConfirm = customDialog.findViewById(R.id.btnAlertConfirm);

                tvTitle.setText("Delete Item?");
                tvMessage.setText("Are you sure you want to permanently delete " + currentItem.getProductName() + "? This cannot be undone.");
                btnConfirm.setText("Delete");

                btnCancel.setOnClickListener(view -> customDialog.dismiss());

                btnConfirm.setOnClickListener(view -> {
                    DatabaseHelper db = new DatabaseHelper(v.getContext());
                    db.deleteProduct(currentItem.getBarcode());
                    android.widget.Toast.makeText(v.getContext(), "Item Deleted!", android.widget.Toast.LENGTH_SHORT).show();

                    int pos = holder.getAdapterPosition();
                    productList.remove(pos);
                    notifyItemRemoved(pos);

                    customDialog.dismiss();
                    editDialog.dismiss();
                });

                customDialog.show();
            });

            // Save Logic
            btnSave.setOnClickListener(v1 -> {
                String newName = etName.getText().toString();
                double newCost = Double.parseDouble(etCost.getText().toString());
                double newPrice = Double.parseDouble(etSell.getText().toString());
                int newQty = Integer.parseInt(etQty.getText().toString());
                String newBarcode = etBarcode.getText().toString();

                currentItem.setProductName(newName);
                currentItem.setCostPrice(newCost);
                currentItem.setSellingPrice(newPrice);
                currentItem.setCurrentQuantity(newQty);

                // If they changed the barcode, we'd theoretically need to delete old & save new,
                // but for now, updating the fields works for basic edits.
                currentItem.setBarcode(newBarcode);

                DatabaseHelper db = new DatabaseHelper(v.getContext());
                db.addProduct(currentItem);

                android.widget.Toast.makeText(v.getContext(), "Item Updated!", android.widget.Toast.LENGTH_SHORT).show();
                notifyItemChanged(holder.getAdapterPosition());
                editDialog.dismiss();
            });

            editDialog.setContentView(sheetView);
            editDialog.show();
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    // THE SEARCH ENGINE FILTER WIRING
    public void filterList(ArrayList<Product> filteredList) {
        this.productList = filteredList;
        notifyDataSetChanged();
    }

    // THE PROPERLY STRUCTURED VIEWHOLDER
    public static class InventoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice, tvStock, tvBarcode;
        ImageView btnEdit; // SAFELY TUCKED INSIDE!

        public InventoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCardName);
            tvBarcode = itemView.findViewById(R.id.tvCardBarcode);
            tvPrice = itemView.findViewById(R.id.tvCardPrice);
            tvStock = itemView.findViewById(R.id.tvCardStock);
            btnEdit = itemView.findViewById(R.id.btnEditItem); // LINKED PERFECTLY!
        }
    }
}