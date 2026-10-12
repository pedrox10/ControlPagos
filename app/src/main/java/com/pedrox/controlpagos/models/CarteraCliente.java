package com.pedrox.controlpagos.models;

import java.io.Serializable;

public class CarteraCliente implements Serializable {
    private int id;
    private String nombre;
    private String sector;
    private int pedidosConSaldo;
    private double saldoTotal;
    private boolean esMoroso;

    public CarteraCliente() {}

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public String getNombre()                   { return nombre; }
    public void setNombre(String nombre)        { this.nombre = nombre; }

    public String getSector()                    { return sector; }
    public void setSector(String sector)          { this.sector = sector; }

    public int getPedidosConSaldo()                         { return pedidosConSaldo; }
    public void setPedidosConSaldo(int pedidosConSaldo)     { this.pedidosConSaldo = pedidosConSaldo; }

    public double getSaldoTotal()                   { return Math.round(saldoTotal * 10.0) / 10.0; }
    public void setSaldoTotal(double saldoTotal)    { this.saldoTotal = saldoTotal; }

    public boolean isEsMoroso()                     { return esMoroso; }
    public void setEsMoroso(boolean esMoroso)       { this.esMoroso = esMoroso; }
}
