package com.pedrox.controlpagos.models;

import java.io.Serializable;

public class Usuario implements Serializable {
    private int id; // ID para la base de datos (clave primaria, se generará en el backend)
    private String nombre;
    private String alias;
    private String clave;
    private String rol;
    private int habilitado;

    public Usuario() {

    }

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

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public int getHabilitado() {
        return habilitado;
    }

    public void setHabilitado(int habilitado) {
        this.habilitado = habilitado;
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", alias='" + alias + '\'' +
                ", clave='" + clave + '\'' +
                ", rol='" + rol + '\'' +
                ", habilitado='" + habilitado + '\'' +
                '}';
    }
}
