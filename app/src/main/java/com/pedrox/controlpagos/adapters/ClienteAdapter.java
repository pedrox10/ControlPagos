package com.pedrox.controlpagos.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.pedrox.controlpagos.R;
import com.pedrox.controlpagos.models.Cliente;
import com.pedrox.controlpagos.models.Pago;
import com.pedrox.controlpagos.models.Usuario;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

public class ClienteAdapter extends RecyclerView.Adapter<ClienteAdapter.ClienteViewHolder> {

    // ── Modo de visualización ─────────────────────────────────────────────────
    public enum Modo { NORMAL, ARCHIVADOS }

    private List<Cliente> clientes;
    private final Modo modo;
    private OnClienteActionListener onClienteActionListener;

    // ── Interfaz de callbacks ─────────────────────────────────────────────────
    public interface OnClienteActionListener {
        void onClienteClick(Cliente cliente);
        void onRestaurarCliente(Cliente cliente);
    }

    // ── Constructores ─────────────────────────────────────────────────────────

    // Constructor sin modo → por defecto NORMAL
    public ClienteAdapter(List<Cliente> lista) {
        this.clientes = lista;
        this.modo     = Modo.NORMAL;
    }

    // Constructor con modo explícito
    public ClienteAdapter(List<Cliente> lista, Modo modo) {
        this.clientes = lista;
        this.modo     = modo;
    }

    public void setOnClienteActionListener(OnClienteActionListener listener) {
        this.onClienteActionListener = listener;
    }

    public void updateClientes(List<Cliente> nuevosClientes) {
        clientes.clear();
        if (nuevosClientes != null) clientes.addAll(nuevosClientes);
        notifyDataSetChanged();
    }

    // ── Inflate ───────────────────────────────────────────────────────────────
    @NonNull
    @Override
    public ClienteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cliente, parent, false);
        return new ClienteViewHolder(view);
    }

    // ── Bind ──────────────────────────────────────────────────────────────────
    @Override
    public void onBindViewHolder(@NonNull ClienteViewHolder holder, int position) {
        Cliente cliente = clientes.get(position);

        holder.tvNumeroCliente.setText((position + 1) + ".");
        holder.tvNombreCliente.setText(cliente.getNombre());
        holder.tvClienteReferencia.setText(cliente.getReferencia());

        String tel = cliente.getTelefono();
        boolean tieneTelefono = tel != null && !tel.trim().isEmpty() && !tel.equals("0");
        holder.tvClienteTelefono.setText(tieneTelefono ? tel : "—");

        // ✅ Null-safe para sector
        String nombreSector = (cliente.getSector() != null)
                ? cliente.getSector().getNombre() : "Sin sector";
        holder.tvClienteGrupo.setText(nombreSector);

        if (modo == Modo.ARCHIVADOS) {
            // ── Modo archivados — mostrar solo lo esencial + botón restaurar ──
            holder.btnWhatsApp.setVisibility(View.GONE);
            holder.btnPagoHoy.setVisibility(View.GONE);
            holder.tvStamp.setVisibility(View.GONE);
            holder.tvMoroso.setVisibility(View.GONE);
            holder.tvUltimoPago.setVisibility(View.GONE);
            holder.btnReactivar.setVisibility(View.VISIBLE);
            holder.btnReactivar.setOnClickListener(v -> {
                if (onClienteActionListener != null)
                    onClienteActionListener.onRestaurarCliente(cliente);
            });
            holder.itemView.setOnClickListener(null);

        } else {
            // ── Modo normal ───────────────────────────────────────────────────
            holder.btnReactivar.setVisibility(View.GONE);
            holder.btnWhatsApp.setVisibility(View.VISIBLE);

            // Badge moroso
            holder.tvMoroso.setVisibility(
                    cliente.isEsMoroso() ? View.VISIBLE : View.GONE);

            // Stamp sin saldo
            holder.tvStamp.setVisibility(
                    cliente.isHaPagadoTodo() ? View.VISIBLE : View.GONE);

            // Botón WhatsApp
            holder.btnWhatsApp.setOnClickListener(v -> {
                if (!tieneTelefono) {
                    Toast.makeText(v.getContext(),
                            "Teléfono no válido", Toast.LENGTH_SHORT).show();
                    return;
                }
                mostrarDialogWhatsApp(v, cliente, tel);
            });

            // Botón pago de hoy
            if (cliente.haPagadoHoy()) {
                holder.btnPagoHoy.setVisibility(View.VISIBLE);
                holder.btnPagoHoy.setOnClickListener(v -> {
                    Pago pago = cliente.getUltimoPago();
                    Usuario cobrador = pago.getUsuario();
                    String mensaje = "💰 Monto: " + pago.getMonto() + " Bs\n"
                            + "📅 Fecha: " + pago.getFecha() + "\n"
                            + "👤 Cobrador: " + (cobrador != null
                            ? cobrador.getNombre() : "Desconocido");
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

            if (cliente.getUltimoPago() != null) {
                holder.tvUltimoPago.setVisibility(View.VISIBLE);
                holder.tvUltimoPago.setText("Último pago: " + formatearUltimoPago(cliente.getUltimoPago().getFecha()));
            } else {
                holder.tvUltimoPago.setVisibility(View.GONE);
            }

            // Click para ver detalle
            holder.itemView.setOnClickListener(v -> {
                if (onClienteActionListener != null)
                    onClienteActionListener.onClienteClick(cliente);
            });
        }
    }

    @Override
    public int getItemCount() { return clientes.size(); }

    // ── Dialog WhatsApp ───────────────────────────────────────────────────────
    private void mostrarDialogWhatsApp(View v, Cliente cliente, String tel) {
        Context ctx = v.getContext();
        View dialogView = LayoutInflater.from(ctx)
                .inflate(R.layout.dialog_whatsapp, null);

        TextInputEditText etMensaje = dialogView.findViewById(R.id.etMensajeWA);
        MaterialButton    btnEnviar = dialogView.findViewById(R.id.btnEnviarWA);
        MaterialButton    btnLlamar = dialogView.findViewById(R.id.btnLlamarWA);

        String predeterminado = "📌 *Recordatorio*" + "\n"
                + "¡Hola! " + cliente.getNombre() + ". Por favor, no olvides realizar la cancelación de tu cuota pendiente.\n"
                + "Agradecemos que puedas ponerte al día ¡Muchas gracias! 😊";

        etMensaje.setText(predeterminado);

        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setView(dialogView)
                .setTitle("Enviar mensaje a " + cliente.getNombre())
                .setNegativeButton("Cancelar", (d, i) -> d.dismiss())
                .create();

        btnEnviar.setOnClickListener(btn -> {
            String texto = etMensaje.getText() != null
                    ? etMensaje.getText().toString().trim()
                    : predeterminado;
            String url = "https://wa.me/591" + tel + "?text=" + Uri.encode(texto);
            try {
                ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception e) {
                Toast.makeText(ctx, "WhatsApp no está instalado",
                        Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        btnLlamar.setOnClickListener(btn -> {
            String url = "https://wa.me/591" + tel;
            try {
                ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception e) {
                Toast.makeText(ctx, "WhatsApp no está instalado",
                        Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private String formatearUltimoPago(String fechaISO) {
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate fecha = LocalDate.parse(fechaISO, fmt);
            long dias = ChronoUnit.DAYS.between(fecha, LocalDate.now());
            if (dias == 0) return "Hoy";
            if (dias == 1) return "Ayer";
            if (dias <= 6) return "Hace " + dias + " días";
            return fecha.format(DateTimeFormatter
                    .ofPattern("dd/MMM/yyyy", new Locale("es")));
        } catch (Exception e) { return fechaISO; }
    }


    // ── ViewHolder ────────────────────────────────────────────────────────────
    public static class ClienteViewHolder extends RecyclerView.ViewHolder {
        TextView       tvNumeroCliente;
        TextView       tvNombreCliente;
        TextView       tvClienteReferencia;
        TextView       tvClienteTelefono;
        Chip           tvClienteGrupo;
        TextView       tvUltimoPago;
        TextView       tvStamp;
        TextView       tvMoroso;
        Button         btnReactivar;
        MaterialButton btnWhatsApp;
        MaterialButton btnPagoHoy;

        public ClienteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumeroCliente     = itemView.findViewById(R.id.tvNumeroCliente);
            tvNombreCliente     = itemView.findViewById(R.id.tvNombreCliente);
            tvClienteReferencia = itemView.findViewById(R.id.tvClienteRubro);
            tvClienteTelefono   = itemView.findViewById(R.id.tvClienteTelefono);
            tvClienteGrupo      = itemView.findViewById(R.id.tvClienteGrupo);
            tvUltimoPago        = itemView.findViewById(R.id.tvUltimoPago);
            tvStamp             = itemView.findViewById(R.id.tvStamp);
            tvMoroso            = itemView.findViewById(R.id.tvMoroso);
            btnReactivar        = itemView.findViewById(R.id.btnReactivar);
            btnWhatsApp         = itemView.findViewById(R.id.btnWhatsApp);
            btnPagoHoy          = itemView.findViewById(R.id.btnPagoHoy);
        }
    }
}