package com.pedrox.controlpagos;

import static androidx.constraintlayout.motion.utils.Oscillator.TAG;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.navigation.Navigation;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jimmyworks.easyhttp.EasyHttp;
import com.jimmyworks.easyhttp.exception.HttpException;
import com.jimmyworks.easyhttp.listener.JsonResponseListener;
import com.jimmyworks.easyhttp.listener.StringResponseListener;
import com.pedrox.controlpagos.adapters.PedidoAdapter;
import com.pedrox.controlpagos.models.Cliente;
import com.pedrox.controlpagos.models.Pedido;
import com.pedrox.controlpagos.responses.ClienteRespuesta;
import com.pedrox.controlpagos.responses.PedidoRespuesta;
import com.pedrox.controlpagos.responses.RespuestaLista;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Headers;

public class DetalleClienteFragment extends Fragment implements PedidoAdapter.OnPedidoClickListener {

    private static final String ARG_CLIENTE = "cliente";
    private Cliente cliente; // El cliente que se mostrará
    private TextView tvNombreClienteDetalle;
    private TextView tvDireccionClienteDetalle;
    private TextView tvTelefonoClienteDetalle;
    private TextView tvGrupoClienteDetalle;
    private RecyclerView rvPedidos;
    private MaterialButton btnEditar;
    private MaterialButton btnEliminar;
    private MaterialButton btnAddPedido;
    private PedidoAdapter pedidoAdapter;
    private CircularProgressIndicator progressBar;
    private String rolUsuario;

    public DetalleClienteFragment() {

    }
    /**
     * Usa este factory method para crear una nueva instancia de
     * este fragmento usando los parámetros proporcionados.
     *
     * @param cliente El objeto Cliente a mostrar.
     * @return Una nueva instancia de fragment DetalleClienteFragment.
     */
    public static DetalleClienteFragment newInstance(Cliente cliente) {
        DetalleClienteFragment fragment = new DetalleClienteFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_CLIENTE, cliente); // Cliente debe ser Serializable
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            cliente = (Cliente) getArguments().getSerializable(ARG_CLIENTE);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rolUsuario = AppSession.getUsuarioActual().getRol();
        View view = inflater.inflate(R.layout.fragment_detalle_cliente, container, false);
        tvNombreClienteDetalle = view.findViewById(R.id.tvDetalleClienteNombre);
        tvDireccionClienteDetalle = view.findViewById(R.id.tvDetalleClienteDireccion);
        tvTelefonoClienteDetalle = view.findViewById(R.id.tvDetalleClienteTelefono);
        tvGrupoClienteDetalle = view.findViewById(R.id.tvDetalleClienteGrupo);
        btnEditar = view.findViewById(R.id.btnEditar);
        btnEliminar = view.findViewById(R.id.btnEliminar);
        btnAddPedido = view.findViewById(R.id.btnAddPedido);
        rvPedidos = view.findViewById(R.id.rvPedidos);
        rvPedidos.setLayoutManager(new LinearLayoutManager(getContext()));
        pedidoAdapter = new PedidoAdapter(new ArrayList<>()); // Inicializa con una lista vacía
        pedidoAdapter.setOnPedidoClickListener(this);
        rvPedidos.setAdapter(pedidoAdapter);
        progressBar = view.findViewById(R.id.progressBarPedidos);
        // Mostrar datos del cliente y cargar pedidos
        if (cliente != null) {
            tvNombreClienteDetalle.setText(cliente.getNombre());
            tvDireccionClienteDetalle.setText(cliente.getReferencia());
            tvTelefonoClienteDetalle.setText("Teléfono: " + cliente.getTelefono());
            tvGrupoClienteDetalle.setText("Grupo: " + cliente.getGrupo());
            btnEditar.setOnClickListener(v -> {
                mostrarDialogEditarCliente(cliente);
            });
            btnEliminar.setOnClickListener(v -> {
                confirmarEliminarCliente(cliente);
            });
            btnAddPedido.setOnClickListener(v -> {
                mostrarDialogNuevoPedido(cliente);
            });
            cargarPedidos();
        }
        if (!"Admin".equals(rolUsuario)) {
            btnAddPedido.setVisibility(View.GONE);
            btnEditar.setVisibility(View.GONE);
            btnEliminar.setVisibility(View.GONE);
        }
        return view;
    }

    private void cargarPedidos() {
        progressBar.setVisibility(View.VISIBLE);
        rvPedidos.setVisibility(View.GONE);
        if (cliente == null) return;
            EasyHttp.get(getContext(), Constants.API_BASE_URL + "cliente/" + cliente.getId() + "/pedidos")
                .build()
                .getAsString(new StringResponseListener() {
                    @Override
                    public void onSuccess(@NonNull Headers headers, @NonNull String body) {
                        try {
                            Type tipoLista = new TypeToken<RespuestaLista<Pedido>>() {}.getType();
                            RespuestaLista<Pedido> respuesta = new Gson().fromJson(body, tipoLista);
                            if (respuesta.isError()) {
                                Log.e(TAG, "Error: " + respuesta.getMensaje());
                            } else {
                                List<Pedido> pedidos = respuesta.getData();
                                Log.d(TAG, pedidos.toString());
                                pedidoAdapter.updatePedidos(pedidos);
                            }
                            progressBar.setVisibility(View.GONE);
                            rvPedidos.setVisibility(View.VISIBLE);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parseando JSON", e);
                        }
                    }
                    @Override
                    public void onError(@NonNull HttpException e) {
                        Log.e(TAG, "Error al cargar pedidos: " + e.getMessage());
                    }
                });
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    @Override
    public void onPedidoClick(Pedido pedido) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("pedido", pedido); // 'cliente' es el nombre del argumento en nav_graph.xml
        Navigation.findNavController(getView()).navigate(R.id.action_detalleClienteFragment_to_detallePedidoFragment, bundle);
    }

    public void mostrarDialogEditarCliente(Cliente cliente) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View view = inflater.inflate(R.layout.dialog_agregar_cliente, null);
        EditText etNombre = view.findViewById(R.id.etNombre);
        EditText etReferencia = view.findViewById(R.id.etReferencia);
        EditText etTelefono = view.findViewById(R.id.etTelefono);
        Spinner spGrupo = view.findViewById(R.id.spGrupo);
        // Prellenar datos
        etNombre.setText(cliente.getNombre());
        etReferencia.setText(cliente.getReferencia());
        etTelefono.setText(cliente.getTelefono() != null ? cliente.getTelefono().toString() : "");
        // Seleccionar grupo correcto
        String[] grupos = getResources().getStringArray(R.array.grupos_array);
        for (int i = 0; i < grupos.length; i++) {
            if (grupos[i].equalsIgnoreCase(cliente.getGrupo())) {
                spGrupo.setSelection(i);
                break;
            }
        }
        builder.setView(view)
                .setTitle("Editar Cliente")
                .setPositiveButton("Actualizar", (dialog, id) -> {
                    String nombre = etNombre.getText().toString().trim();
                    if (!nombre.isEmpty()) {
                        String referencia = etReferencia.getText().toString().trim();
                        String telefono = etTelefono.getText().toString().trim();
                        String grupo = spGrupo.getSelectedItem().toString();

                        JSONObject obj = new JSONObject();
                        try {
                            obj.put("id", cliente.getId());
                            obj.put("nombre", nombre);
                            obj.put("referencia", referencia);
                            obj.put("telefono", telefono);
                            obj.put("grupo", grupo);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        EasyHttp.post(getContext(), Constants.API_BASE_URL + "cliente/edit")
                                .stringBody("application/json; charset=utf-8", obj.toString())
                                .build()
                                .getJsonAsObject(ClienteRespuesta.class, new JsonResponseListener<ClienteRespuesta>() {
                                    @Override
                                    public void onSuccess(@NonNull Headers headers, @NonNull ClienteRespuesta respuesta) {
                                        if (!respuesta.isError()) {
                                            Cliente clienteActualizado = respuesta.getData();
                                            setCliente(clienteActualizado);
                                            // Refrescar la UI
                                            tvNombreClienteDetalle.setText(clienteActualizado.getNombre());
                                            tvDireccionClienteDetalle.setText(clienteActualizado.getReferencia());
                                            tvTelefonoClienteDetalle.setText("Teléfono: " +
                                                    clienteActualizado.getTelefono() != null ? clienteActualizado.getTelefono().toString() : ""
                                            );
                                            tvGrupoClienteDetalle.setText("Grupo: " + clienteActualizado.getGrupo());
                                            Toast.makeText(getContext(), "Cliente actualizado", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(getContext(), respuesta.getMensaje(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                    @Override
                                    public void onError(@NonNull HttpException e) {
                                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(getContext(), "¡El nombre no puede estar vacío!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", (dialog, id) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void confirmarEliminarCliente(Cliente cliente) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar Cliente")
                .setMessage("¿Seguro que deseas eliminar a " + cliente.getNombre() + "?. Se borrarán también todos su pedidos y pagos realizados")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    JSONObject obj = new JSONObject();
                    try {
                        obj.put("id", cliente.getId());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    EasyHttp.post(getContext(), Constants.API_BASE_URL + "cliente/delete")
                            .stringBody("application/json; charset=utf-8", obj.toString())
                            .build()
                            .getJsonAsObject(ClienteRespuesta.class, new JsonResponseListener<ClienteRespuesta>() {
                                @Override
                                public void onSuccess(@NonNull Headers headers, @NonNull ClienteRespuesta respuesta) {
                                    if (!respuesta.isError()) {
                                        Toast.makeText(getContext(), "Cliente eliminado", Toast.LENGTH_SHORT).show();
                                        Navigation.findNavController(requireView()).popBackStack(); // volver
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

    private void mostrarDialogNuevoPedido(Cliente cliente) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View view = inflater.inflate(R.layout.dialog_nuevo_pedido, null);

        DatePicker datePicker = view.findViewById(R.id.datePicker);

        builder.setView(view)
                .setTitle("Agregar Pedido")
                .setPositiveButton("Aceptar", (dialog, id) -> {
                    int day = datePicker.getDayOfMonth();
                    int month = datePicker.getMonth() + 1; // aquí sumas 1 porque DatePicker da base 0
                    int year = datePicker.getYear();
                    String fechaStr = String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month, year);
                    JSONObject obj = new JSONObject();
                    try {
                        obj.put("fecha", fechaStr);
                        obj.put("cliente_id", cliente.getId());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // Crear pedido en backend
                    EasyHttp.post(getContext(), Constants.API_BASE_URL + "pedido/add")
                            .stringBody("application/json; charset=utf-8", obj.toString())
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
}
