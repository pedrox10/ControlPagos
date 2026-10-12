package com.pedrox.controlpagos.models;

import java.io.Serializable;

public class ProductoPedido implements Serializable { // Implementar Serializable

    private int id;
    private Pedido pedido;
    private Producto producto;
    private int cantidad;
    private double precio;

    public ProductoPedido() {}

    // Constructor con todos los campos
    public ProductoPedido(int id, Pedido pedido, Producto producto, int cantidad, double precioUnitario) {
        this.id = id;
        this.pedido = pedido;
        this.producto = producto;
        this.precio = precioUnitario;
        this.cantidad = cantidad;
    }

    // --- Getters y Setters ---

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

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public double getPrecio() {
        return precio;
    }

    public double getTotal() {
        return precio*cantidad;
    }
    public void setPrecio(double precio) {
        this.precio = precio;
    }

    // Opcional: Para una mejor depuración
    @Override
    public String toString() {
        return "Producto{" +
                "id=" + id +
                ", pedido='" + pedido.toString() + '\'' +
                ", producto='" + producto.toString() + '\'' +
                ", cantidad=" + cantidad +
                ", precioUnitario=" + precio +
                '}';
    }
}