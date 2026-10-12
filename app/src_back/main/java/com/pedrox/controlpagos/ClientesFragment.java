package com.pedrox.controlpagos;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.jimmyworks.easyhttp.EasyHttp;
import com.jimmyworks.easyhttp.builder.BodyRequestBuilder;
import com.jimmyworks.easyhttp.exception.HttpException;
import com.jimmyworks.easyhttp.listener.JsonResponseListener;
import com.pedrox.controlpagos.adapters.ClienteAdapter;
import com.pedrox.controlpagos.models.Cliente;
import com.pedrox.controlpagos.responses.ClienteRespuesta;
import com.pedrox.controlpagos.viewmodels.ClientesViewModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;

import okhttp3.Headers;

public class ClientesFragment extends Fragment implements ClienteAdapter.OnClienteActionListener {

    private static final String TAG = "ClientesFragment";

    private ClientesViewModel            viewModel;
    private RecyclerView                 rvClientes;
    private ClienteAdapter               clienteAdapter;
    private FloatingActionButton         fabAddCliente;
    private MaterialButtonToggleGroup    toggleButtonGroupZonas;
    private ChipGroup                    chipGroupEstado;
    private EditText                     etSearchCliente;
    private CircularProgressIndicator    progressBar;
    private ImageButton                  btnMenuAdmin;
    private ImageButton                  btnRefrescar;
    private String                       rolUsuario;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lista_clientes, container, false);

        // ── Referencias a vistas ──────────────────────────────────────────────
        fabAddCliente          = view.findViewById(R.id.fabAddCliente);
        etSearchCliente        = view.findViewById(R.id.etSearchCliente);
        toggleButtonGroupZonas = view.findViewById(R.id.toggleButtonGroupZonas);
        chipGroupEstado        = view.findViewById(R.id.chipGroupEstado);
        rvClientes             = view.findViewById(R.id.rvClientes);
        progressBar            = view.findViewById(R.id.progressBarClientes);
        btnMenuAdmin           = view.findViewById(R.id.btnMenuAdmin);
        btnRefrescar           = view.findViewById(R.id.btnRefrescar);

        rolUsuario = AppSession.getUsuarioActual().getRol();

        // ── RecyclerView ──────────────────────────────────────────────────────
        clienteAdapter = new ClienteAdapter(new ArrayList<>());
        rvClientes.setLayoutManager(new LinearLayoutManager(getContext()));
        rvClientes.setAdapter(clienteAdapter);
        clienteAdapter.setOnClienteActionListener(this);

        // ── ViewModel ─────────────────────────────────────────────────────────
        // ViewModelProvider con requireActivity() para que sobreviva la navegación
        viewModel = new ViewModelProvider(requireActivity()).get(ClientesViewModel.class);

        viewModel.getClientesFiltrados().observe(getViewLifecycleOwner(),
                clientes -> clienteAdapter.updateClientes(clientes));

        viewModel.isCargando().observe(getViewLifecycleOwner(), cargando -> {
            progressBar.setVisibility(cargando ? View.VISIBLE : View.GONE);
            rvClientes.setVisibility(cargando ? View.GONE : View.VISIBLE);
        });

        // ── Cargar botones de sector dinámicos ────────────────────────────────
        cargarBotonesGrupo();

        // ── Restaurar estado visual de filtros al volver del detalle ──────────
        restaurarEstadoVisual();

        // ── Solo cargar del servidor si la lista está vacía ───────────────────
        // Al volver del detalle, el ViewModel ya tiene los datos — no hace request
        if (viewModel.getClientesFiltrados().getValue() == null ||
                viewModel.getClientesFiltrados().getValue().isEmpty()) {
            viewModel.cargarClientes(requireContext());
        }

        // ── Listeners ─────────────────────────────────────────────────────────

        // Búsqueda por texto
        etSearchCliente.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setFiltroTexto(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Filtro nivel 1 — Sector
        // Al cambiar sector, el ViewModel resetea estado y texto internamente.
        // El Fragment limpia visualmente los chips y el SearchView.
        toggleButtonGroupZonas.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                Button btn = group.findViewById(checkedId);
                if (btn != null) {
                    viewModel.setFiltroGrupo(btn.getText().toString());
                    // Reset visual — chips de estado vuelven a "Todos"
                    chipGroupEstado.check(R.id.chipEstadoTodos);
                    // Reset visual — SearchView
                    etSearchCliente.setText("");
                }
            }
        });

        // Filtro nivel 2 — Estado
        chipGroupEstado.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipEstadoPendientes) {
                viewModel.setFiltroEstado(ClientesViewModel.ESTADO_PENDIENTES);
            } else if (id == R.id.chipEstadoMorosos) {
                viewModel.setFiltroEstado(ClientesViewModel.ESTADO_MOROSOS);
            } else {
                viewModel.setFiltroEstado(ClientesViewModel.ESTADO_TODOS);
            }
        });

        // FAB agregar cliente
        fabAddCliente.setOnClickListener(v -> mostrarDialogAgregarCliente());

        // Botones admin
        if (!"Admin".equals(rolUsuario)) {
            btnMenuAdmin.setVisibility(View.GONE);
            fabAddCliente.setVisibility(View.GONE);
        }
        btnMenuAdmin.setOnClickListener(this::mostrarMenuAdmin);
        btnRefrescar.setOnClickListener(v -> viewModel.cargarClientes(requireContext()));

        return view;
    }

    /**
     * Restaura el estado visual de los chips y el SearchView
     * según lo que tiene el ViewModel — importante al volver del detalle.
     */
    private void restaurarEstadoVisual() {
        // Restaurar chip de estado
        String estado = viewModel.getFiltroEstado();
        if (ClientesViewModel.ESTADO_PENDIENTES.equals(estado)) {
            chipGroupEstado.check(R.id.chipEstadoPendientes);
        } else if (ClientesViewModel.ESTADO_MOROSOS.equals(estado)) {
            chipGroupEstado.check(R.id.chipEstadoMorosos);
        } else {
            chipGroupEstado.check(R.id.chipEstadoTodos);
        }

        // Restaurar texto de búsqueda
        String texto = viewModel.getFiltroTexto();
        if (!texto.isEmpty() && !etSearchCliente.getText().toString().equals(texto)) {
            etSearchCliente.setText(texto);
            etSearchCliente.setSelection(texto.length());
        }

        // Restaurar sector activo — se hace después de crear los botones en cargarBotonesGrupo()
        toggleButtonGroupZonas.post(() -> {
            String grupoActivo = viewModel.getFiltroGrupo();
            for (int i = 0; i < toggleButtonGroupZonas.getChildCount(); i++) {
                View v = toggleButtonGroupZonas.getChildAt(i);
                if (v instanceof MaterialButton) {
                    MaterialButton btn = (MaterialButton) v;
                    if (btn.getText().toString().equals(grupoActivo)) {
                        toggleButtonGroupZonas.check(btn.getId());
                        break;
                    }
                }
            }
        });
    }

    @Override
    public void onClienteClick(Cliente cliente) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("cliente", cliente);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_clientesFragment_to_detalleClienteFragment, bundle);
    }

    // ── Dialog agregar cliente ────────────────────────────────────────────────

    private void mostrarDialogAgregarCliente() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_agregar_cliente, null);
        EditText etNombre    = view.findViewById(R.id.etNombre);
        EditText etReferencia = view.findViewById(R.id.etReferencia);
        EditText etTelefono   = view.findViewById(R.id.etTelefono);
        Spinner  spGrupo      = view.findViewById(R.id.spGrupo);
        spGrupo.setSelection(0);

        builder.setView(view)
                .setTitle("Agregar Cliente")
                .setPositiveButton("Guardar", (dialog, id) -> {
                    String nombre = etNombre.getText().toString().trim();
                    if (nombre.isEmpty()) {
                        Toast.makeText(getContext(), "¡El nombre no puede ser vacío!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    JSONObject obj = new JSONObject();
                    try {
                        obj.put("nombre",     nombre);
                        obj.put("referencia", etReferencia.getText().toString().trim());
                        obj.put("telefono",   etTelefono.getText().toString().trim());
                        obj.put("grupo",      spGrupo.getSelectedItem().toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // ← interceptor agregado
                    BodyRequestBuilder rb = EasyHttp.post(getContext(), Constants.API_BASE_URL + "cliente/add");
                    rb.getOkHttpClientBuilder().addInterceptor(HttpClient.interceptor(getContext()));
                    rb.stringBody("application/json; charset=utf-8", obj.toString())
                            .build()
                            .getJsonAsObject(ClienteRespuesta.class, new JsonResponseListener<ClienteRespuesta>() {
                                @Override
                                public void onSuccess(@NonNull Headers headers, @NonNull ClienteRespuesta respuesta) {
                                    if (respuesta.isError()) {
                                        Log.e(TAG, respuesta.getMensaje());
                                    } else {
                                        viewModel.agregarCliente(respuesta.getData());
                                        Toast.makeText(getContext(), "Cliente agregado", Toast.LENGTH_SHORT).show();
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

    // ── Menú admin ────────────────────────────────────────────────────────────

    private void mostrarMenuAdmin(View anchor) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), anchor);
        popupMenu.getMenuInflater().inflate(R.menu.menu_admin, popupMenu.getMenu());
        try {
            Field f = popupMenu.getClass().getDeclaredField("mPopup");
            f.setAccessible(true);
            Object mPopup = f.get(popupMenu);
            mPopup.getClass().getDeclaredMethod("setForceShowIcon", boolean.class).invoke(mPopup, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_gestionar_productos) {
                Navigation.findNavController(requireView()).navigate(R.id.productosFragment);
                return true;
            } else if (id == R.id.action_gestionar_usuarios) {
                Navigation.findNavController(requireView()).navigate(R.id.usuariosFragment);
                return true;
            } else if (id == R.id.action_cartera_viva) {
                Navigation.findNavController(requireView()).navigate(R.id.carteraVivaFragment);
                return true;
            } else if (id == R.id.action_gestionar_sectores) {
                Navigation.findNavController(requireView()).navigate(R.id.sectoresFragment);
                return true;
            } else if (id == R.id.action_reportes) {
                MaterialDatePicker.Builder<Pair<Long, Long>> b = MaterialDatePicker.Builder.dateRangePicker();
                b.setTitleText("Selecciona un rango de fechas");
                MaterialDatePicker<Pair<Long, Long>> picker = b.build();
                picker.show(getParentFragmentManager(), picker.toString());
                picker.addOnPositiveButtonClickListener(selection -> {
                    LocalDate inicio = Instant.ofEpochMilli(selection.first).atZone(ZoneOffset.UTC).toLocalDate();
                    LocalDate fin    = Instant.ofEpochMilli(selection.second).atZone(ZoneOffset.UTC).toLocalDate();
                    Bundle bundle = new Bundle();
                    bundle.putString("inicio", inicio.toString());
                    bundle.putString("fin",    fin.toString());
                    Navigation.findNavController(requireView()).navigate(R.id.reportePagosFragment, bundle);
                });
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    // ── Botones de sector dinámicos ───────────────────────────────────────────

    private void cargarBotonesGrupo() {
        String[] grupos = getResources().getStringArray(R.array.grupos_array);
        for (String grupo : grupos) {
            ContextThemeWrapper ctx = new ContextThemeWrapper(
                    requireContext(),
                    com.google.android.material.R.style.Widget_MaterialComponents_Button_OutlinedButton);
            MaterialButton button = new MaterialButton(ctx, null, 0);
            button.setText(grupo);
            button.setId(View.generateViewId());
            button.setCheckable(true);
            button.setBackgroundTintList(
                    getResources().getColorStateList(R.color.colorToggle, requireContext().getTheme()));
            toggleButtonGroupZonas.addView(button);
        }
    }
}
