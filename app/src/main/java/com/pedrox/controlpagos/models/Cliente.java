package com.pedrox.controlpagos.models; // Asegúrate de que el paquete coincida con la estructura de tu proyecto

import java.io.Serializable; // Importar Serializable para pasar objetos entre Fragments/Activities
import java.util.ArrayList;
import java.util.List;

public class Cliente implements Serializable { // Implementar Serializable es importante para pasar objetos

    private int id; // ID para la base de datos (clave primaria, se generará en el backend)
    private String nombre;
    private String referencia;
    private String telefono;
    private String grupo;
    private Pago pagoHoy;
    private boolean haPagadoTodo;
    private List<Pedido> pedidos; // Un cliente puede tener varios pedidos

    // Constructor vacío (útil para algunas librerías o para instanciar y luego setear valores)
    public Cliente() {
        this.pedidos = new ArrayList<>(); // Inicializar la lista de pedidos para evitar NullPointerException
    }

    // Constructor con todos los campos principales (sin pedidos al inicio)
    public Cliente(int id, String nombre, String referencia, String telefono, String grupo, Pago pagoHoy, boolean haPagadoTodo) {
        this.id = id;
        this.nombre = nombre;
        this.referencia = referencia;
        this.telefono = telefono;
        this.grupo = grupo;
        this.pagoHoy = pagoHoy;
        this.haPagadoTodo = haPagadoTodo;
        this.pedidos = new ArrayList<>(); // Inicializar la lista de pedidos
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

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getGrupo() {
        return grupo;
    }

    public void setGrupo(String grupo) {
        this.grupo = grupo;
    }

    public List<Pedido> getPedidos() {
        return pedidos;
    }

    public void setPedidos(List<Pedido> pedidos) {
        this.pedidos = pedidos;
    }

    public Pago getPagoHoy() {
        return pagoHoy;
    }

    public void setPagoHoy(Pago pago) {
        this.pagoHoy = pago;
    }

    public boolean isHaPagadoTodo() {
        return haPagadoTodo;
    }

    public void setHaPagadoTodo(boolean haPagadoTodo) {
        this.haPagadoTodo = haPagadoTodo;
    }

    // Opcional: Para una mejor depuración o visualización en logs
    @Override
    public String toString() {
        return "Cliente{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", referencia='" + referencia + '\'' +
                ", telefono='" + telefono + '\'' +
                ", pagoHoy='" + pagoHoy + '\'' +
                ", haPagadoTodo='" + haPagadoTodo + '\'' +
                ", grupo='" + grupo + '\'' +
                ", numPedidos=" + (pedidos != null ? pedidos.size() : 0) +
                '}';
    }
}