package com.pedrox.controlpagos.models;

import java.io.Serializable; // Importar Serializable

public class Producto implements Serializable { // Implementar Serializable

    private int id; // ID del producto (si viene de una base de datos de productos preexistentes)
    private String nombre; // Nombre del producto (ej. "Libro de Matemáticas", "Pelota de Fútbol")
    private int habilitado;

    // Constructor vacío
    public Producto() {}

    // Constructor con todos los campos
    public Producto(int id, String nombre, int cantidad, double precioUnitario) {
        this.id = id;
        this.nombre = nombre;
    }

    // --- Getters y Setters ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getHabilitado() {
        return habilitado;
    }

    public void setHabilitado(int habilitado) {
        this.habilitado = habilitado;
    }

    // Opcional: Para una mejor depuración
    @Override
    public String toString() {
        return "Producto{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                '}';
    }
}