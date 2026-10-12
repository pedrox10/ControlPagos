package com.pedrox.controlpagos.responses;

import com.pedrox.controlpagos.models.Usuario;

public class UsuarioRespuesta {
    private boolean error;
    private String mensaje;
    private Usuario data; // o null si hay error

    public boolean isError() { return error; }
    public String getMensaje() { return mensaje; }
    public Usuario getData() { return data; }
}
