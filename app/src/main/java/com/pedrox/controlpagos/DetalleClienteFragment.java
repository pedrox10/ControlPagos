package com.pedrox.controlpagos;

import android.content.Context;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jimmyworks.easyhttp.EasyHttp;
import com.jimmyworks.easyhttp.builder.BodyRequestBuilder;
import com.jimmyworks.easyhttp.builder.RequestBuilder;
import com.jimmyworks.easyhttp.exception.HttpException;
import com.jimmyworks.easyhttp.listener.JsonResponseListener;
import com.jimmyworks.easyhttp.listener.StringResponseListener;
import com.pedrox.controlpagos.adapters.PedidoAdapter;
import com.pedrox.controlpagos.models.Cliente;
import com.pedrox.controlpagos.models.Pago;
import com.pedrox.controlpagos.models.Pedido;
import com.pedrox.controlpagos.models.Sector;
import com.pedrox.controlpagos.responses.ClienteRespuesta;
import com.pedrox.controlpagos.responses.PedidoRespuesta;
import com.pedrox.controlpagos.responses.RespuestaLista;
import com.pedrox.controlpagos.viewmodels.ClientesViewModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Headers;

public class DetalleClienteFragment extends Fragment implements PedidoAdapter.OnPedidoClickListener {

    private static final String TAG = "DetalleClienteFragment";
    private static final String ARG_CLIENTE = "cliente";
    private Cliente cliente;

    private TextView tvNombreClienteDetalle;
    private TextView tvDireccionClienteDetalle;
    private TextView tvTelefonoClienteDetalle;
    private TextView tvGrupoClienteDetalle;
    private RecyclerView rvPedidos;
    private MaterialButton btnEditar;
    private MaterialButton btnEliminar;
    private MaterialButton btnAddPedido;
    private MaterialButton btnArchivar;
    private LinearLayout layoutSwitchesAdmin;
    // NUEVO
    private SwitchMaterial switchMoroso;
    private PedidoAdapter pedidoAdapter;
    private CircularProgressIndicator progressBar;
    private String rolUsuario;
    // Referencia al ViewModel para actualizar esMoroso sin recargar lista
    private ClientesViewModel clientesViewModel;
    InputFilter[] filtro;

    public DetalleClienteFragment() {}

    public static DetalleClienteFragment newInstance(Cliente cliente) {
        DetalleClienteFragment fragment = new DetalleClienteFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_CLIENTE, cliente);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            cliente = (Cliente) getArguments().getSerializable(ARG_CLIENTE);
        }
        filtro = new InputFilter[]{
                (source, start, end, dest, dstart, dend) -> {
                    String resultado = dest.subSequence(0, dstart)
                            + source.toString()
                            + dest.subSequence(dend, dest.length());
                    return resultado.matches("^\\d{0,4}(\\.\\d{0,1})?$") ? null : "";
                }
        };
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rolUsuario = AppSession.getUsuarioActual().getRol();
        View view = inflater.inflate(R.layout.fragment_detalle_cliente, container, false);

        tvNombreClienteDetalle    = view.findViewById(R.id.tvDetalleClienteNombre);
        tvDireccionClienteDetalle = view.findViewById(R.id.tvDetalleClienteDireccion);
        tvTelefonoClienteDetalle  = view.findViewById(R.id.tvDetalleClienteTelefono);
        tvGrupoClienteDetalle     = view.findViewById(R.id.tvDetalleClienteGrupo);
        btnEditar                 = view.findViewById(R.id.btnEditar);
        btnEliminar               = view.findViewById(R.id.btnEliminar);
        btnAddPedido              = view.findViewById(R.id.btnAddPedido);
        switchMoroso              = view.findViewById(R.id.switchMoroso); // NUEVO
        rvPedidos                 = view.findViewById(R.id.rvPedidos);
        progressBar               = view.findViewById(R.id.progressBarPedidos);
        btnArchivar = view.findViewById(R.id.btnArchivar);
        layoutSwitchesAdmin       = view.findViewById(R.id.layoutSwitchesAdmin);

        rvPedidos.setLayoutManager(new LinearLayoutManager(getContext()));
        pedidoAdapter = new PedidoAdapter(new ArrayList<>());
        pedidoAdapter.setOnPedidoClickListener(this);
        rvPedidos.setAdapter(pedidoAdapter);

        // ViewModel compartido con ClientesFragment para actualizar la lista
        clientesViewModel = new ViewModelProvider(requireActivity()).get(ClientesViewModel.class);
        getParentFragmentManager().setFragmentResultListener(
                "pagoResult",
                getViewLifecycleOwner(),
                (key, bundle) -> {
                    if (bundle.getBoolean("pagoModificado", false)) {
                        int clienteId = bundle.getInt("clienteId");
                        actualizarUltimoPagoCliente(clienteId);
                    }
                }
        );

        if (cliente != null) {
            tvNombreClienteDetalle.setText(cliente.getNombre());
            tvDireccionClienteDetalle.setText(cliente.getReferencia());
            tvTelefonoClienteDetalle.setText("Teléfono: " + cliente.getTelefono());
            tvGrupoClienteDetalle.setText("Sector: " + cliente.getSector().getNombre());

            if ("Admin".equals(rolUsuario)) {
                layoutSwitchesAdmin.setVisibility(View.VISIBLE);
                // Switch moroso — igual que antes
                switchMoroso.setOnCheckedChangeListener(null);
                switchMoroso.setChecked(cliente.isEsMoroso());
                switchMoroso.setOnCheckedChangeListener((b, isChecked) ->
                        marcarMoroso(cliente.getId(), isChecked));
                // Botón archivar
                btnArchivar.setOnClickListener(v -> confirmarArchivar());

            } else {
                layoutSwitchesAdmin.setVisibility(View.GONE);
            }

            btnEditar.setOnClickListener(v -> mostrarDialogEditarCliente(cliente));
            btnEliminar.setOnClickListener(v -> confirmarEliminarCliente(cliente));
            btnAddPedido.setOnClickListener(v -> mostrarDialogNuevoPedido(cliente));
            cargarPedidos();
        }

        if (!"Admin".equals(rolUsuario)) {
            btnAddPedido.setVisibility(View.GONE);
            btnEditar.setVisibility(View.GONE);
            btnEliminar.setVisibility(View.GONE);
        }

        return view;
    }

    // ── Toggle moroso ─────────────────────────────────────────────────────────

    /**
     * Llama al endpoint cliente/edit con el campo esMoroso actualizado.
     * Si el servidor responde OK, actualiza el ViewModel para reflejar el cambio
     * en la lista sin necesidad de recargar todos los clientes.
     */
    private void marcarMoroso(int clienteId, boolean esMoroso) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id",        clienteId);
            obj.put("nombre",    cliente.getNombre());
            obj.put("referencia", cliente.getReferencia() != null ? cliente.getReferencia() : "");
            obj.put("telefono",  cliente.getTelefono() != null ? cliente.getTelefono() : "");
            obj.put("sector_id", cliente.getSector() != null ? cliente.getSector().getId() : 0);
            obj.put("es_moroso", esMoroso ? 1 : 0);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        BodyRequestBuilder rb = EasyHttp.post(getContext(), Constants.API_BASE_URL + "cliente/edit");
        rb.getOkHttpClientBuilder().addInterceptor(HttpClient.interceptor(getContext()));
        rb.stringBody("application/json; charset=utf-8", obj.toString())
                .build()
                .getJsonAsObject(ClienteRespuesta.class, new JsonResponseListener<ClienteRespuesta>() {
                    @Override
                    public void onSuccess(@NonNull Headers headers, @NonNull ClienteRespuesta respuesta) {
                        if (!respuesta.isError()) {
                            cliente.setEsMoroso(esMoroso);
                            // Actualizar lista sin recargar del servidor
                            clientesViewModel.actualizarMoroso(clienteId, esMoroso);
                            String msg = esMoroso ? "Marcado como moroso" : "Quitado de morosos";
                            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                        } else {
                            // Revertir el switch si el servidor falló
                            switchMoroso.setOnCheckedChangeListener(null);
                            switchMoroso.setChecked(!esMoroso);
                            switchMoroso.setOnCheckedChangeListener((b, c) -> marcarMoroso(clienteId, c));
                            Toast.makeText(getContext(), respuesta.getMensaje(), Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onError(@NonNull HttpException e) {
                        // Revertir el switch si hubo error de red
                        switchMoroso.setOnCheckedChangeListener(null);
                        switchMoroso.setChecked(!esMoroso);
                        switchMoroso.setOnCheckedChangeListener((b, c) -> marcarMoroso(clienteId, c));
                        Toast.makeText(getContext(), "Error de red", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ── Cargar pedidos ────────────────────────────────────────────────────────
    private void cargarPedidos() {
        if (cliente == null) return;
        long tiempoInicio = System.currentTimeMillis();

        // ✅ post() garantiza que el layout ya terminó de dibujarse
        progressBar.post(() -> progressBar.setVisibility(View.VISIBLE));
        rvPedidos.post(() -> rvPedidos.setVisibility(View.GONE));

        RequestBuilder builder = EasyHttp.get(getContext(),
                Constants.API_BASE_URL + "cliente/" + cliente.getId() + "/pedidos");
        builder.getOkHttpClientBuilder().addInterceptor(HttpClient.interceptor(getContext()));
        builder.build().getAsString(new StringResponseListener() {
            @Override
            public void onSuccess(@NonNull Headers headers, @NonNull String body) {
                try {
                    Type tipo = new TypeToken<RespuestaLista<Pedido>>() {}.getType();
                    RespuestaLista<Pedido> respuesta = new Gson().fromJson(body, tipo);
                    if (!respuesta.isError()) {
                        List<Pedido> pedidos = respuesta.getData();
                        Log.d(TAG, "cargarPedidos → OK en "
                                + (System.currentTimeMillis() - tiempoInicio) + "ms");
                        requireActivity().runOnUiThread(() -> {
                            pedidoAdapter.updatePedidos(pedidos);
                            progressBar.setVisibility(View.GONE);
                            rvPedidos.setVisibility(View.VISIBLE);
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parseando JSON", e);
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Error al leer pedidos",
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
            @Override
            public void onError(@NonNull HttpException e) {
                Log.e(TAG, "Error: " + e.getMessage());
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error de red: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    @Override
    public void onPedidoClick(Pedido pedido) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("pedido", pedido);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_detalleClienteFragment_to_detallePedidoFragment, bundle);
    }

    // ── Dialog editar cliente ─────────────────────────────────────────────────

    public void mostrarDialogEditarCliente(Cliente cliente) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_agregar_cliente, null);
        EditText etNombre     = view.findViewById(R.id.etNombre);
        EditText etReferencia = view.findViewById(R.id.etReferencia);
        EditText etTelefono   = view.findViewById(R.id.etTelefono);
        Spinner  spGrupo      = view.findViewById(R.id.spGrupo);

        // ✅ Usar cache del ViewModel — sin request extra
        List<Sector> sectores = clientesViewModel.getSectores().getValue();
        if (sectores != null) {
            // Filtrar "TODOS" — no es asignable a un cliente
            List<Sector> sinTodos = new ArrayList<>();
            for (Sector s : sectores) {
                if (s.getId() != -1) sinTodos.add(s);
            }
            ArrayAdapter<Sector> adapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    sinTodos);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spGrupo.setAdapter(adapter);

            // Preseleccionar el sector actual del cliente por ID
            if (cliente.getSector() != null) {
                for (int i = 0; i < sinTodos.size(); i++) {
                    if (sinTodos.get(i).getId() == cliente.getSector().getId()) {
                        spGrupo.setSelection(i);
                        break;
                    }
                }
            }
        }

        etNombre.setText(cliente.getNombre());
        etReferencia.setText(cliente.getReferencia());
        etTelefono.setText(cliente.getTelefono() != null ? cliente.getTelefono() : "");

        builder.setView(view)
                .setTitle("Editar Cliente")
                .setPositiveButton("Actualizar", (dialog, id) -> {
                    String nombre = etNombre.getText().toString().trim();
                    if (nombre.isEmpty()) {
                        Toast.makeText(getContext(), "El nombre no puede estar vacío",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Sector seleccionado en el Spinner
                    Sector sectorSeleccionado = (Sector) spGrupo.getSelectedItem();
                    JSONObject obj = new JSONObject();
                    try {
                        obj.put("id",         cliente.getId());
                        obj.put("nombre",     nombre);
                        obj.put("referencia", etReferencia.getText().toString().trim());
                        obj.put("telefono",   etTelefono.getText().toString().trim());
                        // ✅ sector_id en lugar de grupo
                        obj.put("sector_id",  sectorSeleccionado != null ? sectorSeleccionado.getId() : 0);
                        obj.put("es_moroso",  cliente.isEsMoroso() ? 1 : 0);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Context ctx = requireContext();
                    BodyRequestBuilder rb = EasyHttp.post(ctx,
                            Constants.API_BASE_URL + "cliente/edit");
                    rb.getOkHttpClientBuilder()
                            .addInterceptor(HttpClient.interceptor(ctx));
                    rb.stringBody("application/json; charset=utf-8", obj.toString())
                            .build()
                            .getJsonAsObject(ClienteRespuesta.class,
                                    new JsonResponseListener<ClienteRespuesta>() {
                                        @Override
                                        public void onSuccess(@NonNull Headers headers,
                                                              @NonNull ClienteRespuesta respuesta) {
                                            if (!respuesta.isError()) {
                                                requireActivity().runOnUiThread(() -> {
                                                    setCliente(respuesta.getData());
                                                    clientesViewModel.actualizarCliente(respuesta.getData());
                                                    tvNombreClienteDetalle.setText(
                                                            respuesta.getData().getNombre());
                                                    tvDireccionClienteDetalle.setText(
                                                            respuesta.getData().getReferencia());
                                                    tvTelefonoClienteDetalle.setText(
                                                            "Teléfono: " + respuesta.getData().getTelefono());
                                                    // ✅ null-safe para sector
                                                    String nombreSector = respuesta.getData().getSector() != null
                                                            ? respuesta.getData().getSector().getNombre()
                                                            : "Sin sector";
                                                    tvGrupoClienteDetalle.setText("Grupo: " + nombreSector);
                                                    Toast.makeText(ctx, "Cliente actualizado",
                                                            Toast.LENGTH_SHORT).show();
                                                });
                                            } else {
                                                requireActivity().runOnUiThread(() ->
                                                        Toast.makeText(ctx, respuesta.getMensaje(),
                                                                Toast.LENGTH_SHORT).show()
                                                );
                                            }
                                        }
                                        @Override
                                        public void onError(@NonNull HttpException e) {
                                            requireActivity().runOnUiThread(() ->
                                                    Toast.makeText(ctx, "Error: " + e.getMessage(),
                                                            Toast.LENGTH_SHORT).show()
                                            );
                                        }
                                    });
                })
                .setNegativeButton("Cancelar", (dialog, id) -> dialog.dismiss());
        builder.create().show();
    }

    // ── Dialog eliminar cliente ───────────────────────────────────────────────

    private void confirmarEliminarCliente(Cliente cliente) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar Cliente")
                .setMessage("¿Seguro que deseas eliminar a " + cliente.getNombre()
                        + "? Se borrarán también todos sus pedidos y pagos.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    JSONObject obj = new JSONObject();
                    try { obj.put("id", cliente.getId()); } catch (JSONException e) { e.printStackTrace(); }
                    BodyRequestBuilder rb = EasyHttp.post(getContext(), Constants.API_BASE_URL + "cliente/delete");
                    rb.getOkHttpClientBuilder().addInterceptor(HttpClient.interceptor(getContext()));
                    rb.stringBody("application/json; charset=utf-8", obj.toString())
                            .build()
                            .getJsonAsObject(ClienteRespuesta.class, new JsonResponseListener<ClienteRespuesta>() {
                                @Override
                                public void onSuccess(@NonNull Headers headers, @NonNull ClienteRespuesta respuesta) {
                                    if (!respuesta.isError()) {
                                        clientesViewModel.eliminarCliente(cliente.getId());
                                        Toast.makeText(getContext(), "Cliente eliminado", Toast.LENGTH_SHORT).show();
                                        Navigation.findNavController(requireView()).popBackStack();
                                    } else {
                                        Toast.makeText(getContext(), respuesta.getMensaje(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                                @Override
                                public void onError(@NonNull HttpException e) {
                                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // ── Dialog nuevo pedido ───────────────────────────────────────────────────

    private void mostrarDialogNuevoPedido(Cliente cliente) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_nuevo_pedido, null);
        DatePicker datePicker = view.findViewById(R.id.datePicker);
        EditText etCuotasDe   = view.findViewById(R.id.etCuotasDe);
        etCuotasDe.setFilters(filtro);

        builder.setView(view)
                .setTitle("Agregar Pedido")
                .setPositiveButton("Aceptar", (dialog, id) -> {
                    String fechaStr = String.format(Locale.getDefault(), "%02d/%02d/%04d",
                            datePicker.getDayOfMonth(), datePicker.getMonth() + 1, datePicker.getYear());
                    String cuotasStr = etCuotasDe.getText().toString().trim();
                    if (cuotasStr.isEmpty() || cuotasStr.equals(".")) cuotasStr = "0";
                    double cuotasDe = Double.parseDouble(cuotasStr);

                    JSONObject obj = new JSONObject();
                    try {
                        obj.put("fecha",      fechaStr);
                        obj.put("cliente_id", cliente.getId());
                        obj.put("cuotas_de",  cuotasDe);
                    } catch (JSONException e) { e.printStackTrace(); }

                    BodyRequestBuilder rb = EasyHttp.post(getContext(), Constants.API_BASE_URL + "pedido/add");
                    rb.getOkHttpClientBuilder().addInterceptor(HttpClient.interceptor(getContext()));
                    rb.stringBody("application/json; charset=utf-8", obj.toString())
                            .build()
                            .getJsonAsObject(PedidoRespuesta.class, new JsonResponseListener<PedidoRespuesta>() {
                                @Override
                                public void onSuccess(@NonNull Headers headers, @NonNull PedidoRespuesta respuesta) {
                                    if (!respuesta.isError()) {
                                        Pedido pedido = respuesta.getData();
                                        pedido.setCliente(cliente);
                                        Bundle bundle = new Bundle();
                                        bundle.putSerializable("pedido", pedido);
                                        Navigation.findNavController(requireView())
                                                .navigate(R.id.action_detalleClienteFragment_to_detallePedidoFragment, bundle);
                                    } else {
                                        Toast.makeText(getContext(), respuesta.getMensaje(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                                @Override
                                public void onError(@NonNull HttpException e) {
                                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancelar", (dialog, id) -> dialog.dismiss());
        builder.create().show();
    }

    private void actualizarUltimoPagoCliente(int clienteId) {
        RequestBuilder rb = EasyHttp.get(getContext(), Constants.API_BASE_URL + "cliente/" + clienteId + "/ultimopago");
        rb.getOkHttpClientBuilder().addInterceptor(HttpClient.interceptor(getContext()));
        rb.build().getAsString(new StringResponseListener() {
            @Override
            public void onSuccess(@NonNull Headers headers, @NonNull String body) {
                try {
                    JSONObject resp = new JSONObject(body);
                    // ✅ Parsear objeto Pago con Gson en lugar de getString
                    Pago ultimoPago = null;
                    if (!resp.isNull("ultimoPago")) {
                        ultimoPago = new Gson().fromJson(
                                resp.getJSONObject("ultimoPago").toString(),
                                Pago.class
                        );
                    }
                    clientesViewModel.actualizarUltimoPago(clienteId, ultimoPago);
                } catch (Exception e) {
                    Log.e(TAG, "Error actualizando ultimoPago", e);
                }
            }
            @Override
            public void onError(@NonNull HttpException e) {
                Log.e(TAG, "Error HTTP ultimoPago: " + e.getMessage());
            }
        });
    }

    private void confirmarArchivar() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Archivar cliente")
                .setMessage("¿Archivar a " + cliente.getNombre()
                        + "?\nNo aparecerá en la lista principal."
                        + " Sus pedidos y pagos se conservan.")
                .setPositiveButton("Archivar", (d, w) -> archivarCliente())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void archivarCliente() {
        Context ctx = requireContext();
        JSONObject obj = new JSONObject();
        try {
            obj.put("id",         cliente.getId());
            obj.put("nombre",     cliente.getNombre());
            obj.put("referencia", cliente.getReferencia() != null ? cliente.getReferencia() : "");
            obj.put("telefono",   cliente.getTelefono()   != null ? cliente.getTelefono()   : "");
            obj.put("sector_id",  cliente.getSector()     != null ? cliente.getSector().getId() : 0);
            obj.put("es_moroso",  cliente.isEsMoroso() ? 1 : 0);
            obj.put("archivado",  1); // ← archivar
        } catch (JSONException e) { e.printStackTrace(); }

        BodyRequestBuilder rb = EasyHttp.post(ctx,
                Constants.API_BASE_URL + "cliente/edit");
        rb.getOkHttpClientBuilder().addInterceptor(HttpClient.interceptor(ctx));
        rb.stringBody("application/json; charset=utf-8", obj.toString())
                .build().getJsonAsObject(ClienteRespuesta.class,
                        new JsonResponseListener<ClienteRespuesta>() {
                            @Override
                            public void onSuccess(@NonNull Headers headers,
                                                  @NonNull ClienteRespuesta respuesta) {
                                if (!respuesta.isError()) {
                                    // Quitar de la lista del ViewModel
                                    clientesViewModel.eliminarCliente(cliente.getId());
                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(ctx,
                                                cliente.getNombre() + " archivado",
                                                Toast.LENGTH_SHORT).show();
                                        Navigation.findNavController(requireView()).popBackStack();
                                    });
                                } else {
                                    requireActivity().runOnUiThread(() ->
                                            Toast.makeText(ctx,
                                                    respuesta.getMensaje(), Toast.LENGTH_SHORT).show()
                                    );
                                }
                            }
                            @Override
                            public void onError(@NonNull HttpException e) {
                                requireActivity().runOnUiThread(() ->
                                        Toast.makeText(ctx, "Error de red", Toast.LENGTH_SHORT).show()
                                );
                            }
                        });
    }
}
