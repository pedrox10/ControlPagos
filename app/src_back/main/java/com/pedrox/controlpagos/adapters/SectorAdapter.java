package com.pedrox.controlpagos.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.pedrox.controlpagos.R;
import com.pedrox.controlpagos.models.Sector;

import java.util.List;

public class SectorAdapter extends RecyclerView.Adapter<SectorAdapter.ViewHolder> {

    private List<Sector> sectores;
    private OnSectorActionListener listener;

    public interface OnSectorActionListener {
        void onEditarSector(Sector sector);
        void onEliminarSector(Sector sector);
    }

    public SectorAdapter(List<Sector> sectores) { this.sectores = sectores; }

    public void setOnSectorActionListener(OnSectorActionListener l) { this.listener = l; }

    public void updateSectores(List<Sector> nuevos) {
        sectores.clear();
        if (nuevos != null) sectores.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sector, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Sector s = sectores.get(position);
        h.tvNombre.setText(s.getNombre());
        h.tvClientes.setText(s.getTotalClientes() + " clientes");
        h.btnEditar.setOnClickListener(v -> { if (listener != null) listener.onEditarSector(s); });
        h.btnEliminar.setOnClickListener(v -> { if (listener != null) listener.onEliminarSector(s); });
    }

    @Override
    public int getItemCount() { return sectores.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvClientes;
        MaterialButton btnEditar, btnEliminar;
        public ViewHolder(@NonNull View v) {
            super(v);
            tvNombre   = v.findViewById(R.id.tvSectorNombre);
            tvClientes = v.findViewById(R.id.tvSectorClientes);
            btnEditar  = v.findViewById(R.id.btnEditarSector);
            btnEliminar = v.findViewById(R.id.btnEliminarSector);
        }
    }
}
