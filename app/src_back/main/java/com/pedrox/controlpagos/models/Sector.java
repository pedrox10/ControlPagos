package com.pedrox.controlpagos.models;

import java.io.Serializable;

public class Sector implements Serializable {
    private int id;
    private String nombre;
    private int totalClientes; // el servidor puede devolver cuántos clientes tiene

    public Sector() {}

    public int getId()                      { return id; }
    public void setId(int id)               { this.id = id; }

    public String getNombre()               { return nombre; }
    public void setNombre(String nombre)    { this.nombre = nombre; }

    public int getTotalClientes()                       { return totalClientes; }
    public void setTotalClientes(int totalClientes)     { this.totalClientes = totalClientes; }

    @Override
    public String toString() { return nombre; }
}
