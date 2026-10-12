package com.pedrox.controlpagos;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jimmyworks.easyhttp.EasyHttp;
import com.jimmyworks.easyhttp.builder.BodyRequestBuilder;
import com.jimmyworks.easyhttp.builder.RequestBuilder;
import com.jimmyworks.easyhttp.exception.HttpException;
import com.jimmyworks.easyhttp.listener.StringResponseListener;
import com.pedrox.controlpagos.adapters.ClienteAdapter;
import com.pedrox.controlpagos.models.Cliente;
import com.pedrox.controlpagos.models.Sector;
import com.pedrox.controlpagos.responses.RespuestaLista;
import com.pedrox.controlpagos.viewmodels.ClientesViewModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Headers;

public class ClientesArchivadosFragment extends Fragment
        implements ClienteAdapter.OnClienteActionListener {

    private static final String TAG = "ClientesArchivadosFragment";

    private ClientesViewModel         viewModel;
    private ClienteAdapter            adapter;
    private RecyclerView              rvClientes;
    private MaterialButtonToggleGroup toggleSectores;
    private HorizontalScrollView      scrollSectores;
    private EditText                  etBuscar;
    private Chip                      chipContador;

    // Lista completa sin filtrar — filtros se aplican localmente
    private final List<Cliente> listaCompleta = new ArrayList<>();
    private String filtroSector = "TODOS";
    private String filtroTexto  = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_clientes_archivados, container, false);

        // ── Bind vistas ───────────────────────────────────────────────────────
        toggleSectores = view.findViewById(R.id.toggleSectoresArchivados);
        scrollSectores = view.findViewById(R.id.scrollSectoresArchivados);
        etBuscar       = view.findViewById(R.id.etBuscarArchivado);
        rvClientes     = view.findViewById(R.id.rvClientesArchivados);
        chipContador   = view.findViewById(R.id.chipContadorArchivados);

        // ── RecyclerView con modo ARCHIVADOS ──────────────────────────────────
        rvClientes.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ClienteAdapter(new ArrayList<>(), ClienteAdapter.Modo.ARCHIVADOS);
        adapter.setOnClienteActionListener(this);
        rvClientes.setAdapter(adapter);

        // ── ViewModel compartido con ClientesFragment ─────────────────────────
        // Los sectores ya están en cache — sin request extra
        viewModel = new ViewModelProvider(requireActivity())
                .get(ClientesViewModel.class);

        // ── Observer de sectores — reusar cache ───────────────────────────────
        viewModel.getSectores().observe(getViewLifecycleOwner(), sectores -> {
            if (sectores == null) return;
            toggleSectores.removeAllViews();
            for (Sector s : sectores) {
                ContextThemeWrapper ctx = new ContextThemeWrapper(requireContext(),
                        com.google.android.material.R.style
                                .Widget_MaterialComponents_Button_OutlinedButton);
                MaterialButton btn = new MaterialButton(ctx, null, 0);
                btn.setText(s.getNombre());
                btn.setId(View.generateViewId());
                btn.setCheckable(true);
                btn.setBackgroundTintList(getResources().getColorStateList(
                        R.color.colorToggle, requireContext().getTheme()));
                toggleSectores.addView(btn);
            }
            // Seleccionar TODOS por defecto
            if (toggleSectores.getChildCount() > 0) {
                MaterialButton primero =
                        (MaterialButton) toggleSectores.getChildAt(0);
                toggleSectores.check(primero.getId());
            }
        });

        // ── Listener toggle sectores ──────────────────────────────────────────
        toggleSectores.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            MaterialButton btn = group.findViewById(checkedId);
            if (btn != null) {
                filtroSector = btn.getText().toString();
                // Centrar el botón seleccionado
                scrollSectores.post(() -> {
                    int centrado = btn.getLeft()
                            - (scrollSectores.getWidth() / 2)
                            + (btn.getWidth() / 2);
                    scrollSectores.scrollTo(Math.max(0, centrado), 0);
                });
                aplicarFiltros();
            }
        });

        // ── Listener búsqueda ─────────────────────────────────────────────────
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) {
                filtroTexto = s.toString();
                aplicarFiltros();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // ── Cargar archivados del servidor ────────────────────────────────────
        cargarArchivados();

        return view;
    }

    // ── Carga de datos ────────────────────────────────────────────────────────

    private void cargarArchivados() {
        Context ctx = requireContext();
        RequestBuilder rb = EasyHttp.get(ctx,
                Constants.API_BASE_URL + "clientes/archivados");
        rb.getOkHttpClientBuilder().addInterceptor(HttpClient.interceptor(ctx));
        rb.build().getAsString(new StringResponseListener() {
            @Override
            public void onSuccess(@NonNull Headers headers, @NonNull String body) {
                try {
                    Type tipo = new TypeToken<RespuestaLista<Cliente>>() {}.getType();
                    RespuestaLista<Cliente> resp = new Gson().fromJson(body, tipo);
                    if (!resp.isError()) {
                        listaCompleta.clear();
                        if (resp.getData() != null) listaCompleta.addAll(resp.getData());
                        requireActivity().runOnUiThread(this::aplicarFiltrosUI);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parseando archivados", e);
                }
            }

            private void aplicarFiltrosUI() {
                aplicarFiltros();
            }

            @Override
            public void onError(@NonNull HttpException e) {
                Log.e(TAG, "Error HTTP: " + e.getMessage());
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(ctx,
                                "Error cargando clientes archivados",
                                Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    // ── Filtrado local ────────────────────────────────────────────────────────

    private void aplicarFiltros() {
        List<Cliente> resultado = new ArrayList<>();
        for (Cliente c : listaCompleta) {

            // Filtro sector
            if (!filtroSector.equals("TODOS")) {
                String nombreSector = (c.getSector() != null)
                        ? c.getSector().getNombre() : "";
                if (!nombreSector.equalsIgnoreCase(filtroSector)) continue;
            }

            // Filtro texto — nombre, referencia, teléfono
            if (!filtroTexto.isEmpty()) {
                String q      = filtroTexto.toLowerCase();
                String nombre = c.getNombre()     != null ? c.getNombre().toLowerCase()     : "";
                String ref    = c.getReferencia() != null ? c.getReferencia().toLowerCase() : "";
                String tel    = c.getTelefono()   != null ? c.getTelefono()                 : "";
                if (!nombre.contains(q) && !ref.contains(q) && !tel.contains(q)) continue;
            }

            resultado.add(c);
        }

        adapter.updateClientes(resultado);
        int total = resultado.size();
        chipContador.setText(total + " archivado" + (total != 1 ? "s" : ""));
    }

    // ── Callbacks del adapter ─────────────────────────────────────────────────

    @Override
    public void onClienteClick(Cliente cliente) {
        // No navega en modo archivados — sin acción
    }

    @Override
    public void onRestaurarCliente(Cliente cliente) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Restaurar cliente")
                .setMessage("¿Restaurar a " + cliente.getNombre()
                        + "?\nVolverá a aparecer en la lista principal"
                        + " con todos sus pedidos y pagos.")
                .setPositiveButton("Restaurar", (d, w) -> ejecutarRestaurar(cliente))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void ejecutarRestaurar(Cliente cliente) {
        Context ctx = requireContext();
        JSONObject obj = new JSONObject();
        try {
            obj.put("id",        cliente.getId());
            obj.put("archivado", 0);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        BodyRequestBuilder rb = EasyHttp.post(ctx,
                Constants.API_BASE_URL + "cliente/archivar");
        rb.getOkHttpClientBuilder().addInterceptor(HttpClient.interceptor(ctx));
        rb.stringBody("application/json; charset=utf-8", obj.toString())
                .build().getAsString(new StringResponseListener() {
                    @Override
                    public void onSuccess(@NonNull Headers headers, @NonNull String body) {
                        try {
                            JSONObject resp = new JSONObject(body);
                            if (!resp.getBoolean("error")) {
                                // Quitar de la lista local
                                listaCompleta.remove(cliente);
                                // Agregar al ViewModel para que aparezca en ClientesFragment
                                // sin necesidad de recargar toda la lista
                                viewModel.agregarCliente(cliente);
                                requireActivity().runOnUiThread(() -> {
                                    aplicarFiltros();
                                    Toast.makeText(ctx,
                                            cliente.getNombre() + " restaurado correctamente",
                                            Toast.LENGTH_SHORT).show();
                                });
                            } else {
                                requireActivity().runOnUiThread(() ->
                                        Toast.makeText(ctx,
                                                resp.optString("mensaje", "Error al restaurar"),
                                                Toast.LENGTH_SHORT).show()
                                );
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parseando respuesta restaurar", e);
                        }
                    }

                    @Override
                    public void onError(@NonNull HttpException e) {
                        Log.e(TAG, "Error HTTP restaurar: " + e.getMessage());
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(ctx,
                                        "Error de red: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show()
                        );
                    }
                });
    }
}