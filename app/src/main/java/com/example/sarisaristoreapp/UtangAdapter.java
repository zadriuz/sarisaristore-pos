package com.example.sarisaristoreapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class UtangAdapter extends RecyclerView.Adapter<UtangAdapter.UtangViewHolder> {

    private ArrayList<String> namesList;
    private ArrayList<Double> balancesList;
    private OnUtangActionListener listener;

    // THE TRAPDOOR: Now we have TWO distinct actions!
    public interface OnUtangActionListener {
        void onViewDetails(String name, double balance);
        void onSettleDebt(String name, double balance);
    }

    public UtangAdapter(ArrayList<String> namesList, ArrayList<Double> balancesList, OnUtangActionListener listener) {
        this.namesList = namesList;
        this.balancesList = balancesList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UtangViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_utang_card, parent, false);
        return new UtangViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UtangViewHolder holder, int position) {
        String name = namesList.get(position);
        double balance = balancesList.get(position);

        holder.tvNeighborName.setText(name);
        holder.tvNeighborBalance.setText(String.format("₱%.2f", balance));

        // Wire the split buttons
        holder.btnViewDetails.setOnClickListener(v -> listener.onViewDetails(name, balance));
        holder.btnSettleDebt.setOnClickListener(v -> listener.onSettleDebt(name, balance));
    }

    @Override
    public int getItemCount() {
        return namesList.size();
    }

    public static class UtangViewHolder extends RecyclerView.ViewHolder {
        TextView tvNeighborName, tvNeighborBalance;
        Button btnViewDetails, btnSettleDebt;

        public UtangViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNeighborName = itemView.findViewById(R.id.tvNeighborName);
            tvNeighborBalance = itemView.findViewById(R.id.tvNeighborBalance);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            btnSettleDebt = itemView.findViewById(R.id.btnSettleDebt);
        }
    }
}