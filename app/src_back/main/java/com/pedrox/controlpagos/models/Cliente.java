package com.pedrox.controlpagos.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Cliente implements Serializable {

    private int id;
    private String nombre;
    private String referencia;
    private String telefono;
    private String grupo;
    private Pago pagoHoy;
    private boolean haPagadoTodo;
    // NUEVO: estado moroso — el servidor devuelve 0/1, Gson lo mapea a boolean
    private boolean esMoroso;
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

    public String getGrupo()                        { return grupo; }
    public void setGrupo(String grupo)              { this.grupo = grupo; }

    public Pago getPagoHoy()                        { return pagoHoy; }
    public void setPagoHoy(Pago pago)               { this.pagoHoy = pago; }

    public boolean isHaPagadoTodo()                 { return haPagadoTodo; }
    public void setHaPagadoTodo(boolean v)          { this.haPagadoTodo = v; }

    public boolean isEsMoroso()                     { return esMoroso; }
    public void setEsMoroso(boolean esMoroso)       { this.esMoroso = esMoroso; }

    public List<Pedido> getPedidos()                { return pedidos; }
    public void setPedidos(List<Pedido> pedidos)    { this.pedidos = pedidos; }

    @Override
    public String toString() {
        return "Cliente{id=" + id + ", nombre='" + nombre + "', grupo='" + grupo +
                "', esMoroso=" + esMoroso + ", haPagadoTodo=" + haPagadoTodo + "}";
    }
}
