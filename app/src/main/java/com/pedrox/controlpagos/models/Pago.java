package com.pedrox.controlpagos.models; // Asegúrate de que el paquete coincida

import java.io.Serializable; // Importar Serializable
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;   // Para manejar la fecha del pago
import java.util.Locale;

public class Pago implements Serializable { // Implementar Serializable

    private int id; // ID del pago (clave primaria, generado en el backend)
    private Usuario usuario;
    private Pedido pedido; // ID del pedido al que se aplica este pago (clave foránea)
    private String fecha; // Fecha en que se realizó el pago
    private double monto; // Monto del pago

    // Constructor vacío
    public Pago() {}

    // Constructor con todos los campos
    public Pago(int id, Usuario usuario, Pedido pedido, String fecha, double monto) {
        this.id = id;
        this.usuario = usuario;
        this.pedido = pedido;
        this.fecha = fecha;
        this.monto = monto;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getFecha() {
        String fechaFormateada = "";
        try {
            SimpleDateFormat formatoServidor = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat formatoDeseado = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date fecha = formatoServidor.parse(this.fecha);
            fechaFormateada = formatoDeseado.format(fecha);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return fechaFormateada;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public double getMonto() {
        return Math.round(monto * 10.0) / 10.0;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    // Opcional: Para una mejor depuración
    @Override
    public String toString() {
        return "Pago{" +
                "id=" + id +
                ", pedido=" + pedido.getFecha() +
                ", fechaPago=" + fecha +
                ", monto=" + monto +
                '}';
    }
}