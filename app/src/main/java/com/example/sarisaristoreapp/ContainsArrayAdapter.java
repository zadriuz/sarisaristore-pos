package com.example.sarisaristoreapp;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ContainsArrayAdapter extends ArrayAdapter<String> {

    private final List<String> originalData;
    private final List<String> filteredData;
    private final ItemFilter mFilter = new ItemFilter();

    public ContainsArrayAdapter(@NonNull Context context, int resource, @NonNull List<String> objects) {
        super(context, resource, objects);
        this.originalData = new ArrayList<>(objects);
        this.filteredData = new ArrayList<>(objects);
    }

    @Override
    public int getCount() {
        return filteredData.size();
    }

    @Nullable
    @Override
    public String getItem(int position) {
        return filteredData.get(position);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return mFilter;
    }

    // Custom method to refresh data when Firebase updates
    public void updateData(List<String> newData) {
        originalData.clear();
        originalData.addAll(newData);
        filteredData.clear();
        filteredData.addAll(newData);
        notifyDataSetChanged();
    }

    private class ItemFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            String filterString = constraint != null ? constraint.toString().toLowerCase().trim() : "";
            FilterResults results = new FilterResults();
            final List<String> list = originalData;
            int count = list.size();
            final ArrayList<String> nlist = new ArrayList<>(count);

            for (int i = 0; i < count; i++) {
                String filterableString = list.get(i);
                // THE CORE FIX: Checking if it CONTAINS the string, ignoring case
                if (filterableString.toLowerCase().contains(filterString)) {
                    nlist.add(filterableString);
                }
            }

            results.values = nlist;
            results.count = nlist.size();
            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredData.clear();
            if (results.values != null) {
                filteredData.addAll((ArrayList<String>) results.values);
            }
            notifyDataSetChanged();
        }
    }
}