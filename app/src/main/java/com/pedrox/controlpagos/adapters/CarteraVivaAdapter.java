package com.pedrox.controlpagos.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.pedrox.controlpagos.R;
import com.pedrox.controlpagos.models.CarteraCliente;

import java.util.List;

public class CarteraVivaAdapter extends RecyclerView.Adapter<CarteraVivaAdapter.ViewHolder> {

    private List<CarteraCliente> lista;

    public CarteraVivaAdapter(List<CarteraCliente> lista) { this.lista = lista; }

    public void updateCartera(List<CarteraCliente> nueva) {
        lista.clear();
        if (nueva != null) lista.addAll(nueva);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cartera_cliente, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        CarteraCliente c   = lista.get(position);
        Context        ctx = h.itemView.getContext();

        // Si es moroso: nombre en rojo con ⚠, borde izquierdo rojo
        if (c.isEsMoroso()) {
            h.tvNombre.setText(c.getNombre() + " ⚠");
            h.tvNombre.setTextColor(ContextCompat.getColor(ctx, R.color.colorDanger));
            // Borde izquierdo rojo via background — requiere drawable o padding
            h.itemView.setBackgroundResource(R.drawable.bg_row_moroso);
        } else {
            h.tvNombre.setText(c.getNombre());
            h.tvNombre.setTextColor(ContextCompat.getColor(ctx, android.R.color.tab_indicator_text));
            h.itemView.setBackground(null);
        }

        h.tvGrupo.setText(c.getSector() != null ? c.getSector() : "—");
        h.tvPedidos.setText(String.valueOf(c.getPedidosConSaldo()));
        h.tvSaldo.setText("Bs " + String.format("%.1f", c.getSaldoTotal()));
    }

    @Override
    public int getItemCount() { return lista.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvGrupo, tvPedidos, tvSaldo;
        public ViewHolder(@NonNull View v) {
            super(v);
            tvNombre  = v.findViewById(R.id.tvCarteraNombre);
            tvGrupo   = v.findViewById(R.id.tvCarteraGrupo);
            tvPedidos = v.findViewById(R.id.tvCarteraPedidos);
            tvSaldo   = v.findViewById(R.id.tvCarteraSaldo);
        }
    }
}