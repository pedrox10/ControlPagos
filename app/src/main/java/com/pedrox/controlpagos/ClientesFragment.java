package com.pedrox.controlpagos;

import static androidx.constraintlayout.motion.utils.Oscillator.TAG;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.jimmyworks.easyhttp.EasyHttp;
import com.jimmyworks.easyhttp.exception.HttpException;
import com.jimmyworks.easyhttp.listener.JsonResponseListener;
import com.pedrox.controlpagos.adapters.ClienteAdapter;
import com.pedrox.controlpagos.models.Cliente;
import com.pedrox.controlpagos.models.Usuario;
import com.pedrox.controlpagos.responses.ClienteRespuesta;
import com.pedrox.controlpagos.responses.UsuarioRespuesta;
import com.pedrox.controlpagos.viewmodels.ClientesViewModel;
import org.json.JSONException;
import org.json.JSONObject;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import okhttp3.Headers;

public class ClientesFragment extends Fragment implements ClienteAdapter.OnClienteActionListener {

    private ClientesViewModel viewModel;
    private RecyclerView rvClientes;
    private ClienteAdapter clienteAdapter;
    private FloatingActionButton fabAddCliente;
    private MaterialButtonToggleGroup toggleButtonGroupZonas;
    private android.widget.EditText etSearchCliente;
    private CircularProgressIndicator progressBar;
    private ImageButton btnMenuAdmin;
    private ImageButton btnRefrescar;
    private String rolUsuario;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lista_clientes, container, false);
        fabAddCliente = view.findViewById(R.id.fabAddCliente);
        etSearchCliente = view.findViewById(R.id.etSearchCliente);
        toggleButtonGroupZonas = view.findViewById(R.id.toggleButtonGroupZonas);
        rvClientes = view.findViewById(R.id.rvClientes);
        rolUsuario = AppSession.getUsuarioActual().getRol();
        // RecyclerView + Adapter
        clienteAdapter = new ClienteAdapter(new ArrayList<>());
        rvClientes.setLayoutManager(new LinearLayoutManager(getContext()));
        rvClientes.setAdapter(clienteAdapter);
        clienteAdapter.setOnClienteActionListener(this);
        viewModel = new ViewModelProvider(this).get(ClientesViewModel.class);
        // Observa la lista filtrada
        viewModel.getClientesFiltrados().observe(getViewLifecycleOwner(), clientes -> {
            clienteAdapter.updateClientes(clientes);
        });
        progressBar = view.findViewById(R.id.progressBarClientes);
        // Botón agregar cliente
        fabAddCliente.setOnClickListener(v -> {
            mostrarDialogAgregarCliente();
        });
        // Configuración búsqueda
        etSearchCliente.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setFiltroTexto(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        // Configuración filtros grupo
        toggleButtonGroupZonas.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                Button checkedButton = group.findViewById(checkedId);
                if (checkedButton != null) {
                    viewModel.setFiltroGrupo(checkedButton.getText().toString());
                }
            }
        });
        viewModel.isCargando().observe(getViewLifecycleOwner(), cargando -> {
            if (cargando) {
                progressBar.setVisibility(View.VISIBLE);
                rvClientes.setVisibility(View.GONE);
            } else {
                progressBar.setVisibility(View.GONE);
                rvClientes.setVisibility(View.VISIBLE);
            }
        });
        viewModel.cargarClientes(getContext());
        toggleButtonGroupZonas.check(R.id.btnGroupTodos);
        btnMenuAdmin = view.findViewById(R.id.btnMenuAdmin);
        btnRefrescar = view.findViewById(R.id.btnRefrescar);

        // Si no es admin, ocultar el botón de menú
        if (!"Admin".equals(rolUsuario)) {
            btnMenuAdmin.setVisibility(View.GONE);
            fabAddCliente.setVisibility(View.GONE);
        }
        btnMenuAdmin.setOnClickListener(v -> mostrarMenuAdmin(v));
        btnRefrescar.setOnClickListener(v -> viewModel.cargarClientes(getContext()));
        return view;
    }

    @Override
    public void onClienteClick(Cliente cliente) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("cliente", cliente);
        Log.d(TAG, "Clientes: " + cliente.toString());
        Navigation.findNavController(getView()).navigate(R.id.action_clientesFragment_to_detalleClienteFragment, bundle);
    }

    private void mostrarDialogAgregarCliente() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View view = inflater.inflate(R.layout.dialog_agregar_cliente, null);
        EditText etNombre = view.findViewById(R.id.etNombre);
        EditText etReferencia = view.findViewById(R.id.etReferencia);
        EditText etTelefono = view.findViewById(R.id.etTelefono);
        Spinner spGrupo = view.findViewById(R.id.spGrupo);
        spGrupo.setSelection(0);
        builder.setView(view)
                .setTitle("Agregar Cliente")
                .setPositiveButton("Guardar", (dialog, id) -> {
                    String nombre = etNombre.getText().toString().trim();
                    if(!nombre.equals("")) {
                        String referencia = etReferencia.getText().toString().trim();
                        String telefono = etTelefono.getText().toString().trim();
                        String grupo = spGrupo.getSelectedItem().toString();
                        JSONObject obj = new JSONObject();
                        try {
                            obj.put("nombre", nombre);
                            obj.put("referencia", referencia);
                            obj.put("telefono", telefono);
                            obj.put("grupo", grupo);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        EasyHttp.post(getContext(), Constants.API_BASE_URL + "cliente/add")
                                .stringBody("application/json; charset=utf-8", obj.toString())
                                .build()
                                .getJsonAsObject(ClienteRespuesta.class, new JsonResponseListener<ClienteRespuesta>() {
                                    @Override
                                    public void onSuccess(@NonNull Headers headers, @NonNull ClienteRespuesta respuesta) {
                                        if (respuesta.isError()) {
                                            Log.d(TAG, respuesta.getMensaje());
                                        } else {
                                            Cliente clienteNuevo = respuesta.getData();
                                            viewModel.agregarCliente(clienteNuevo);
                                            Toast.makeText(getContext(), "usuario agregado", Toast.LENGTH_SHORT).show();
                                            Log.d(TAG, "onSuccess: " + clienteNuevo.toString());
                                        }
                                    }
                                    @Override
                                    public void onError(@NonNull HttpException e) {
                                        Log.d(TAG, e.getMessage());
                                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(getContext(), "¡El nombre no puede ser vacio!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", (dialog, id) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void mostrarMenuAdmin(View anchor) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), anchor);
        popupMenu.getMenuInflater().inflate(R.menu.menu_admin, popupMenu.getMenu());
        // Forzar que se muestren íconos
        try {
            Field mFieldPopup = popupMenu.getClass().getDeclaredField("mPopup");
            mFieldPopup.setAccessible(true);
            Object mPopup = mFieldPopup.get(popupMenu);
            mPopup.getClass()
                    .getDeclaredMethod("setForceShowIcon", boolean.class)
                    .invoke(mPopup, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_gestionar_productos) {
                Navigation.findNavController(getView()).navigate(R.id.productosFragment);
                return true;
            } else if (id == R.id.action_gestionar_usuarios) {
                Navigation.findNavController(getView()).navigate(R.id.usuariosFragment);
                return true;
            } else if (id == R.id.action_reportes) {
                MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker();
                builder.setTitleText("Selecciona un rango de fechas");
                final MaterialDatePicker<Pair<Long, Long>> picker = builder.build();
                picker.show(getParentFragmentManager(), picker.toString());
                picker.addOnPositiveButtonClickListener(selection -> {

                    LocalDate inicio = Instant.ofEpochMilli(selection.first)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate();

                    LocalDate fin = Instant.ofEpochMilli(selection.second)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate();

                    Bundle bundle = new Bundle();
                    bundle.putString("inicio", inicio.toString()); // yyyy-MM-dd
                    bundle.putString("fin", fin.toString());       // yyyy-MM-dd

                    Navigation.findNavController(requireView())
                            .navigate(R.id.reportePagosFragment, bundle);
                });
                return true;
            }
            return false;
        });
        popupMenu.show();
    }
}
