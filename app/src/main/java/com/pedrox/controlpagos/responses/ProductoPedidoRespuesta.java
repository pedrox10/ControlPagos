package com.pedrox.controlpagos.responses;

import com.pedrox.controlpagos.models.Pedido;
import com.pedrox.controlpagos.models.ProductoPedido;

public class ProductoPedidoRespuesta {
    private boolean error;
    private String mensaje;
    private ProductoPedido data; // o null si hay error

    public boolean isError() { return error; }
    public String getMensaje() { return mensaje; }
    public ProductoPedido getData() { return data; }
}