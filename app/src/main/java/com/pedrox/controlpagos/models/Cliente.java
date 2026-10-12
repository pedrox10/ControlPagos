package com.pedrox.controlpagos.models;

import android.util.Log;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class Cliente implements Serializable {

    private int id;
    private String nombre;
    private String referencia;
    private String telefono;
    private Sector sector;
    private Pago ultimoPago;
    private boolean haPagadoTodo;
    private boolean esMoroso;
    private boolean esArchivado;
    private List<Pedido> pedidos;

    public Cliente() {
        this.pedidos = new ArrayList<>();
    }

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }

    public String getNombre()                       { return nombre; }
    public void setNombre(String nombre)            { this.nombre = nombre; }

    public String getReferencia()                   { return referencia; }
    public void setReferencia(String referencia)    { this.referencia = referencia; }

    public String getTelefono()                     { return telefono; }
    public void setTelefono(String telefono)        { this.telefono = telefono; }

    public Sector getSector()                       { return sector; }
    public void setSector(Sector sector)            { this.sector = sector; }

    public Pago getUltimoPago()             { return ultimoPago; }
    public void setUltimoPago(Pago p)       { this.ultimoPago = p; }
    public boolean haPagadoHoy() {
        if (ultimoPago == null) return false;
        try {
            // ✅ Usar el campo fecha raw, no getFecha() que ya convirtió el formato
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate fechaPago = LocalDate.parse(ultimoPago.getFecha(), fmt);
            return fechaPago.equals(LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isHaPagadoTodo()                 { return haPagadoTodo; }
    public void setHaPagadoTodo(boolean v)          { this.haPagadoTodo = v; }

    public boolean isEsMoroso()                     { return esMoroso; }
    public void setEsMoroso(boolean esMoroso)       { this.esMoroso = esMoroso; }

    public boolean isEsArchivado()                     { return esArchivado; }
    public void setEsArchivado(boolean archivado)       { this.esArchivado = archivado; }

    public List<Pedido> getPedidos()                { return pedidos; }
    public void setPedidos(List<Pedido> pedidos)    { this.pedidos = pedidos; }

    @Override
    public String toString() {
        return "Cliente{id=" + id + ", nombre='" + nombre + "', sector='" + (sector != null ? sector.getNombre() : "null") +
                "', esMoroso=" + esMoroso + ", haPagadoTodo=" + haPagadoTodo + "}";
    }
}
