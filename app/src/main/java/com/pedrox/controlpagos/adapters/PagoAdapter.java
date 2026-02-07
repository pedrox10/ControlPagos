package com.pedrox.controlpagos.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pedrox.controlpagos.R; // Asegúrate de importar R
import com.pedrox.controlpagos.models.Pago;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class PagoAdapter extends RecyclerView.Adapter<PagoAdapter.PagoViewHolder> {

    private List<Pago> listaPagos;
    private OnPagoClickListener onPagoClickListener;

    public interface OnPagoClickListener {
        void onPagoClick(Pago pago);
    }

    public void setOnPagoClickListener(OnPagoClickListener listener) {
        this.onPagoClickListener = listener;
    }

    public PagoAdapter(List<Pago> listaPagos) {
        this.listaPagos = listaPagos;
    }

    public void updatePagos(List<Pago> nuevosPagos) {
        this.listaPagos = nuevosPagos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PagoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pago, parent, false);
        return new PagoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PagoViewHolder holder, int position) {
        Pago pago = listaPagos.get(position);

        holder.tvPagoNumero.setText((position + 1) + ""); // Muestra el número de ítem (1., 2., etc.)
        holder.tvPagoFecha.setText(pago.getFecha());
        holder.tvPagoMonto.setText(pago.getMonto() + "");
        holder.itemView.setOnClickListener(v -> {
            if (onPagoClickListener != null) {
                onPagoClickListener.onPagoClick(pago);
            }
        });
        holder.itemView.setOnLongClickListener(null);
    }

    @Override
    public int getItemCount() {
        return listaPagos.size();
    }

    static class PagoViewHolder extends RecyclerView.ViewHolder {
        TextView tvPagoNumero;
        TextView tvPagoFecha;
        TextView tvPagoMonto;

        public PagoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPagoNumero = itemView.findViewById(R.id.tvPagoNumero);
            tvPagoFecha = itemView.findViewById(R.id.tvPagoFecha);
            tvPagoMonto = itemView.findViewById(R.id.tvPagoMonto);
        }
    }
}