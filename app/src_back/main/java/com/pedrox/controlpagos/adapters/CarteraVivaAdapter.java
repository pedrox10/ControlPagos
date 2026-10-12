package com.pedrox.controlpagos.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
        CarteraCliente c = lista.get(position);
        h.tvNombre.setText(c.getNombre());
        h.tvGrupo.setText(c.getGrupo() != null ? c.getGrupo() : "—");
        h.tvPedidos.setText(String.valueOf(c.getPedidosConSaldo()));
        h.tvSaldo.setText("Bs " + c.getSaldoTotal());
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
