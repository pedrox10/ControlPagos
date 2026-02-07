package com.pedrox.controlpagos.responses;

import com.pedrox.controlpagos.models.Cliente;

public class ClienteRespuesta {
    private boolean error;
    private String mensaje;
    private Cliente data; // o null si hay error

    public boolean isError() { return error; }
    public String getMensaje() { return mensaje; }
    public Cliente getData() { return data; }
}