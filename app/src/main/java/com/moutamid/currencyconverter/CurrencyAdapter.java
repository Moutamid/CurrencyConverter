package com.moutamid.currencyconverter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class CurrencyAdapter extends RecyclerView.Adapter<CurrencyAdapter.CurrencyVH> {

    Context context;
    ArrayList<CurrencyModel> list;

    public CurrencyAdapter(Context context, ArrayList<CurrencyModel> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public CurrencyVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CurrencyVH(LayoutInflater.from(context).inflate(R.layout.currency, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CurrencyVH holder, int position) {
        CurrencyModel model = list.get(holder.getAdapterPosition());
        holder.base.setText(model.base);
        holder.convert.setText(model.convert);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class CurrencyVH extends RecyclerView.ViewHolder {
        TextView base, convert;

        public CurrencyVH(@NonNull View itemView) {
            super(itemView);
            base = itemView.findViewById(R.id.baseCurrency);
            convert = itemView.findViewById(R.id.convertCurrency);
        }
    }

}
