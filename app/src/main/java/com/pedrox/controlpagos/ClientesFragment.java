package com.pedrox.controlpagos;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
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
import com.pedrox.controlpagos.models.Sector;
import com.pedrox.controlpagos.responses.ClienteRespuesta;
import com.pedrox.controlpagos.viewmodels.ClientesViewModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Headers;

public class ClientesFragment extends Fragment implements ClienteAdapter.OnClienteActionListener {

    private static final String TAG = "ClientesFragment";

    private ClientesViewModel         viewModel;
    private RecyclerView              rvClientes;
    private ClienteAdapter            clienteAdapter;
    private FloatingActionButton      fabAddCliente;
    private MaterialButtonToggleGroup toggleButtonGroupZonas;
    private HorizontalScrollView      scrollViewZonas;
    private ChipGroup                 chipGroupEstado;
    private EditText                  etSearchCliente;
    private CircularProgressIndicator progressBar;
    private ImageButton               btnMenuAdmin;
    private ImageButton               btnRefrescar;
    private String                    rolUsuario;

    // Protege los listeners durante la restauración visual
    private boolean restaurando  = false;
    // Evita restaurar más de una vez por instancia del Fragment
    private boolean yaRestaurado = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lista_clientes, container, false);

        // ── 1. Bind de vistas ─────────────────────────────────────────────────
        fabAddCliente          = view.findViewById(R.id.fabAddCliente);
        etSearchCliente        = view.findViewById(R.id.etSearchCliente);
        toggleButtonGroupZonas = view.findViewById(R.id.toggleButtonGroupZonas);
        scrollViewZonas        = view.findViewById(R.id.scrollViewZonas);
        chipGroupEstado        = view.findViewById(R.id.chipGroupEstado);
        rvClientes             = view.findViewById(R.id.rvClientes);
        progressBar            = view.findViewById(R.id.progressBarClientes);
        btnMenuAdmin           = view.findViewById(R.id.btnMenuAdmin);
        btnRefrescar           = view.findViewById(R.id.btnRefrescar);
        Chip chipContadorClientes = view.findViewById(R.id.chipContadorClientes);
        rolUsuario = AppSession.getUsuarioActual().getRol();

        // ── 2. RecyclerView ───────────────────────────────────────────────────
        clienteAdapter = new ClienteAdapter(new ArrayList<>());
        rvClientes.setLayoutManager(new LinearLayoutManager(getContext()));
        rvClientes.setAdapter(clienteAdapter);
        clienteAdapter.setOnClienteActionListener(this);

        // ── 3. ViewModel ──────────────────────────────────────────────────────
        viewModel = new ViewModelProvider(requireActivity()).get(ClientesViewModel.class);

        // ── 4. Listeners — ANTES de observers ────────────────────────────────
        // Es importante que estén registrados antes de que cualquier observer
        // dispare restaurarEstadoVisual(), para que el flag `restaurando` funcione.
        registrarListeners();
        getParentFragmentManager().setFragmentResultListener(
                "sectorResult",
                getViewLifecycleOwner(),
                (key, bundle) -> {
                    if (bundle.getBoolean("sectoresModificados", false)) {
                        Log.d(TAG, "sectorResult recibido → recargando sectores");
                        viewModel.invalidarYRecargarSectores(requireContext());
                        viewModel.cargarClientes(requireContext());
                    }
                }
        );

        // ── 5. Observers simples (contador, cargando, clientes filtrados) ─────
        viewModel.getClientesFiltrados().observe(getViewLifecycleOwner(),
                clientes -> clienteAdapter.updateClientes(clientes));

        viewModel.getContadorClientes().observe(getViewLifecycleOwner(),
                count -> chipContadorClientes.setText(
                        count + " cliente" + (count != 1 ? "s" : "")));

        viewModel.isCargando().observe(getViewLifecycleOwner(), cargando -> {
            progressBar.setVisibility(cargando ? View.VISIBLE : View.GONE);
            rvClientes.setVisibility(cargando ? View.GONE : View.VISIBLE);
        });

        // ── 6. Observer de sectores — carga encadenada ────────────────────────
        // Cuando llegan los sectores:
        //   a) Se crean los botones si la vista está vacía (instancia nueva)
        //   b) Se restaura el estado visual UNA sola vez
        //   c) Se cargan los clientes DESPUÉS (encadenado)
        viewModel.getSectores().observe(getViewLifecycleOwner(), sectores -> {
            Log.d(TAG, "▶ observer sectores disparado, size=" +
                    (sectores == null ? "NULL" : sectores.size()));
            if (sectores == null) return;
            // ✅ Siempre limpiar y recrear — cubre primera entrada, regreso,
            // y actualización desde SectoresFragment
            toggleButtonGroupZonas.removeAllViews();
            for (Sector sector : sectores) {
                ContextThemeWrapper ctx = new ContextThemeWrapper(
                        requireContext(),
                        com.google.android.material.R.style.Widget_MaterialComponents_Button_OutlinedButton);
                MaterialButton button = new MaterialButton(ctx, null, 0);
                button.setText(sector.getNombre());
                button.setId(View.generateViewId());
                button.setCheckable(true);
                button.setBackgroundTintList(
                        getResources().getColorStateList(R.color.colorToggle,
                                requireContext().getTheme()));
                toggleButtonGroupZonas.addView(button);
            }

            // Siempre restaurar — botones siempre recién creados
            yaRestaurado = true;
            restaurarEstadoVisual();

            if (!viewModel.hayClientesCargados()) {
                viewModel.cargarClientes(requireContext());
            }
        });

        // ── 7. Disparar carga de sectores ─────────────────────────────────────
        // Si ya están en cache, el observer recibe el valor inmediatamente
        // (LiveData re-emite al registrarse) y encadena la carga de clientes.
        // Si no están, hace el request y cuando lleguen dispara el observer.
        viewModel.cargarSectores(requireContext());

        // ── 8. Visibilidad según rol ──────────────────────────────────────────
        if (!"Admin".equals(rolUsuario)) {
            btnMenuAdmin.setVisibility(View.GONE);
            fabAddCliente.setVisibility(View.GONE);
        }
        btnMenuAdmin.setOnClickListener(this::mostrarMenuAdmin);
        btnRefrescar.setOnClickListener(v -> {
            // Refrescar fuerza recarga de clientes sin tocar sectores
            viewModel.cargarClientes(requireContext());
        });
        fabAddCliente.setOnClickListener(v -> mostrarDialogAgregarCliente());

        return view;
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    private void registrarListeners() {

        // Búsqueda por texto
        etSearchCliente.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!restaurando) viewModel.setFiltroTexto(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Filtro sector
        toggleButtonGroupZonas.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked && !restaurando) {
                Button btn = group.findViewById(checkedId);
                if (btn != null) viewModel.setFiltroGrupo(btn.getText().toString());
            }
        });

        // Filtro estado
        chipGroupEstado.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (restaurando || checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipEstadoPendientes) {
                viewModel.setFiltroEstado(ClientesViewModel.ESTADO_PENDIENTES);
            } else if (id == R.id.chipEstadoMorosos) {
                viewModel.setFiltroEstado(ClientesViewModel.ESTADO_MOROSOS);
            } else {
                viewModel.setFiltroEstado(ClientesViewModel.ESTADO_TODOS);
            }
        });
    }

    // ── Restaurar estado visual ────────────────────────────────────────────────
    // Se llama UNA vez por instancia del Fragment, desde el observer de sectores,
    // garantizando que los botones ya están en la vista cuando se ejecuta.

    private void restaurarEstadoVisual() {
        restaurando = true;
        // 1. Chip de estado
        String estado = viewModel.getFiltroEstado();
        if (ClientesViewModel.ESTADO_PENDIENTES.equals(estado)) {
            chipGroupEstado.check(R.id.chipEstadoPendientes);
        } else if (ClientesViewModel.ESTADO_MOROSOS.equals(estado)) {
            chipGroupEstado.check(R.id.chipEstadoMorosos);
        } else {
            chipGroupEstado.check(R.id.chipEstadoTodos);
        }

        // 2. Texto de búsqueda
        String texto = viewModel.getFiltroTexto();
        if (!etSearchCliente.getText().toString().equals(texto)) {
            etSearchCliente.setText(texto);
            if (!texto.isEmpty()) etSearchCliente.setSelection(texto.length());
        }

        // 3. Sector activo + scroll
        // post() encola la acción DESPUÉS del layout pass actual,
        // garantizando que btn.getLeft() y getWidth() tienen valores reales.
        String grupoActivo = viewModel.getFiltroGrupo();
        toggleButtonGroupZonas.post(() -> marcarYScrollSector(grupoActivo));
    }

    private void marcarYScrollSector(String grupoActivo) {
        // Primero limpiar selección actual del grupo
        toggleButtonGroupZonas.clearChecked();
        for (int i = 0; i < toggleButtonGroupZonas.getChildCount(); i++) {
            View v = toggleButtonGroupZonas.getChildAt(i);
            if (v instanceof MaterialButton) {
                MaterialButton btn = (MaterialButton) v;
                if (btn.getText().toString().equals(grupoActivo)) {
                    Log.d(TAG, "marcarYScrollSector: detecto aboton activo");
                    toggleButtonGroupZonas.check(btn.getId());
                    int centrado = btn.getLeft()
                            - (scrollViewZonas.getWidth() / 2)
                            + (btn.getWidth() / 2);
                    scrollViewZonas.scrollTo(Math.max(0, centrado), 0);
                    break;
                }
            }
        }
        restaurando = false;
    }

    // ── Navegación ────────────────────────────────────────────────────────────

    @Override
    public void onClienteClick(Cliente cliente) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("cliente", cliente);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_clientesFragment_to_detalleClienteFragment, bundle);
    }

    @Override
    public void onRestaurarCliente(Cliente cliente) {

    }

    // ── Dialog agregar cliente ────────────────────────────────────────────────

    private void mostrarDialogAgregarCliente() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_agregar_cliente, null);
        EditText etNombre     = view.findViewById(R.id.etNombre);
        EditText etReferencia = view.findViewById(R.id.etReferencia);
        EditText etTelefono   = view.findViewById(R.id.etTelefono);
        Spinner  spGrupo      = view.findViewById(R.id.spGrupo);

        // Sectores ya están en cache — sin request extra
        List<Sector> sectores = viewModel.getSectores().getValue();
        if (sectores != null) {
            String grupoActivo = viewModel.getFiltroGrupo();
            String sectorInicial = "TODOS".equals(grupoActivo) ? null : grupoActivo;
            aplicarSpinner(spGrupo, sectores, sectorInicial);
        }

        builder.setView(view)
                .setTitle("Agregar Cliente")
                .setPositiveButton("Guardar", (dialog, id) -> {
                    String nombre = etNombre.getText().toString().trim();
                    if (nombre.isEmpty()) {
                        Toast.makeText(getContext(), "El nombre no puede ser vacío",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Sector sectorSeleccionado = (Sector) spGrupo.getSelectedItem();
                    JSONObject obj = new JSONObject();
                    try {
                        obj.put("nombre",     nombre);
                        obj.put("referencia", etReferencia.getText().toString().trim());
                        obj.put("telefono",   etTelefono.getText().toString().trim());
                        obj.put("sector_id",  sectorSeleccionado != null ? sectorSeleccionado.getId() : 0);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    BodyRequestBuilder rb = EasyHttp.post(getContext(),
                            Constants.API_BASE_URL + "cliente/add");
                    rb.getOkHttpClientBuilder()
                            .addInterceptor(HttpClient.interceptor(getContext()));
                    rb.stringBody("application/json; charset=utf-8", obj.toString())
                            .build()
                            .getJsonAsObject(ClienteRespuesta.class,
                                    new JsonResponseListener<ClienteRespuesta>() {
                                        @Override
                                        public void onSuccess(@NonNull Headers headers,
                                                              @NonNull ClienteRespuesta respuesta) {
                                            if (!respuesta.isError()) {
                                                viewModel.agregarCliente(respuesta.getData());
                                                requireActivity().runOnUiThread(() ->
                                                        Toast.makeText(getContext(), "Cliente agregado",
                                                                Toast.LENGTH_SHORT).show()
                                                );
                                            }
                                        }
                                        @Override
                                        public void onError(@NonNull HttpException e) {
                                            requireActivity().runOnUiThread(() ->
                                                    Toast.makeText(getContext(), e.getMessage(),
                                                            Toast.LENGTH_SHORT).show()
                                            );
                                        }
                                    });
                })
                .setNegativeButton("Cancelar", (dialog, id) -> dialog.dismiss());
        builder.create().show();
    }

    private void aplicarSpinner(Spinner sp, List<Sector> sectores, String grupoActual) {
        // Filtrar "TODOS" — no es un sector asignable a un cliente
        List<Sector> sinTodos = new ArrayList<>();
        for (Sector s : sectores) {
            if (s.getId() != -1) sinTodos.add(s);
        }
        ArrayAdapter<Sector> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, sinTodos);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(adapter);
        if (grupoActual != null) {
            for (int i = 0; i < sinTodos.size(); i++) {
                if (sinTodos.get(i).getNombre().equalsIgnoreCase(grupoActual)) {
                    sp.setSelection(i);
                    break;
                }
            }
        }
    }

    // ── Menú admin ────────────────────────────────────────────────────────────

    private void mostrarMenuAdmin(View anchor) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), anchor);
        popupMenu.getMenuInflater().inflate(R.menu.menu_admin, popupMenu.getMenu());
        try {
            Field f = popupMenu.getClass().getDeclaredField("mPopup");
            f.setAccessible(true);
            Object mPopup = f.get(popupMenu);
            mPopup.getClass()
                    .getDeclaredMethod("setForceShowIcon", boolean.class)
                    .invoke(mPopup, true);
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
            } else if (id == R.id.action_clientes_archivados) {
                Navigation.findNavController(requireView()).navigate(R.id.clientesArchivadosFragment);
                return true;
            } else if (id == R.id.action_cartera_viva) {
                Navigation.findNavController(requireView()).navigate(R.id.carteraVivaFragment);
                return true;
            } else if (id == R.id.action_gestionar_sectores) {
                Navigation.findNavController(requireView()).navigate(R.id.sectoresFragment);
                return true;
            } else if (id == R.id.action_reportes) {
                MaterialDatePicker.Builder<Pair<Long, Long>> b =
                        MaterialDatePicker.Builder.dateRangePicker();
                b.setTitleText("Selecciona un rango de fechas");
                MaterialDatePicker<Pair<Long, Long>> picker = b.build();
                picker.show(getParentFragmentManager(), picker.toString());
                picker.addOnPositiveButtonClickListener(selection -> {
                    LocalDate inicio = Instant.ofEpochMilli(selection.first)
                            .atZone(ZoneOffset.UTC).toLocalDate();
                    LocalDate fin = Instant.ofEpochMilli(selection.second)
                            .atZone(ZoneOffset.UTC).toLocalDate();
                    Bundle bundle = new Bundle();
                    bundle.putString("inicio", inicio.toString());
                    bundle.putString("fin",    fin.toString());
                    Navigation.findNavController(requireView())
                            .navigate(R.id.reportePagosFragment, bundle);
                });
                return true;
            } else if (id == R.id.action_productos_vendidos) {
                MaterialDatePicker.Builder<Pair<Long, Long>> b =
                        MaterialDatePicker.Builder.dateRangePicker();
                b.setTitleText("Selecciona un rango de fechas");
                MaterialDatePicker<Pair<Long, Long>> picker = b.build();
                picker.show(getParentFragmentManager(), picker.toString());
                picker.addOnPositiveButtonClickListener(selection -> {
                    LocalDate inicio = Instant.ofEpochMilli(selection.first)
                            .atZone(ZoneOffset.UTC).toLocalDate();
                    LocalDate fin = Instant.ofEpochMilli(selection.second)
                            .atZone(ZoneOffset.UTC).toLocalDate();
                    Bundle bundle = new Bundle();
                    bundle.putString("inicio", inicio.toString());
                    bundle.putString("fin",    fin.toString());
                    Navigation.findNavController(requireView())
                            .navigate(R.id.reporteProductosFragment, bundle);
                });
                return true;
            } else if (id == R.id.action_cerrar_sesion) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Cerrar sesión")
                        .setMessage("¿Estás seguro que deseas cerrar sesión?")
                        .setPositiveButton("Cerrar sesión", (d, w) -> cerrarSesion())
                        .setNegativeButton("Cancelar", null)
                        .show();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void cerrarSesion() {
        // Limpiar sesión guardada
        AppSession.logout(requireContext());
        // Navegar al Login limpiando todo el back stack
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}