package com.pedrox.controlpagos.responses;

import com.pedrox.controlpagos.models.Pago;

public class PagoRespuesta {
    private boolean error;
    private String mensaje;
    private Pago data; // o null si hay error

    public boolean isError() { return error; }
    public String getMensaje() { return mensaje; }
    public Pago getData() { return data; }
}
