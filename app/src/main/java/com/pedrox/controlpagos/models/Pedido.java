package com.pedrox.controlpagos.models; // Asegúrate de que el paquete coincida con la estructura de tu proyecto

import java.io.Serializable; // Importar Serializable para pasar objetos entre Fragments/Activities
import java.util.ArrayList;
import java.util.List;

public class Pedido implements Serializable { // Implementar Serializable para pasar objetos

    private int id;
    private String fecha;
    private Cliente cliente;                  // relación con Cliente
    private List<ProductoPedido> productos;   // relación intermedia
    private List<Pago> pagos;

    // Constructor vacío (útil para algunas librerías o para instanciar y luego setear valores)
    public Pedido() {
        this.productos = new ArrayList<>(); // Inicializar listas para evitar NullPointerException
        this.pagos = new ArrayList<>();
    }

    public Pedido(int id, String fecha, Cliente cliente) {
        this.id = id;
        this.fecha = fecha;
        this.cliente = cliente;
        this.productos = new ArrayList<>(); // Inicializar listas para evitar NullPointerException
        this.pagos = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public double getTotal() {
        double total = 0;
        for (ProductoPedido productoPedido : productos) {
            total += productoPedido.getPrecio()*productoPedido.getCantidad();
        }
        return Math.round(total * 10.0) / 10.0;
    }

    public double getSaldo() {
        double saldo = getTotal() - getPagado();
        return Math.round(saldo * 10.0) / 10.0;
    }

    public List<ProductoPedido> getProductos() {
        return productos;
    }

    public void addProducto(ProductoPedido producto) {
        this.productos.add(producto);
    }

    public void editProducto(ProductoPedido producto) {
        for (int i = 0; i < this.productos.size(); i++) {
            ProductoPedido actual = this.productos.get(i);
            if (actual.getId() == producto.getId()) {
                this.productos.set(i, producto);
                break;
            }
        }
    }

    public void deleteProducto(ProductoPedido producto) {
        for (int i = 0; i < this.productos.size(); i++) {
            ProductoPedido actual = this.productos.get(i);
            if (actual.getId() == producto.getId()) {
                this.productos.remove(i);
                break;
            }
        }
    }

    public List<Pago> getPagos() {
        return pagos;
    }

    public double getPagado() {
        double pagado = 0;
        for (Pago pago : pagos) {
            pagado += pago.getMonto();
        }
        return Math.round(pagado * 10.0) / 10.0;
    }

    public void addPago(Pago pago) {
        this.pagos.add(pago);
    }

    public void editPago(Pago pago) {
        for (int i = 0; i < this.pagos.size(); i++) {
            Pago actual = this.pagos.get(i);
            if (actual.getId() == pago.getId()) {
                this.pagos.set(i, pago);
                break;
            }
        }
    }

    public void deletePago(Pago pago) {
        for (int i = 0; i < this.pagos.size(); i++) {
            Pago actual = this.pagos.get(i);
            if (actual.getId() == pago.getId()) {
                this.pagos.remove(i);
                break;
            }
        }
    }

    // Opcional: Para una mejor depuración
    @Override
    public String toString() {
        return "Pedido{" +
                "id=" + id +
                ", fechaEntrega=" + fecha +
                ", numProductos=" + (productos != null ? productos.size() : 0) +
                ", numPagos=" + (pagos != null ? pagos.size() : 0) +
                '}';
    }
}
