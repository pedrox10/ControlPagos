package com.pedrox.controlpagos.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pedrox.controlpagos.R;
import com.pedrox.controlpagos.models.Pago;
import com.pedrox.controlpagos.models.Pedido;

import java.util.List;

public class ReportePagosAdapter extends RecyclerView.Adapter<ReportePagosAdapter.ViewHolder> {

    private final List<Pago> listaPagos;

    public ReportePagosAdapter(List<Pago> listaPagos) {
        this.listaPagos = listaPagos;
    }

    public void updatePagos(List<Pago> pagos) {
        this.listaPagos.clear();
        this.listaPagos.addAll(pagos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reporte_pago, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Pago pago = listaPagos.get(position);

        holder.tvCliente.setText(pago.getPedido().getCliente().getNombre());
        holder.tvCobrador.setText(pago.getUsuario().getNombre());
        holder.tvFecha.setText(pago.getFecha());
        holder.tvMonto.setText(String.format("%.1f", pago.getMonto()));
    }

    @Override
    public int getItemCount() {
        return listaPagos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvCliente, tvFecha, tvMonto, tvCobrador;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvCliente = itemView.findViewById(R.id.tvCliente);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvMonto = itemView.findViewById(R.id.tvMonto);
            tvCobrador = itemView.findViewById(R.id.tvCobrador);
        }
    }
}