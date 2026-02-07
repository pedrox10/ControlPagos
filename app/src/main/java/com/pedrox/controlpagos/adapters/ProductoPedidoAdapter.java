package com.pedrox.controlpagos.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pedrox.controlpagos.R; // Asegúrate de importar R
import com.pedrox.controlpagos.models.ProductoPedido;

import java.util.List;

public class ProductoPedidoAdapter extends RecyclerView.Adapter<ProductoPedidoAdapter.ProductoViewHolder> {

    private List<ProductoPedido> listaProductos;

    public ProductoPedidoAdapter(List<ProductoPedido> listaProductos) {
        this.listaProductos = listaProductos;
    }

    public void updateProductos(List<ProductoPedido> nuevosProductos) {
        this.listaProductos = nuevosProductos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_producto_pedido, parent, false);
        return new ProductoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductoViewHolder holder, int position) {
        ProductoPedido producto = listaProductos.get(position);
        holder.tvProductName.setText(
                String.format("- %s (%d x Bs%.2f)",
                        producto.getProducto().getNombre(),
                        producto.getCantidad(),
                        producto.getPrecio())
        );
        // Si añades listeners para editar/eliminar, irían aquí
    }

    @Override
    public int getItemCount() {
        return listaProductos.size();
    }

    public static class ProductoViewHolder extends RecyclerView.ViewHolder {
        TextView tvProductName;

        public ProductoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProductName = itemView.findViewById(R.id.tvProductName);
        }
    }
}
