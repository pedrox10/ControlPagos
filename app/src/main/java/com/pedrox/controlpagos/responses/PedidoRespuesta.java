package com.pedrox.controlpagos.responses;

import com.pedrox.controlpagos.models.Pedido;

public class PedidoRespuesta {
    private boolean error;
    private String mensaje;
    private Pedido data; // o null si hay error

    public boolean isError() { return error; }
    public String getMensaje() { return mensaje; }
    public Pedido getData() { return data; }
}