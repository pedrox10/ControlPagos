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
import com.pedrox.controlpagos.models.Pago;
import com.pedrox.controlpagos.models.Sector;
import com.pedrox.controlpagos.responses.RespuestaLista;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Headers;

public class ClientesViewModel extends ViewModel {
    private static final String TAG = "ClientesViewModel";

    // ── Constantes de filtro de estado ────────────────────────────────────────
    public static final String ESTADO_TODOS      = "TODOS";
    public static final String ESTADO_PENDIENTES = "PENDIENTES";
    public static final String ESTADO_MOROSOS    = "MOROSOS";

    private final MutableLiveData<List<Sector>> sectores = new MutableLiveData<>();
    private final MutableLiveData<List<Cliente>> clientes          = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Cliente>> clientesFiltrados = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean>       cargando          = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> contadorClientes = new MutableLiveData<>(0);

    // Los tres filtros — independientes, acumulables, sobreviven navegación
    private String filtroGrupo  = "TODOS";
    private String filtroEstado = ESTADO_TODOS;
    private String filtroTexto  = "";

    // ── LiveData públicos ─────────────────────────────────────────────────────
    public LiveData<Boolean>       isCargando()           { return cargando; }
    public LiveData<List<Cliente>> getClientesFiltrados() { return clientesFiltrados; }
    public LiveData<Integer> getContadorClientes() { return contadorClientes; }

    // ── Getters para restaurar estado visual en el Fragment ───────────────────
    public LiveData<List<Sector>> getSectores() { return sectores; }
    public String getFiltroGrupo()  { return filtroGrupo; }
    public String getFiltroEstado() { return filtroEstado; }
    public String getFiltroTexto()  { return filtroTexto; }

    // ── Setters — cada filtro es independiente ────────────────────────────────

    /**
     * Cambiar sector sin resetear estado ni búsqueda.
     * Todos los filtros son acumulables — el usuario controla cada uno.
     */
    public void setFiltroGrupo(String grupo) {
        this.filtroGrupo = grupo;
        aplicarFiltros();
    }

    /** Cambiar estado sin afectar sector ni búsqueda. */
    public void setFiltroEstado(String estado) {
        this.filtroEstado = estado;
        aplicarFiltros();
    }

    /** Búsqueda por texto sin afectar otros filtros. */
    public void setFiltroTexto(String texto) {
        this.filtroTexto = texto;
        aplicarFiltros();
    }


    // ── Carga de datos ────────────────────────────────────────────────────────
    public void cargarSectores(Context context) {
        Log.d(TAG, "cargarSectores → getValue()=" + sectores.getValue());
        if (sectores.getValue() != null && !sectores.getValue().isEmpty()) {
            Log.d(TAG, "cargarSectores → CACHE, no hace request. size=" + sectores.getValue().size());
            return;
        }
        Log.d(TAG, "cargarSectores → haciendo request a: " + Constants.API_BASE_URL + "sectores");

        RequestBuilder rb = EasyHttp.get(context, Constants.API_BASE_URL + "sectores");
        rb.getOkHttpClientBuilder().addInterceptor(HttpClient.interceptor(context));
        rb.build().getAsString(new StringResponseListener() {
            @Override
            public void onSuccess(@NonNull Headers headers, @NonNull String body) {
                Log.d(TAG, "cargarSectores → onSuccess body=" + body.substring(0, Math.min(body.length(), 200)));
                try {
                    Type tipo = new TypeToken<RespuestaLista<Sector>>() {}.getType();
                    RespuestaLista<Sector> respuesta = new Gson().fromJson(body, tipo);
                    Log.d(TAG, "cargarSectores → isError=" + respuesta.isError() + " data=" + (respuesta.getData() != null ? respuesta.getData().size() : "null"));
                    if (!respuesta.isError()) {
                        List<Sector> lista = new ArrayList<>();
                        Sector todos = new Sector();
                        todos.setId(-1);
                        todos.setNombre("TODOS");
                        lista.add(todos);
                        lista.addAll(respuesta.getData());
                        Log.d(TAG, "cargarSectores → postValue con " + lista.size() + " sectores");
                        sectores.postValue(lista);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "cargarSectores → ERROR parseando", e);
                }
            }
            @Override
            public void onError(@NonNull HttpException e) {
                Log.e(TAG, "cargarSectores → onError: " + e.getMessage());
            }
        });
    }

    public void cargarClientes(Context context) {
        cargando.postValue(true); // ← postValue es seguro desde cualquier hilo
        long tiempoInicio = System.currentTimeMillis();

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
                        Log.d(TAG, "getClientes → OK en " + d + "ms — "
                                + respuesta.getData().size() + " clientes");
                        // ✅ postValue — seguro desde hilo de red
                        clientes.postValue(respuesta.getData());
                        List<Cliente> resultado = filtrarLista(respuesta.getData()); // ver abajo
                        clientesFiltrados.postValue(resultado);
                        contadorClientes.postValue(resultado.size());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parseando JSON", e);
                } finally {
                    cargando.postValue(false); // ✅ postValue
                }
            }

            @Override
            public void onError(@NonNull HttpException e) {
                Log.e(TAG, "Error HTTP: " + e.getMessage());
                cargando.postValue(false); // ✅ postValue
            }
        });
    }

    // aplicarFiltros() ahora es seguro desde cualquier hilo
    private void aplicarFiltros() {
        List<Cliente> lista = clientes.getValue();
        if (lista == null) return;
        List<Cliente> resultado = filtrarLista(lista);
        clientesFiltrados.postValue(resultado);
        contadorClientes.postValue(resultado.size());
    }
    // ── Actualizaciones locales ───────────────────────────────────────────────

    public void agregarCliente(Cliente cliente) {
        List<Cliente> lista = clientes.getValue();
        if (lista == null) lista = new ArrayList<>();
        lista.add(cliente);
        clientes.setValue(lista);
        aplicarFiltros();
    }

    public void actualizarCliente(Cliente clienteActualizado) {
        List<Cliente> lista = clientes.getValue();
        if (lista == null) return;
        List<Cliente> nueva = new ArrayList<>(lista);
        for (int i = 0; i < nueva.size(); i++) {
            if (nueva.get(i).getId() == clienteActualizado.getId()) {
                nueva.set(i, clienteActualizado);
                break;
            }
        }
        clientes.postValue(nueva);
        List<Cliente> resultado = filtrarLista(nueva);
        clientesFiltrados.postValue(resultado);
        contadorClientes.postValue(resultado.size());
    }

    public void eliminarCliente(int clienteId) {
        List<Cliente> lista = clientes.getValue();
        if (lista == null) return;
        List<Cliente> nueva = new ArrayList<>();
        for (Cliente c : lista) {
            if (c.getId() != clienteId) nueva.add(c);
        }
        clientes.postValue(nueva);
        List<Cliente> resultado = filtrarLista(nueva);
        clientesFiltrados.postValue(resultado);
        contadorClientes.postValue(resultado.size());
    }

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

    // Método puro — no toca LiveData, solo filtra
    private List<Cliente> filtrarLista(List<Cliente> listaOriginal) {
        List<Cliente> resultado = new ArrayList<>();
        for (Cliente c : listaOriginal) {
            if (!filtroGrupo.equals("TODOS")) {
                if (c.getSector() == null || !c.getSector().getNombre().equalsIgnoreCase(filtroGrupo))
                    continue;
            }
            switch (filtroEstado) {
                case ESTADO_PENDIENTES:
                    if (c.haPagadoHoy() || c.isHaPagadoTodo()) continue;
                    break;
                case ESTADO_MOROSOS:
                    if (!c.isEsMoroso()) continue;
                    break;
            }
            if (!filtroTexto.isEmpty()) {
                String q      = filtroTexto.toLowerCase();
                String nombre = c.getNombre()     != null ? c.getNombre().toLowerCase()     : "";
                String ref    = c.getReferencia() != null ? c.getReferencia().toLowerCase() : "";
                String tel    = c.getTelefono()   != null ? c.getTelefono()                 : "";
                if (!nombre.contains(q) && !ref.contains(q) && !tel.contains(q)) continue;
            }
            resultado.add(c);
        }
        return resultado;
    }

    public boolean hayClientesCargados() {
        List<Cliente> lista = clientes.getValue();
        return lista != null && !lista.isEmpty();
    }

    public void invalidarYRecargarSectores(Context context) {
        // setValue síncrono — garantiza que getValue() devuelve null inmediatamente
        sectores.setValue(null);
        // Ahora cargarSectores() ve null y hace el request
        cargarSectores(context);
    }

    public void actualizarUltimoPago(int clienteId, Pago ultimoPago) {
        List<Cliente> lista = clientes.getValue();
        if (lista == null) return;
        List<Cliente> nueva = new ArrayList<>(lista);
        for (Cliente c : nueva) {
            if (c.getId() == clienteId) {
                c.setUltimoPago(ultimoPago);
                break;
            }
        }
        clientes.postValue(nueva);
        List<Cliente> resultado = filtrarLista(nueva);
        clientesFiltrados.postValue(resultado);
        contadorClientes.postValue(resultado.size());
    }
}