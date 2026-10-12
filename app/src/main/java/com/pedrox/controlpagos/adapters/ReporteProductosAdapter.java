package com.pedrox.controlpagos.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pedrox.controlpagos.R;
import com.pedrox.controlpagos.models.ProductoVendido;

import java.util.List;
import java.util.Locale;

public class ReporteProductosAdapter
        extends RecyclerView.Adapter<ReporteProductosAdapter.ViewHolder> {

    private List<ProductoVendido> lista;

    public ReporteProductosAdapter(List<ProductoVendido> lista) {
        this.lista = lista;
    }

    public void updateProductos(List<ProductoVendido> nueva) {
        lista.clear();
        if (nueva != null) lista.addAll(nueva);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reporte_producto, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        ProductoVendido p = lista.get(position);
        h.tvNombre.setText(p.getNombre());
        h.tvCantidad.setText(String.valueOf(p.getCantidad()));
        h.tvTotal.setText(String.format(Locale.getDefault(),
                "%.1f", p.getTotal()));
    }

    @Override
    public int getItemCount() { return lista.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvCantidad, tvTotal;
        public ViewHolder(@NonNull View v) {
            super(v);
            tvNombre   = v.findViewById(R.id.tvProductoNombre);
            tvCantidad = v.findViewById(R.id.tvProductoCantidad);
            tvTotal    = v.findViewById(R.id.tvProductoTotal);
        }
    }
}
