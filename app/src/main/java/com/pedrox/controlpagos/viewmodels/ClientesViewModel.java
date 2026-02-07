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
import com.jimmyworks.easyhttp.exception.HttpException;
import com.jimmyworks.easyhttp.listener.StringResponseListener;
import com.pedrox.controlpagos.Constants;
import com.pedrox.controlpagos.models.Cliente;
import com.pedrox.controlpagos.responses.RespuestaLista;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Headers;

public class ClientesViewModel extends ViewModel {
    private static final String TAG = "ClientesViewModel";

    private final MutableLiveData<List<Cliente>> clientes = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Cliente>> clientesFiltrados = new MutableLiveData<>(new ArrayList<>());
    private String filtroGrupo = "TODOS";
    private String filtroTexto = "";
    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);
    public LiveData<Boolean> isCargando() { return cargando; }

    public LiveData<List<Cliente>> getClientesFiltrados() {
        return clientesFiltrados;
    }

    public void cargarClientes(Context context) {
        cargando.setValue(true);
        EasyHttp.get(context, Constants.API_BASE_URL + "clientes")
                .build()
                .getAsString(new StringResponseListener() {
                    @Override
                    public void onSuccess(@NonNull Headers headers, @NonNull String body) {
                        try {
                            Type tipoLista = new TypeToken<RespuestaLista<Cliente>>() {}.getType();
                            RespuestaLista<Cliente> respuesta = new Gson().fromJson(body, tipoLista);
                            if (respuesta.isError()) {
                                Log.e(TAG, "Error: " + respuesta.getMensaje());
                            } else {
                                Log.d(TAG, "Clientes recibidos: " + respuesta.getData());
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

    public String getFiltroGrupo() {
        return this.filtroGrupo;
    }

    public void setFiltroGrupo(String grupo) {
        this.filtroGrupo = grupo;
        aplicarFiltros();
    }

    public void setFiltroTexto(String texto) {
        this.filtroTexto = texto;
        aplicarFiltros();
    }

    private void aplicarFiltros() {
        List<Cliente> listaOriginal = clientes.getValue();
        if (listaOriginal == null) {
            return;
        }

        List<Cliente> filtrados = new ArrayList<>();

        for (Cliente cliente : listaOriginal) {
            boolean coincideGrupo = filtroGrupo.equals("TODOS") ||
                    (cliente.getGrupo() != null && cliente.getGrupo().equalsIgnoreCase(filtroGrupo));

            boolean coincideTexto = filtroTexto.isEmpty() ||
                    cliente.getNombre().toLowerCase().contains(filtroTexto.toLowerCase()) ||
                    cliente.getReferencia().toLowerCase().contains(filtroTexto.toLowerCase());

            if (coincideGrupo && coincideTexto) {
                filtrados.add(cliente);
            }
        }
        clientesFiltrados.setValue(filtrados);
    }

    public void agregarCliente(Cliente cliente) {
        List<Cliente> listaActual = clientes.getValue();
        if (listaActual == null) listaActual = new ArrayList<>();
        listaActual.add(cliente);

        clientes.setValue(listaActual);
        aplicarFiltros(); // Para que aparezca respetando los filtros activos
    }
}