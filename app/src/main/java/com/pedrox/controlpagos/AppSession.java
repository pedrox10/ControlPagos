package com.pedrox.controlpagos;

import com.pedrox.controlpagos.models.Usuario;

public class AppSession {
    private static Usuario usuarioActual;

    public static void setUsuarioActual(Usuario usuario) {
        usuarioActual = usuario;
    }

    public static Usuario getUsuarioActual() {
        return usuarioActual;
    }

    public static boolean isLoggedIn() {
        return usuarioActual != null;
    }

    public static void logout() {
        usuarioActual = null;
    }
}
