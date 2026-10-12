package com.pedrox.controlpagos.viewmodels;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jimmyworks.easyhttp.EasyHttp;
import com.jimmyworks.easyhttp.builder.RequestBuilder;
import com.jimmyworks.easyhttp.exception.HttpException;
import com.jimmyworks.easyhttp.listener.StringResponseListener;
import com.pedrox.controlpagos.Constants;
import com.pedrox.controlpagos.HttpClient;
import com.pedrox.controlpagos.models.Cliente;
import com.pedrox.controlpagos.responses.RespuestaLista;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Headers;

public class ClientesViewModel extends ViewModel {
    private static final String TAG = "ClientesViewModel";

    // ── Constantes de filtro de estado (segundo nivel) ────────────────────────
    public static final String ESTADO_TODOS      = "TODOS";
    public static final String ESTADO_PENDIENTES = "PENDIENTES";
    public static final String ESTADO_MOROSOS    = "MOROSOS";

    private final MutableLiveData<List<Cliente>> clientes          = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Cliente>> clientesFiltrados = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean>       cargando          = new MutableLiveData<>(false);

    // Los tres filtros — sobreviven a la navegación al detalle y al volver
    private String filtroGrupo  = "TODOS";
    private String filtroEstado = ESTADO_TODOS;
    private String filtroTexto  = "";

    // ── LiveData públicos ─────────────────────────────────────────────────────

    public LiveData<Boolean>       isCargando()           { return cargando; }
    public LiveData<List<Cliente>> getClientesFiltrados() { return clientesFiltrados; }

    // ── Getters para restaurar estado visual en el Fragment ───────────────────
    public String getFiltroGrupo()  { return filtroGrupo; }
    public String getFiltroEstado() { return filtroEstado; }
    public String getFiltroTexto()  { return filtroTexto; }

    // ── Setters ───────────────────────────────────────────────────────────────

    /**
     * Cambiar sector — resetea automáticamente estado y búsqueda.
     * El Fragment debe limpiar visualmente los chips y el SearchView.
     */
    public void setFiltroGrupo(String grupo) {
        this.filtroGrupo  = grupo;
        this.filtroEstado = ESTADO_TODOS;
        this.filtroTexto  = "";
        aplicarFiltros();
    }

    /** Cambiar estado sin resetear el sector. */
    public void setFiltroEstado(String estado) {
        this.filtroEstado = estado;
        aplicarFiltros();
    }

    /** Búsqueda por texto — no resetea otros filtros. */
    public void setFiltroTexto(String texto) {
        this.filtroTexto = texto;
        aplicarFiltros();
    }

    // ── Carga de datos ────────────────────────────────────────────────────────

    public void cargarClientes(Context context) {
        cargando.setValue(true);
        long tiempoInicio = System.currentTimeMillis();
        Log.d(TAG, "getClientes → iniciando request");

        RequestBuilder builder = EasyHttp.get(
                context.getApplicationContext(),
                Constants.API_BASE_URL + "clientes");
        builder.getOkHttpClientBuilder()
               .addInterceptor(HttpClient.interceptor(context));

        builder.build().getAsString(new StringResponseListener() {
            @Override
            public void onSuccess(@NonNull Headers headers, @NonNull String body) {
                try {
                    Type tipo = new TypeToken<RespuestaLista<Cliente>>() {}.getType();
                    RespuestaLista<Cliente> respuesta = new Gson().fromJson(body, tipo);
                    if (respuesta.isError()) {
                        Log.e(TAG, "Error: " + respuesta.getMensaje());
                    } else {
                        long d = System.currentTimeMillis() - tiempoInicio;
                        Log.d(TAG, "getClientes → OK en " + d + "ms — " +
                                respuesta.getData().size() + " clientes");
                        clientes.setValue(respuesta.getData());
                        aplicarFiltros();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parseando JSON", e);
                } finally {
                    cargando.setValue(false);
                }
            }

            @Override
            public void onError(@NonNull HttpException e) {
                Log.e(TAG, "Error HTTP: " + e.getMessage());
                cargando.setValue(false);
            }
        });
    }

    // ── Lógica de filtros ─────────────────────────────────────────────────────

    private void aplicarFiltros() {
        List<Cliente> listaOriginal = clientes.getValue();
        if (listaOriginal == null) return;

        List<Cliente> resultado = new ArrayList<>();

        for (Cliente c : listaOriginal) {

            // Filtro 1 — Sector
            if (!filtroGrupo.equals("TODOS")) {
                if (c.getGrupo() == null || !c.getGrupo().equalsIgnoreCase(filtroGrupo))
                    continue;
            }

            // Filtro 2 — Estado
            switch (filtroEstado) {
                case ESTADO_PENDIENTES:
                    // Sin pago hoy Y con saldo pendiente
                    // Excluye los que ya pagaron hoy Y los que no tienen saldo
                    if (c.getPagoHoy() != null || c.isHaPagadoTodo()) continue;
                    break;
                case ESTADO_MOROSOS:
                    if (!c.isEsMoroso()) continue;
                    break;
                // ESTADO_TODOS: sin filtro
            }

            // Filtro 3 — Texto
            if (!filtroTexto.isEmpty()) {
                String q      = filtroTexto.toLowerCase();
                String nombre = c.getNombre()     != null ? c.getNombre().toLowerCase()     : "";
                String ref    = c.getReferencia() != null ? c.getReferencia().toLowerCase() : "";
                String tel    = c.getTelefono()   != null ? c.getTelefono()                 : "";
                if (!nombre.contains(q) && !ref.contains(q) && !tel.contains(q)) continue;
            }

            resultado.add(c);
        }

        clientesFiltrados.setValue(resultado);
    }

    // ── Actualizaciones locales (sin recargar del servidor) ───────────────────

    /** Agregar cliente nuevo a la lista local. */
    public void agregarCliente(Cliente cliente) {
        List<Cliente> lista = clientes.getValue();
        if (lista == null) lista = new ArrayList<>();
        lista.add(cliente);
        clientes.setValue(lista);
        aplicarFiltros();
    }

    /**
     * Actualizar esMoroso en la lista local.
     * Evita recargar toda la lista solo por un cambio de estado.
     */
    public void actualizarMoroso(int clienteId, boolean esMoroso) {
        List<Cliente> lista = clientes.getValue();
        if (lista == null) return;
        for (Cliente c : lista) {
            if (c.getId() == clienteId) {
                c.setEsMoroso(esMoroso);
                break;
            }
        }
        clientes.setValue(lista);
        aplicarFiltros();
    }
}
