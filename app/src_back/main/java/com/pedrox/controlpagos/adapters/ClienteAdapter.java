package com.pedrox.controlpagos.adapters;

import android.app.Activity;
import android.content.Context;
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
import com.google.android.material.textfield.TextInputEditText;

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
        if (nuevosClientes != null) clientes.addAll(nuevosClientes);
        notifyDataSetChanged();
    }

    @Override
    public ClienteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cliente, parent, false);
        return new ClienteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClienteViewHolder holder, int position) {
        Cliente cliente = clientes.get(position);

        holder.tvNumeroCliente.setText((position + 1) + ".");
        holder.tvNombreCliente.setText(cliente.getNombre());
        holder.tvClienteReferencia.setText(cliente.getReferencia());

        String tel = cliente.getTelefono();
        boolean tieneTelefono = tel != null && !tel.trim().isEmpty() && !tel.equals("0");
        holder.tvClienteTelefono.setText(tieneTelefono ? "Teléfono: " + tel : "Teléfono: —");
        holder.tvClienteGrupo.setText("Grupo: " + cliente.getGrupo());

        // ── Badge moroso ──────────────────────────────────────────────────────
        // NUEVO: mostrar badge rojo si el cliente está marcado como moroso
        if (cliente.isEsMoroso()) {
            holder.tvMoroso.setVisibility(View.VISIBLE);
        } else {
            holder.tvMoroso.setVisibility(View.GONE);
        }

        // ── Stamp sin saldo ───────────────────────────────────────────────────
        holder.tvStamp.setVisibility(cliente.isHaPagadoTodo() ? View.VISIBLE : View.GONE);

        // ── WhatsApp con dialog de preview ────────────────────────────────────
        holder.btnWhatsApp.setOnClickListener(v -> {
            if (!tieneTelefono) {
                Toast.makeText(v.getContext(), "Teléfono no válido", Toast.LENGTH_SHORT).show();
                return;
            }
            mostrarDialogWhatsApp(v, cliente, tel);
        });

        // ── Botón pago de hoy ─────────────────────────────────────────────────
        if (cliente.getPagoHoy() != null) {
            holder.btnPagoHoy.setVisibility(View.VISIBLE);
            holder.btnPagoHoy.setOnClickListener(v -> {
                Pago pago = cliente.getPagoHoy();
                Usuario cobrador = pago.getUsuario();
                String mensaje = "💰 Monto: " + pago.getMonto() + " Bs\n"
                        + "📅 Fecha: " + pago.getFecha() + "\n"
                        + "👤 Cobrador: " + (cobrador != null ? cobrador.getNombre() : "Desconocido");
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

        // ── Click para ver detalle ────────────────────────────────────────────
        holder.itemView.setOnClickListener(v -> {
            if (onClienteActionListener != null) {
                onClienteActionListener.onClienteClick(cliente);
            }
        });
    }

    @Override
    public int getItemCount() { return clientes.size(); }

    /**
     * Muestra un dialog con preview del mensaje WhatsApp antes de enviarlo.
     * El usuario puede editar el texto antes de abrir WhatsApp.
     */
    private void mostrarDialogWhatsApp(View v, Cliente cliente, String tel) {
        Context ctx = v.getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        View dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_whatsapp, null);

        TextView tvPreview         = dialogView.findViewById(R.id.tvPreviewMensaje);
        TextInputEditText etMensaje = dialogView.findViewById(R.id.etMensajeWA);
        MaterialButton btnEnviar   = dialogView.findViewById(R.id.btnEnviarWA);

        // Mensaje predeterminado
        String mensajePredeterminado = "Hola " + cliente.getNombre() + " 👋\n"
                + "Le recordamos que tiene un saldo pendiente en nuestro negocio.\n"
                + "Por favor comuníquese con su cobrador para regularizar su cuenta. 🙏";

        tvPreview.setText(mensajePredeterminado);
        etMensaje.setText(mensajePredeterminado);

        AlertDialog dialog = builder.setView(dialogView)
                .setTitle("Enviar mensaje a " + cliente.getNombre())
                .setNegativeButton("Cancelar", (d, i) -> d.dismiss())
                .create();

        btnEnviar.setOnClickListener(btn -> {
            String texto = etMensaje.getText() != null
                    ? etMensaje.getText().toString().trim()
                    : mensajePredeterminado;
            String url = "https://wa.me/591" + tel + "?text=" + Uri.encode(texto);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            try {
                ctx.startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(ctx, "WhatsApp no está instalado", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    public static class ClienteViewHolder extends RecyclerView.ViewHolder {
        TextView     tvNumeroCliente;
        TextView     tvNombreCliente;
        TextView     tvClienteReferencia;
        TextView     tvClienteTelefono;
        TextView     tvClienteGrupo;
        TextView     tvStamp;
        TextView     tvMoroso;      // NUEVO
        MaterialButton btnWhatsApp;
        MaterialButton btnPagoHoy;

        public ClienteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumeroCliente    = itemView.findViewById(R.id.tvNumeroCliente);
            tvNombreCliente    = itemView.findViewById(R.id.tvNombreCliente);
            tvClienteReferencia = itemView.findViewById(R.id.tvClienteRubro);
            tvClienteTelefono  = itemView.findViewById(R.id.tvClienteTelefono);
            tvClienteGrupo     = itemView.findViewById(R.id.tvClienteGrupo);
            tvStamp            = itemView.findViewById(R.id.tvStamp);
            tvMoroso           = itemView.findViewById(R.id.tvMoroso); // NUEVO
            btnWhatsApp        = itemView.findViewById(R.id.btnWhatsApp);
            btnPagoHoy         = itemView.findViewById(R.id.btnPagoHoy);
        }
    }
}
