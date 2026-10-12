package com.pedrox.controlpagos.responses;

import java.util.List;

public class RespuestaLista<T> {
    private boolean error;
    private String mensaje;
    private List<T> data;

    public boolean isError() { return error; }
    public String getMensaje() { return mensaje; }
    public List<T> getData() { return data; }
}
