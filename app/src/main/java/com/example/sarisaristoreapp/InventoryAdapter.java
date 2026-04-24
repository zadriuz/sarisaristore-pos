package com.example.sarisaristoreapp;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.sarisaristoreapp.models.Product;
import java.util.ArrayList;

public class InventoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<Product> productList;
    private static final int TYPE_ITEM = 0;
    private static final int TYPE_FOOTER = 1;

    public InventoryAdapter(ArrayList<Product> productList) {
        this.productList = productList;
    }

    @Override
    public int getItemViewType(int position) {
        // If we reach the very end of the list, tell Android to load the Footer!
        if (position == productList.size()) return TYPE_FOOTER;
        return TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_FOOTER) {
            // THE DYNAMIC FOOTER ENGINE: Drawing the text purely in Java!
            TextView footer = new TextView(parent.getContext());
            footer.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            footer.setText("- End of List -");
            footer.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            footer.setTextColor(android.graphics.Color.parseColor("#94A3B8"));
            footer.setTextSize(15);
            footer.setTypeface(null, Typeface.BOLD);

            footer.setPadding(0, 32, 0, 100); // Gives it plenty of breathing room at the bottom
            return new RecyclerView.ViewHolder(footer) {};
        }

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inventory, parent, false);
        return new InventoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // If it's the footer, do nothing. It's already drawn!
        if (getItemViewType(position) == TYPE_FOOTER) return;

        InventoryViewHolder itemHolder = (InventoryViewHolder) holder;
        Product currentItem = productList.get(position);

        itemHolder.tvName.setText(currentItem.getProductName());
        itemHolder.tvPrice.setText(String.format("₱%.2f", currentItem.getSellingPrice()));

        String cat = currentItem.getCategory();
        if (cat == null || cat.trim().isEmpty()) {
            itemHolder.tvCategory.setText("UNCATEGORIZED");
            itemHolder.tvCategory.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FECACA")));
            itemHolder.tvCategory.setTextColor(android.graphics.Color.parseColor("#B91C1C"));
        } else {
            itemHolder.tvCategory.setText(cat.toUpperCase());
            itemHolder.tvCategory.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E2E8F0")));
            itemHolder.tvCategory.setTextColor(android.graphics.Color.parseColor("#475569"));
        }

        int stock = currentItem.getCurrentQuantity();
        if (stock <= 5) {
            itemHolder.tvStock.setTextColor(android.graphics.Color.parseColor("#DC2626"));
            itemHolder.tvPrice.setTextColor(android.graphics.Color.parseColor("#DC2626"));
            if (stock <= 0) {
                itemHolder.tvStock.setText("OUT OF STOCK");
            } else {
                itemHolder.tvStock.setText("LOW STOCK: " + stock);
            }
        } else {
            itemHolder.tvStock.setTextColor(android.graphics.Color.parseColor("#64748B"));
            itemHolder.tvPrice.setTextColor(itemHolder.btnEdit.getContext().getResources().getColor(R.color.kanto_primary));
            itemHolder.tvStock.setText("Stock: " + stock);
        }

        itemHolder.btnEdit.setOnClickListener(v -> {
            if (v.getContext() instanceof InventoryActivity) {
                ((InventoryActivity) v.getContext()).openEditProductPanel(currentItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        // We add +1 mathematically to make room for the Footer!
        return productList.size() + 1;
    }

    public void filterList(ArrayList<Product> filteredList) {
        this.productList = filteredList;
        notifyDataSetChanged();
    }

    public static class InventoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice, tvStock, tvCategory;
        ImageView btnEdit;

        public InventoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCardName);
            tvCategory = itemView.findViewById(R.id.tvCardCategory);
            tvPrice = itemView.findViewById(R.id.tvCardPrice);
            tvStock = itemView.findViewById(R.id.tvCardStock);
            btnEdit = itemView.findViewById(R.id.btnEditItem);
        }
    }
}