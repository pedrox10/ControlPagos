package com.pedrox.controlpagos;

import android.content.Context;
import android.content.SharedPreferences;

import com.pedrox.controlpagos.models.Usuario;

public class AppSession {

    private static final String PREFS_NAME  = "app_session";
    private static final String KEY_ID      = "usuario_id";
    private static final String KEY_NOMBRE  = "usuario_nombre";
    private static final String KEY_ALIAS   = "usuario_alias";
    private static final String KEY_ROL     = "usuario_rol";
    private static final String KEY_HABILITADO = "usuario_habilitado";

    // Caché en memoria para acceso rápido
    private static Usuario usuarioActual;

    // ─── Guardar sesión ───────────────────────────────────────────────────────

    public static void guardar(Context context, Usuario usuario) {
        usuarioActual = usuario;

        SharedPreferences.Editor editor = prefs(context).edit();
        editor.putInt(KEY_ID,           usuario.getId());
        editor.putString(KEY_NOMBRE,    usuario.getNombre());
        editor.putString(KEY_ALIAS,     usuario.getAlias());
        editor.putString(KEY_ROL,       usuario.getRol());
        editor.putBoolean(KEY_HABILITADO, usuario.isHabilitado());
        editor.apply();
    }

    // ─── Restaurar sesión desde SharedPreferences (al arrancar la app) ───────

    public static boolean restaurar(Context context) {
        if (usuarioActual != null) return true; // ya está en memoria

        SharedPreferences sp = prefs(context);
        int id = sp.getInt(KEY_ID, -1);
        if (id == -1) return false; // no hay sesión guardada

        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(sp.getString(KEY_NOMBRE, ""));
        u.setAlias(sp.getString(KEY_ALIAS, ""));
        u.setRol(sp.getString(KEY_ROL, ""));
        u.setHabilitado(sp.getBoolean(KEY_HABILITADO, true));

        usuarioActual = u;
        return true;
    }

    // ─── Cerrar sesión ────────────────────────────────────────────────────────

    public static void logout(Context context) {
        usuarioActual = null;
        prefs(context).edit().clear().apply();
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public static Usuario getUsuarioActual() {
        return usuarioActual;
    }

    public static boolean isLoggedIn() {
        return usuarioActual != null;
    }

    public static boolean esAdmin() {
        return usuarioActual != null
                && "Admin".equalsIgnoreCase(usuarioActual.getRol());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
