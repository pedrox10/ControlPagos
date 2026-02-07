package com.pedrox.controlpagos.adapters;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.pedrox.controlpagos.R;
import com.pedrox.controlpagos.models.Cliente;
import com.pedrox.controlpagos.models.Pago;
import com.pedrox.controlpagos.models.Usuario;

import java.util.List;

public class ClienteAdapter extends RecyclerView.Adapter<ClienteAdapter.ClienteViewHolder> {

    private List<Cliente> clientes;
    private OnClienteActionListener onClienteActionListener;

    public interface OnClienteActionListener {
        void onClienteClick(Cliente cliente);
    }

    public ClienteAdapter(List<Cliente> listaClientes) {
        this.clientes = listaClientes;
    }

    public void setOnClienteActionListener(OnClienteActionListener listener) {
        this.onClienteActionListener = listener;
    }

    public void updateClientes(List<Cliente> nuevosClientes) {
        clientes.clear();
        if (nuevosClientes != null) {
            clientes.addAll(nuevosClientes);
        }
        notifyDataSetChanged(); // <- sin esto, nunca se refresca
    }

    @Override
    public ClienteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cliente, parent, false);
        return new ClienteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClienteViewHolder holder, int position) {
        Cliente cliente = clientes.get(position);

        holder.tvNumeroCliente.setText((position + 1) + ".");
        holder.tvNombreCliente.setText(cliente.getNombre());

        holder.tvClienteReferencia.setText(cliente.getReferencia()); // Muestra el rubro/oficio
        if (cliente.getTelefono() == null || cliente.getTelefono().trim().isEmpty() || cliente.getTelefono().equals("0")) {
            holder.tvClienteTelefono.setText("Teléfono: ");
        } else {
            holder.tvClienteTelefono.setText("Teléfono: " + cliente.getTelefono());
        }
        // Muestra el teléfono con prefijo
        holder.tvClienteGrupo.setText("Grupo: " + cliente.getGrupo()); // Muestra el grupo con prefijo

        holder.btnWhatsApp.setOnClickListener(v -> {
            String telefono = cliente.getTelefono();
            if (telefono != null && !telefono.trim().isEmpty() && !telefono.equals("0")) {
                String url = "https://wa.me/591" + telefono;
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                try {
                    v.getContext().startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(v.getContext(), "WhatsApp no está instalado", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(v.getContext(), "Teléfono no válido", Toast.LENGTH_SHORT).show();
            }
        });
        if (cliente.getPagoHoy() != null) {
            holder.btnPagoHoy.setVisibility(View.VISIBLE);
            holder.btnPagoHoy.setOnClickListener(v -> {
                Pago pago = cliente.getPagoHoy();
                Usuario cobrador = pago.getUsuario();

                String mensaje = "💰 Monto: " + pago.getMonto() + " Bs\n" +
                        "📅 Fecha: " + pago.getFecha() + "\n" +
                        "👤 Cobrador: " + (cobrador != null ? cobrador.getNombre() : "Desconocido");

                new AlertDialog.Builder(v.getContext())
                        .setTitle("Pago del día")
                        .setMessage(mensaje)
                        .setPositiveButton("OK", null)
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show();
            });
        } else {
            holder.btnPagoHoy.setVisibility(View.INVISIBLE);
            holder.btnPagoHoy.setOnClickListener(null);
        }
        holder.itemView.setOnClickListener(v -> {
            if (onClienteActionListener != null) {
                onClienteActionListener.onClienteClick(cliente);
            }
        });
        holder.itemView.setOnLongClickListener(null);
        if (cliente.isHaPagadoTodo()) {
            holder.tvStamp.setVisibility(View.VISIBLE);
        } else {
            holder.tvStamp.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return clientes.size();
    }

    public static class ClienteViewHolder extends RecyclerView.ViewHolder {
        TextView tvNumeroCliente;
        TextView tvNombreCliente;
        TextView tvClienteReferencia; // Declaración para el nuevo TextView de rubro
        TextView tvClienteTelefono; // Declaración para el nuevo TextView de teléfono
        TextView tvClienteGrupo; // Declaración para el nuevo TextView de grupo
        MaterialButton btnWhatsApp;
        MaterialButton btnPagoHoy;
        TextView tvStamp;

        public ClienteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumeroCliente = itemView.findViewById(R.id.tvNumeroCliente);
            tvNombreCliente = itemView.findViewById(R.id.tvNombreCliente);
            tvClienteReferencia = itemView.findViewById(R.id.tvClienteRubro); // Enlazar con el ID del XML
            tvClienteTelefono = itemView.findViewById(R.id.tvClienteTelefono); // Enlazar con el ID del XML
            tvClienteGrupo = itemView.findViewById(R.id.tvClienteGrupo); // Enlazar con el ID del XML
            tvStamp = itemView.findViewById(R.id.tvStamp);
            btnWhatsApp = itemView.findViewById(R.id.btnWhatsApp); // Enlazar con el ID del XML
            btnPagoHoy = itemView.findViewById(R.id.btnPagoHoy); // Enlazar con el ID del XML
        }
    }
}