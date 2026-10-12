package com.pedrox.controlpagos.models;

import java.io.Serializable;

public class CarteraCliente implements Serializable {
    private int id;
    private String nombre;
    private String grupo;
    private int pedidosConSaldo;
    private double saldoTotal;

    public CarteraCliente() {}

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public String getNombre()                   { return nombre; }
    public void setNombre(String nombre)        { this.nombre = nombre; }

    public String getGrupo()                    { return grupo; }
    public void setGrupo(String grupo)          { this.grupo = grupo; }

    public int getPedidosConSaldo()                         { return pedidosConSaldo; }
    public void setPedidosConSaldo(int pedidosConSaldo)     { this.pedidosConSaldo = pedidosConSaldo; }

    public double getSaldoTotal()                   { return Math.round(saldoTotal * 10.0) / 10.0; }
    public void setSaldoTotal(double saldoTotal)    { this.saldoTotal = saldoTotal; }
}
