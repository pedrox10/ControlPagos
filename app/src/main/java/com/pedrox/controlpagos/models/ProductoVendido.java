package com.pedrox.controlpagos.models;

import java.io.Serializable;

public class ProductoVendido implements Serializable {
    private int    id;
    private String nombre;
    private int    cantidad;
    private double total;

    public int    getId()       { return id; }
    public String getNombre()   { return nombre; }
    public int    getCantidad() { return cantidad; }
    public double getTotal()    { return total; }

    public void setId(int id)           { this.id = id; }
    public void setNombre(String n)     { this.nombre = n; }
    public void setCantidad(int c)      { this.cantidad = c; }
    public void setTotal(double t)      { this.total = t; }
}
