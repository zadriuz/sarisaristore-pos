package com.example.sarisaristoreapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import com.example.sarisaristoreapp.models.Product;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private ArrayList<Product> checkoutCart;
    private CartUpdateListener listener;

    // The communications bridge back to MainActivity
    public interface CartUpdateListener {
        void onCartUpdated();
        int getMaxStock(String barcode);
    }

    public CartAdapter(ArrayList<Product> cart, CartUpdateListener listener) {
        this.checkoutCart = cart;
        this.listener = listener;
    }

    public class CartViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemName, tvItemStock, tvItemQty, tvItemPrice;
        ImageButton btnPlus, btnMinus;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvItemStock = itemView.findViewById(R.id.tvItemStock); // Using this to show barcode
            tvItemQty = itemView.findViewById(R.id.tvItemQty);
            tvItemPrice = itemView.findViewById(R.id.tvItemPrice);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            btnMinus = itemView.findViewById(R.id.btnMinus);
        }
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Blows up your Kanto Mint XML blueprint into a physical view
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart_card, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        Product currentItem = checkoutCart.get(position);

        holder.tvItemName.setText(currentItem.getProductName());
        holder.tvItemStock.setText("Barcode: " + currentItem.getBarcode());

        holder.tvItemQty.setText(String.valueOf(currentItem.getCurrentQuantity()));

        // Math Engine for the specific card
        double subtotal = currentItem.getSellingPrice() * currentItem.getCurrentQuantity();
        holder.tvItemPrice.setText(String.format("₱%.2f", subtotal));

        // --- INTERACTIVITY ---

        holder.btnPlus.setOnClickListener(v -> {
            int currentQty = currentItem.getCurrentQuantity();
            String barcode = currentItem.getBarcode();

            // Ping MainActivity to get the absolute truth about the stock
            int maxStock = listener.getMaxStock(barcode);

            if (currentQty >= maxStock) {
                android.widget.Toast.makeText(v.getContext(), "Max stock reached! Only " + maxStock + " available.", android.widget.Toast.LENGTH_SHORT).show();
                return; // The Bouncer stops the transaction here
            }

            currentItem.setCurrentQuantity(currentQty + 1);
            notifyItemChanged(holder.getAdapterPosition()); // Redraws just this card
            listener.onCartUpdated(); // Tells MainActivity to recalculate the Grand Total
        });

        holder.btnMinus.setOnClickListener(v -> {
            int currentQty = currentItem.getCurrentQuantity();
            if (currentQty <= 1) {
                // The Trapdoor: Destroys the item completely if they drop it to 0
                checkoutCart.remove(holder.getAdapterPosition());
                notifyItemRemoved(holder.getAdapterPosition());
                notifyItemRangeChanged(holder.getAdapterPosition(), checkoutCart.size());
            } else {
                currentItem.setCurrentQuantity(currentQty - 1);
                notifyItemChanged(holder.getAdapterPosition());
            }
            listener.onCartUpdated(); // Tells MainActivity to recalculate the Grand Total
        });
    }

    @Override
    public int getItemCount() {
        return checkoutCart.size();
    }
}