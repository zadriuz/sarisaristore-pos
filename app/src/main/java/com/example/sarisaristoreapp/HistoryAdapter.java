    package com.example.sarisaristoreapp;

    import android.graphics.Color;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.ImageView;
    import android.widget.TextView;

    import androidx.annotation.NonNull;
    import androidx.recyclerview.widget.RecyclerView;

    import com.example.sarisaristoreapp.models.TransactionRecord;

    import java.util.ArrayList;

    public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

        private ArrayList<TransactionRecord> historyList;
        private OnReceiptClickListener listener;

        // THE TRAPDOOR: This interface passes the click back to the Activity
        public interface OnReceiptClickListener {
            void onReceiptClick(TransactionRecord record);
        }

        public HistoryAdapter(ArrayList<TransactionRecord> historyList, OnReceiptClickListener listener) {
            this.historyList = historyList;
            this.listener = listener;
        }

        @NonNull
        @Override
        public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Inflate the floating card blueprint we built
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction_card, parent, false);
            return new HistoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
            TransactionRecord record = historyList.get(position);

            // --- THE DYNAMIC DATE GROUPING ENGINE ---
            String currentDateRaw = record.getTransactionDate();
            // SQLite saves as "YYYY-MM-DD HH:MM:SS". We chop off the time to group by Day.
            String currentDate = currentDateRaw.contains(" ") ? currentDateRaw.split(" ")[0] : currentDateRaw;

            // If it's the very first item, ALWAYS show the date header
            if (position == 0) {
                holder.tvDateHeader.setVisibility(View.VISIBLE);
                holder.tvDateHeader.setText(currentDate);
            } else {
                // Check the previous item's date
                String previousDateRaw = historyList.get(position - 1).getTransactionDate();
                String previousDate = previousDateRaw.contains(" ") ? previousDateRaw.split(" ")[0] : previousDateRaw;

                // If the dates are different, show the header! If they are the same, hide it.
                if (!currentDate.equals(previousDate)) {
                    holder.tvDateHeader.setVisibility(View.VISIBLE);
                    holder.tvDateHeader.setText(currentDate);
                } else {
                    holder.tvDateHeader.setVisibility(View.GONE);
                }
            }
            // --- END ENGINE ---

            // 1. Time / Date
            holder.tvTime.setText(record.getTransactionDate().replace(" ", "\n"));

            // 2. Summary
            // If your TransactionRecord model has a getPaymentMethod(), you can append it here!
            holder.tvSummary.setText(record.getPurseType());

            // 3. Amount
            holder.tvAmount.setText(String.format("₱%.2f", record.getTotalAmount()));

            // 4. Smart Status Badge Logic
            // Safely extract both strings. (Requires getPaymentMethod() in your model!)
            String purseStr = record.getPurseType() != null ? record.getPurseType().toLowerCase() : "";
            String paymentStr = record.getPaymentMethod() != null ? record.getPaymentMethod().toLowerCase() : "";

            if (purseStr.contains("payment")) {
                // Money coming IN to settle debt
                holder.tvStatusBadge.setText("REMITTED");
                holder.tvStatusBadge.setTextColor(Color.parseColor("#2E7D32")); // Kanto Green
            } else if (paymentStr.contains("utang")) {
                // Items going OUT on credit
                holder.tvStatusBadge.setText("UTANG");
                holder.tvStatusBadge.setTextColor(Color.parseColor("#E53935")); // Kanto Red
            } else {
                // Normal Cash/GCash purchase
                holder.tvStatusBadge.setText("PAID");
                holder.tvStatusBadge.setTextColor(Color.parseColor("#2E7D32")); // Kanto Green
            }

            // 5. Set the Device Info
            // (Requires getDeviceName() in your model!)
            holder.tvDeviceName.setText("Device: " + record.getDeviceInfo());

            // 6. Wire the Click Listener
            holder.itemView.setOnClickListener(v -> {
                listener.onReceiptClick(record);
            });

            // 5. Wire the Click Listener
            holder.itemView.setOnClickListener(v -> {
                listener.onReceiptClick(record);
            });
        }

        @Override
        public int getItemCount() {
            return historyList.size();
        }

        // THE VIEWHOLDER: Maps the Java variables to the XML IDs
        public static class HistoryViewHolder extends RecyclerView.ViewHolder {
            TextView tvTime, tvSummary, tvAmount, tvStatusBadge;
            ImageView imgTransactionType;
            TextView tvDateHeader;
            TextView tvDeviceName;

            public HistoryViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTime = itemView.findViewById(R.id.tvTime);
                tvSummary = itemView.findViewById(R.id.tvSummary);
                tvAmount = itemView.findViewById(R.id.tvAmount);
                tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
                imgTransactionType = itemView.findViewById(R.id.imgTransactionType);
                tvDateHeader = itemView.findViewById(R.id.tvDateHeader);
                tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            }
        }
    }