package com.pedrox.controlpagos.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView; // Asegúrate de que este import exista
import com.pedrox.controlpagos.R;
import com.pedrox.controlpagos.models.Pedido;
import com.pedrox.controlpagos.models.ProductoPedido;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class PedidoAdapter extends RecyclerView.Adapter<PedidoAdapter.PedidoViewHolder> {

    private List<Pedido> listaPedidos;
    private OnPedidoClickListener onPedidoClickListener; // Solo un tipo de clic

    // Opcional: Listener para clics en el pedido si en el futuro quieres ver el detalle del pedido
    public interface OnPedidoClickListener {
        void onPedidoClick(Pedido pedido); // Solo clic simple para navegar
    }

    public PedidoAdapter(List<Pedido> pedidos) { // Constructor sin Context
        this.listaPedidos = pedidos;
    }

    public void setOnPedidoClickListener(OnPedidoClickListener listener) {
        this.onPedidoClickListener = listener;
    }

    public void updatePedidos(List<Pedido> newPedidos) {
        this.listaPedidos.clear();
        this.listaPedidos.addAll(newPedidos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PedidoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Context context = parent.getContext(); // No necesitas almacenar en variable de clase si solo se usa aquí
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pedido, parent, false);
        return new PedidoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PedidoViewHolder holder, int position) {
        Pedido pedido = listaPedidos.get(position);

        holder.tvPedidoFecha.setText(pedido.getFecha());
        holder.tvTotal.setText(pedido.getTotal() + "");
        holder.tvCuotasDe.setText(pedido.getCuotasDe() + "");
        holder.tvPagado.setText(pedido.getPagado() + "");
        holder.tvSaldo.setText(pedido.getSaldo() + "");

        // Construir la lista de productos
        StringBuilder productosText = new StringBuilder(); // Quitar "Productos: " inicial aquí
        if (pedido.getProductos() != null && !pedido.getProductos().isEmpty()) {
            for (ProductoPedido p : pedido.getProductos()) {
                productosText.append("- ").append(p.getProducto().getNombre()).append(" (")
                        .append(p.getCantidad()).append(" x ")
                        .append(p.getPrecio()).append(")").append("\n");
            }
            // Eliminar el último salto de línea, si existe
            if (productosText.length() > 0) {
                productosText.setLength(productosText.length() - 1);
            }
        } else {
            productosText.append("Sin productos"); // O dejar vacío si prefieres no mostrar "Ninguno"
        }
        holder.tvPedidoProductos.setText(productosText.toString());


        // Manejar clics (ahora solo clic simple para navegación)
        holder.itemView.setOnClickListener(v -> {
            if (onPedidoClickListener != null) {
                onPedidoClickListener.onPedidoClick(pedido);
            }
        });
        holder.itemView.setOnLongClickListener(null); // Eliminar el clic largo si no se usa
    }

    @Override
    public int getItemCount() {
        return listaPedidos.size();
    }

    // PedidoViewHolder ya está bien, solo asegúrate de los IDs en XML
    static class PedidoViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardPedido; // El ID de la tarjeta principal
        TextView tvPedidoFecha;
        TextView tvTotal;
        TextView tvCuotasDe;
        TextView tvPagado;
        TextView tvSaldo;
        TextView tvPedidoProductos; // Para mostrar la lista de productos

        public PedidoViewHolder(@NonNull View itemView) {
            super(itemView);
            cardPedido = itemView.findViewById(R.id.cardPedido);
            tvPedidoFecha = itemView.findViewById(R.id.tvPedidoFecha);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            tvCuotasDe = itemView.findViewById(R.id.tvCuotasDe);
            tvPagado = itemView.findViewById(R.id.tvPagado);
            tvSaldo = itemView.findViewById(R.id.tvSaldo);
            tvPedidoProductos = itemView.findViewById(R.id.tvPedidoProductos);
        }
    }
}