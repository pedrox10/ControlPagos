package com.pedrox.controlpagos;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager; // Importar
import androidx.recyclerview.widget.RecyclerView; // Importar

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jimmyworks.easyhttp.EasyHttp;
import com.jimmyworks.easyhttp.exception.HttpException;
import com.jimmyworks.easyhttp.listener.JsonResponseListener;
import com.jimmyworks.easyhttp.listener.StringResponseListener;
import com.pedrox.controlpagos.adapters.PagoAdapter; // Importar
import com.pedrox.controlpagos.adapters.ProductoPedidoAdapter; // Importar
import com.pedrox.controlpagos.models.Pago;
import com.pedrox.controlpagos.models.Pedido;
import com.pedrox.controlpagos.models.Producto;
import com.pedrox.controlpagos.models.ProductoPedido;
import com.pedrox.controlpagos.models.Usuario;
import com.pedrox.controlpagos.responses.PagoRespuesta;
import com.pedrox.controlpagos.responses.PedidoRespuesta;
import com.pedrox.controlpagos.responses.ProductoPedidoRespuesta;
import com.pedrox.controlpagos.responses.RespuestaLista;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList; // Importar
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Headers;

public class DetallePedidoFragment extends Fragment {

    private static final String ARG_PEDIDO = "pedido"; // Clave del argumento

    private Usuario usuario;
    private Pedido pedido;
    private TextView tvFechaEntrega, tvTotalPedido,  tvCuotasDePedido,  tvCliente, tvSaldoPedido, tvPagadoPedido;
    private MaterialButton btnEditarPedido, btnEliminarPedido, btnAddProductoPedido, btnAgregarPago;
    private RecyclerView rvPagos;
    private ProductoPedidoAdapter productoPedidoAdapter;
    private PagoAdapter pagoAdapter;
    private TableLayout tableLayout;
    private String rolUsuario;
    List<Producto> listaProductos = new ArrayList<>();
    ArrayAdapter<String> productoAdapter;
    List<String> nombresProductos = new ArrayList<>();
    Producto productoSeleccionado;
    int cantidad;
    InputFilter[] filtro;
    MaterialButton enviarComprobante;

    // Método newInstance (si lo usas para pasar el Pedido)
    public static DetallePedidoFragment newInstance(Pedido pedido) {
        DetallePedidoFragment fragment = new DetallePedidoFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PEDIDO, pedido);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (listaProductos.isEmpty()) {
            cargarProductos();
        }
        usuario = AppSession.getUsuarioActual();
        if (getArguments() != null) {
            pedido = (Pedido) getArguments().getSerializable(ARG_PEDIDO);
        }
        filtro = new InputFilter[]{
                (source, start, end, dest, dstart, dend) -> {
                    String resultado = dest.subSequence(0, dstart)
                            + source.toString()
                            + dest.subSequence(dend, dest.length());

                    // Permite hasta 4 enteros y 1 decimal
                    if (resultado.matches("^\\d{0,4}(\\.\\d{0,1})?$")) {
                        return null; // válido, se permite escribir
                    } else {
                        return "";   // invalido, bloquea la entrada
                    }
                }
        };
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rolUsuario = AppSession.getUsuarioActual().getRol();
        View view = inflater.inflate(R.layout.fragment_detalle_pedido, container, false);

        if (pedido == null) {
            Toast.makeText(getContext(), "Error: No se pudo cargar la información del pedido.", Toast.LENGTH_LONG).show();
            return view;
        }

        tvFechaEntrega = view.findViewById(R.id.tvDetallePedidoFecha);
        tvCliente = view.findViewById(R.id.tvCliente);
        tvTotalPedido = view.findViewById(R.id.tvTotalPedido);
        tvPagadoPedido = view.findViewById(R.id.tvPagadoPedido);
        tvCuotasDePedido = view.findViewById(R.id.tvCuotasDePedido);
        tvSaldoPedido = view.findViewById(R.id.tvSaldoPedido);
        btnEditarPedido = view.findViewById(R.id.btnEditarPedido);
        btnEditarPedido.setOnClickListener(v -> dialogoEditarPedido(pedido));
        btnEliminarPedido = view.findViewById(R.id.btnEliminarPedido);
        btnEliminarPedido.setOnClickListener(v -> confirmarEliminarPedido(pedido));
        btnAddProductoPedido = view.findViewById(R.id.btnAddProductoPedido);
        btnAddProductoPedido.setOnClickListener(v -> {
            if (nombresProductos.isEmpty()) {
                Toast.makeText(getContext(),
                        "Cargando productos...",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            dialogoAgregarProductoPedido();
        });
        btnAgregarPago= view.findViewById(R.id.btnAgregarPago);
        btnAgregarPago.setOnClickListener(v -> mostrarDialogoPago(null));
        tvFechaEntrega.setText(pedido.getFecha());
        tvCliente.setText(pedido.getCliente().getNombre());
        tvTotalPedido.setText(pedido.getTotal() + "");
        tvCuotasDePedido.setText(pedido.getCuotasDe() + "");
        tvPagadoPedido.setText(pedido.getPagado() + "");
        tvSaldoPedido.setText(pedido.getSaldo() + "");
        tableLayout = view.findViewById(R.id.tlProductos);
        for (ProductoPedido pp : pedido.getProductos()) {
            agregarProductoPedido(tableLayout, pp);
        }

        rvPagos = view.findViewById(R.id.rvDetallePagos);
        rvPagos.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        // Asegúrate de que getPagos() no devuelva null
        List<com.pedrox.controlpagos.models.Pago> pagos = pedido.getPagos();
        System.out.println(pagos);
        if (pagos == null) {
            pagos = new ArrayList<>(); // Inicializar lista vacía si es null
        }
        pagoAdapter = new PagoAdapter(pagos);
        pagoAdapter.setOnPagoClickListener(pago -> {
            if (!"Admin".equals(rolUsuario)) {
                boolean esPropio = pago.getUsuario().getId() == usuario.getId();
                if(esPropio)
                    mostrarDialogoPago(pago);
                else
                    Toast.makeText(view.getContext(), "¡No registraste este pago!", Toast.LENGTH_SHORT).show();
            } else {
                System.out.println(pago.toString());
                mostrarDialogoPago(pago);
            }
        });
        rvPagos.setAdapter(pagoAdapter);
        enviarComprobante = view.findViewById(R.id.btn_enviar_comprobante);
        enviarComprobante.setVisibility(View.VISIBLE);
        enviarComprobante.setOnClickListener( v-> {
            String telefono = pedido.getCliente().getTelefono();
            if (telefono != null && !telefono.trim().isEmpty() && !telefono.equals("0")) {
                StringBuilder sb = new StringBuilder();
                sb.append("📋 *Comprobante de Pago*\n\n");
                sb.append("Cliente: ").append(pedido.getCliente().getNombre()).append("\n");
                sb.append("Fecha del pedido: ").append(pedido.getFecha()).append("\n");
                sb.append("Total del pedido: ").append(pedido.getTotal()).append(" Bs\n");
                sb.append("Pagado: ").append(pedido.getPagado()).append(" Bs\n");
                sb.append("Saldo pendiente: ").append(pedido.getSaldo()).append(" Bs\n\n");

                sb.append("*Productos del pedido:*\n");
                int i = 1;
                for (ProductoPedido pp : pedido.getProductos()) {
                    sb.append(pp.getCantidad()).append("x")
                            .append(pp.getProducto().getNombre())
                            .append(" — ").append(pp.getTotal()).append(" Bs\n");
                }
                sb.append("\n*Pagos realizados:*\n");
                i = 1;
                for (Pago pago : pedido.getPagos()) {
                    sb.append("#").append(i++).append(" — ")
                            .append(pago.getMonto()).append(" Bs — ")
                            .append(pago.getFecha()).append("\n");
                }

                sb.append("Gracias por su pago\n");
                String mensaje = sb.toString();
                try {
                    String url = "https://wa.me/591" + telefono + "?text=" + Uri.encode(mensaje);
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    getContext().startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Error al enviar mensaje", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(v.getContext(), "Teléfono no válido", Toast.LENGTH_SHORT).show();
            }
        });
        if (!"Admin".equals(rolUsuario)) {
            btnEditarPedido.setVisibility(View.GONE);
            btnEliminarPedido.setVisibility(View.GONE);
            btnAddProductoPedido.setVisibility(View.GONE);
        }
        return view;
    }

    private void agregarProductoPedido(TableLayout tableLayout, ProductoPedido pp) {
        TableRow row = new TableRow(getContext());
        row.setId(pp.getId());
        row.setGravity(Gravity.CENTER); // Igual que el XML
        row.setPadding(dpToPx(0), dpToPx(0), dpToPx(0), dpToPx(0));

        TextView tvProducto = new TextView(getContext());
        tvProducto.setTag("producto");
        tvProducto.setMaxWidth(200);
        tvProducto.setMaxLines(2);
        tvProducto.setText(pp.getProducto().getNombre());
        tvProducto.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        row.addView(tvProducto);

        TextView tvCantidad = new TextView(getContext());
        tvCantidad.setTag("cantidad");
        tvCantidad.setText(String.valueOf(pp.getCantidad()));
        tvCantidad.setGravity(Gravity.CENTER);
        tvCantidad.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        row.addView(tvCantidad);

        TextView tvTotal = new TextView(getContext());
        tvTotal.setTag("total");
        tvTotal.setText(String.valueOf(pp.getTotal()));
        tvTotal.setGravity(Gravity.CENTER);
        tvTotal.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        row.addView(tvTotal);

        LinearLayout actionsLayout = new LinearLayout(getContext());
        actionsLayout.setOrientation(LinearLayout.HORIZONTAL);
        actionsLayout.setGravity(Gravity.CENTER);

        MaterialButton btnEdit = new MaterialButton(getContext());
        btnEdit.setTag("btnEdit");
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(dpToPx(42), dpToPx(42));
        editParams.setMarginEnd(dpToPx(4));
        btnEdit.setLayoutParams(editParams);
        btnEdit.setCornerRadius(dpToPx(4));
        btnEdit.setPadding(20,0,0,0);
        btnEdit.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        btnEdit.setIconResource(R.drawable.edit_24px);
        btnEdit.setIconTint(ColorStateList.valueOf(Color.parseColor("#49A8F6")));
        btnEdit.setIconSize(dpToPx(21));
        btnEdit.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ECEBEB")));
        btnEdit.setContentDescription("Editar");
        btnEdit.setOnClickListener(v -> {
            dialogoEditarProductoPedido(pp);
        });

        MaterialButton btnDelete = new MaterialButton(getContext());
        LinearLayout.LayoutParams delParams = new LinearLayout.LayoutParams(dpToPx(42), dpToPx(42));
        delParams.setMarginEnd(dpToPx(4));
        btnDelete.setLayoutParams(delParams);

        btnDelete.setCornerRadius(dpToPx(4));
        btnDelete.setPadding(0,0,20,0);
        btnDelete.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_END);

        btnDelete.setIconResource(com.jimmyworks.easyhttp.R.drawable.easy_http_ic_delete);
        btnDelete.setIconTint(ColorStateList.valueOf(Color.parseColor("#B8625C")));
        btnDelete.setIconSize(dpToPx(21));
        btnDelete.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ECEBEB")));
        btnDelete.setContentDescription("Eliminar");
        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Eliminar producto")
                    .setMessage("¿Seguro que deseas eliminar este producto del pedido?")
                    .setPositiveButton("Eliminar", (dialog, which) -> {
                        JSONObject obj = new JSONObject();
                        try { obj.put("id", pp.getId()); } catch (JSONException e) { e.printStackTrace(); }
                        EasyHttp.post(getContext(), Constants.API_BASE_URL + "producto/pedido/delete")
                                .stringBody("application/json; charset=utf-8", obj.toString())
                                .build()
                                .getJsonAsObject(ProductoPedidoRespuesta.class, new JsonResponseListener<ProductoPedidoRespuesta>() {
                                    @Override
                                    public void onSuccess(@NonNull Headers headers, @NonNull ProductoPedidoRespuesta resp) {
                                        if (!resp.isError()) {
                                            ProductoPedido pp = resp.getData();
                                            pp.setPedido(pedido);
                                            pp.setProducto(productoSeleccionado);
                                            pedido.deleteProducto(pp);
                                            tableLayout.removeView(row); // Elimina la fila de la UI
                                            actualizarVistaPedido();
                                            Toast.makeText(getContext(), "Producto eliminado", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(getContext(), resp.getMensaje(), Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onError(@NonNull HttpException e) {
                                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    })
                    .setNegativeButton("Cancelar", (d, w) -> d.dismiss())
                    .show();
        });
        if (!"Admin".equals(rolUsuario)) {
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
        }
        actionsLayout.addView(btnEdit);
        actionsLayout.addView(btnDelete);
        row.addView(actionsLayout);
        tableLayout.addView(row);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void dialogoEditarPedido(Pedido pedido) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View view = inflater.inflate(R.layout.dialog_nuevo_pedido, null);

        DatePicker datePicker = view.findViewById(R.id.datePicker);
        EditText etCuotasDe = view.findViewById(R.id.etCuotasDe);
        etCuotasDe.setFilters(filtro);
        etCuotasDe.setText(pedido.getCuotasDe() +  "");
        String fechaPedido = pedido.getFecha(); // por ej. "26/09/2025"
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        try {
            Date date = sdf.parse(fechaPedido);           // convierte a Date
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);                            // configura el Calendar
            // Ahora actualiza el DatePicker
            datePicker.updateDate(
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),              // ¡ojo! base 0
                    cal.get(Calendar.DAY_OF_MONTH)
            );
        } catch (ParseException e) {
            e.printStackTrace(); // manejar error de parseo
        }
        builder.setView(view)
                .setTitle("Editar Pedido")
                .setPositiveButton("Aceptar", (dialog, id) -> {
                    int day = datePicker.getDayOfMonth();
                    int month = datePicker.getMonth() + 1; // aquí sumas 1 porque DatePicker da base 0
                    int year = datePicker.getYear();
                    String fechaStr = String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month, year);
                    String  cuotasDeStr =  etCuotasDe.getText().toString().trim();
                    if(cuotasDeStr.isEmpty())
                        cuotasDeStr  =  "0";
                    double cuotasDe = Double.parseDouble(cuotasDeStr);
                    JSONObject obj = new JSONObject();
                    try {
                        obj.put("id", pedido.getId());
                        obj.put("fecha", fechaStr);
                        obj.put("cuotas_de", cuotasDe);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // Crear pedido en backend
                    EasyHttp.post(getContext(), Constants.API_BASE_URL + "pedido/edit")
                            .stringBody("application/json; charset=utf-8", obj.toString())
                            .build()
                            .getJsonAsObject(PedidoRespuesta.class, new JsonResponseListener<PedidoRespuesta>() {
                                @Override
                                public void onSuccess(@NonNull Headers headers, @NonNull PedidoRespuesta respuesta) {
                                    if (!respuesta.isError()) {
                                        Pedido res = respuesta.getData();
                                        System.out.println(res.toString());
                                        pedido.setFecha(res.getFecha());
                                        tvFechaEntrega.setText(pedido.getFecha());
                                        pedido.setCuotasDe(res.getCuotasDe());
                                        tvCuotasDePedido.setText(pedido.getCuotasDe() + "");
                                        Toast.makeText(getContext(), respuesta.getMensaje(), Toast.LENGTH_SHORT).show();
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

    private void confirmarEliminarPedido(Pedido pedido) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar Pedido")
                .setMessage("¿Seguro que deseas eliminar el pedido?. Se borrarán también todos sus productos y pagos realizados")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    JSONObject obj = new JSONObject();
                    try {
                        obj.put("id", pedido.getId());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    EasyHttp.post(getContext(), Constants.API_BASE_URL + "pedido/delete")
                            .stringBody("application/json; charset=utf-8", obj.toString())
                            .build()
                            .getJsonAsObject(PedidoRespuesta.class, new JsonResponseListener<PedidoRespuesta>() {
                                @Override
                                public void onSuccess(@NonNull Headers headers, @NonNull PedidoRespuesta respuesta) {
                                    if (!respuesta.isError()) {
                                        Toast.makeText(getContext(), respuesta.getMensaje(), Toast.LENGTH_SHORT).show();
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

    private void dialogoAgregarProductoPedido() {
        productoSeleccionado = null;
        cantidad = 1;
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_producto_pedido, null);
        builder.setView(view);

        AutoCompleteTextView tvProducto = view.findViewById(R.id.tvAutocompleteProducto);
        productoAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                nombresProductos
        );

        tvProducto.setAdapter(productoAdapter);
        tvProducto.setThreshold(1);
        tvProducto.requestFocus();
        EditText etPrecio = view.findViewById(R.id.etPrecio);
        etPrecio.setFilters(filtro);
        TextView tvCantidad = view.findViewById(R.id.tvCantidad);
        TextView tvTotal = view.findViewById(R.id.tvTotal);
        MaterialButton btnAumentar = view.findViewById(R.id.btnAumentar);
        MaterialButton btnDisminuir = view.findViewById(R.id.btnDisminuir);
        btnAumentar.setOnClickListener(v -> {
            cantidad++;
            tvCantidad.setText(cantidad + "");
            actualizarTotal(etPrecio, tvCantidad, tvTotal);
        });

        btnDisminuir.setOnClickListener(v -> {
            if (cantidad > 1) cantidad--;
            tvCantidad.setText(cantidad + "");
            actualizarTotal(etPrecio, tvCantidad, tvTotal);
        });

        // Cuando selecciona un producto del autocomplete
        tvProducto.setOnItemClickListener((parent, v, position, id) -> {
            String nombre = (String) parent.getItemAtPosition(position);
            for (Producto p : listaProductos) {
                if (p.getNombre().equals(nombre)) {
                    productoSeleccionado = p;
                    break;
                }
            }
        });

        etPrecio.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                actualizarTotal(etPrecio, tvCantidad, tvTotal);
            }
        });

        builder.setTitle("Agregar Producto al Pedido");
        builder.setPositiveButton("Guardar", (dialog, which) -> {
            if (productoSeleccionado == null) {
                Toast.makeText(getContext(), "Seleccione un producto", Toast.LENGTH_SHORT).show();
                return;
            }
            double precio = Double.parseDouble(etPrecio.getText().toString());
            JSONObject obj = new JSONObject();
            try {
                obj.put("pedido_id", pedido.getId());
                obj.put("producto_id", productoSeleccionado.getId());
                obj.put("cantidad", cantidad);
                obj.put("precio", precio);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            EasyHttp.post(getContext(), Constants.API_BASE_URL + "producto/pedido/add")
                    .stringBody("application/json; charset=utf-8", obj.toString())
                    .build()
                    .getJsonAsObject(ProductoPedidoRespuesta.class, new JsonResponseListener<ProductoPedidoRespuesta>() {
                        @Override
                        public void onSuccess(@NonNull Headers headers, @NonNull ProductoPedidoRespuesta resp) {
                            if (!resp.isError()) {
                                Toast.makeText(getContext(), "Producto agregado", Toast.LENGTH_SHORT).show();
                                ProductoPedido pp = resp.getData();
                                pp.setPedido(pedido);
                                pp.setProducto(productoSeleccionado);
                                pedido.addProducto(pp);
                                agregarProductoPedido(tableLayout, pp);
                                actualizarVistaPedido();
                            } else {
                                Toast.makeText(getContext(), resp.getMensaje(), Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onError(@NonNull HttpException e) {
                            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        builder.setNegativeButton("Cancelar", (d, w) -> d.dismiss());
        builder.create().show();
    }

    private void dialogoEditarProductoPedido(ProductoPedido pp) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_producto_pedido, null);
        builder.setView(view);

        AutoCompleteTextView tvProducto = view.findViewById(R.id.tvAutocompleteProducto);
        EditText etPrecio = view.findViewById(R.id.etPrecio);
        etPrecio.setFilters(filtro);
        TextView tvCantidad = view.findViewById(R.id.tvCantidad);
        TextView tvTotal = view.findViewById(R.id.tvTotal);
        MaterialButton btnAumentar = view.findViewById(R.id.btnAumentar);
        MaterialButton btnDisminuir = view.findViewById(R.id.btnDisminuir);

        // Rellenar datos existentes
        productoSeleccionado = pp.getProducto();
        tvProducto.setText(productoSeleccionado.getNombre());
        etPrecio.setText(String.valueOf(pp.getPrecio()));
        cantidad = pp.getCantidad();
        tvCantidad.setText(String.valueOf(cantidad));
        tvTotal.setText("Total: " + (pp.getPrecio() * pp.getCantidad()));
        actualizarTotal(etPrecio, tvCantidad, tvTotal);

        // Listeners
        btnAumentar.setOnClickListener(v -> {
            cantidad++;
            tvCantidad.setText(String.valueOf(cantidad));
            actualizarTotal(etPrecio, tvCantidad, tvTotal);
        });

        btnDisminuir.setOnClickListener(v -> {
            if (cantidad > 1) cantidad--;
            tvCantidad.setText(String.valueOf(cantidad));
            actualizarTotal(etPrecio, tvCantidad, tvTotal);
        });
        tvProducto.setEnabled(false);
        tvProducto.setFocusable(false);
        tvProducto.setClickable(false);

        etPrecio.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                actualizarTotal(etPrecio, tvCantidad, tvTotal);
            }
        });

        builder.setTitle("Editar Producto del Pedido");
        builder.setPositiveButton("Guardar", (dialog, which) -> {
            if (productoSeleccionado == null) {
                Toast.makeText(getContext(), "Seleccione un producto", Toast.LENGTH_SHORT).show();
                return;
            }

            double precio = Double.parseDouble(etPrecio.getText().toString());
            JSONObject obj = new JSONObject();
            try {
                obj.put("id", pp.getId());
                obj.put("pedido_id", pedido.getId());
                obj.put("producto_id", productoSeleccionado.getId());
                obj.put("cantidad", cantidad);
                obj.put("precio", precio);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            EasyHttp.post(getContext(), Constants.API_BASE_URL + "producto/pedido/edit")
                    .stringBody("application/json; charset=utf-8", obj.toString())
                    .build()
                    .getJsonAsObject(ProductoPedidoRespuesta.class, new JsonResponseListener<ProductoPedidoRespuesta>() {
                        @Override
                        public void onSuccess(@NonNull Headers headers, @NonNull ProductoPedidoRespuesta resp) {
                            if (!resp.isError()) {
                                Toast.makeText(getContext(), "Producto actualizado", Toast.LENGTH_SHORT).show();
                                ProductoPedido pp = resp.getData();
                                pp.setPedido(pedido);
                                pp.setProducto(productoSeleccionado);
                                TableRow fila = tableLayout.findViewById(pp.getId());
                                pedido.editProducto(pp);
                                actualizarFila(fila, pp);
                                actualizarVistaPedido();
                            } else {
                                Toast.makeText(getContext(), resp.getMensaje(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onError(@NonNull HttpException e) {
                            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        builder.setNegativeButton("Cancelar", (d, w) -> d.dismiss());
        builder.create().show();
    }

    private void mostrarDialogoPago(@Nullable Pago pagoExistente) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.dialog_pago, null);

        TextView tvFechaPago = view.findViewById(R.id.tvFechaPago);
        EditText etMontoPago = view.findViewById(R.id.etMontoPago);
        TextView tvCobradorPago = view.findViewById(R.id.tvCobradorPago);
        etMontoPago.setFilters(filtro);

        // Si es edición, rellenar campos
        if (pagoExistente != null) {
            tvFechaPago.setText(pagoExistente.getFecha());
            etMontoPago.setText(String.valueOf(pagoExistente.getMonto()));
            tvCobradorPago.setText(pagoExistente.getUsuario().getNombre());
        } else {
            // Fecha actual por defecto
            String hoy = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .format(new Date());
            tvFechaPago.setText(hoy);
            etMontoPago.setText(pedido.getCuotasDe() + "");
            tvCobradorPago.setText(usuario.getNombre());
        }
        // Al hacer clic en la fecha, abrir selector
        tvFechaPago.setOnClickListener(v -> {
            // Obtener la fecha actual o la del pago si existe
            final Calendar cal = Calendar.getInstance();
            if (pagoExistente != null) {
                try {
                    Date fechaExistente = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .parse(pagoExistente.getFecha());
                    if (fechaExistente != null) cal.setTime(fechaExistente);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);
            int day = cal.get(Calendar.DAY_OF_MONTH);

            // Crear el diálogo
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    getContext(),
                    (view1, year1, month1, dayOfMonth) -> {
                        // OJO: month1 empieza en 0 (enero = 0)
                        String fechaSeleccionada = String.format(Locale.getDefault(), "%02d/%02d/%04d",
                                dayOfMonth, month1 + 1, year1);
                        tvFechaPago.setText(fechaSeleccionada);
                    },
                    year, month, day
            );

            datePickerDialog.show();
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(view)
                .setTitle(pagoExistente == null ? "Agregar pago" : "Editar pago")
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String fecha = tvFechaPago.getText().toString().trim();
                    String montoStr = etMontoPago.getText().toString().trim();

                    if (fecha.isEmpty() || montoStr.isEmpty() || montoStr.equals(".")) {
                        Toast.makeText(getContext(), "Debe completar los campos", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double monto = Double.parseDouble(montoStr);
                    JSONObject obj = new JSONObject();
                    try {
                        obj.put("pedido_id", pedido.getId());
                        obj.put("usuario_id", usuario.getId());
                        obj.put("fecha", fecha);
                        obj.put("monto", monto);
                        if (pagoExistente != null)
                            obj.put("id", pagoExistente.getId());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    String url = pagoExistente == null
                            ? Constants.API_BASE_URL + "pago/add"
                            : Constants.API_BASE_URL + "pago/edit";

                    EasyHttp.post(getContext(), url)
                            .stringBody("application/json; charset=utf-8", obj.toString())
                            .build()
                            .getJsonAsObject(PagoRespuesta.class, new JsonResponseListener<PagoRespuesta>() {
                                @Override
                                public void onSuccess(@NonNull Headers headers, @NonNull PagoRespuesta resp) {
                                    if (!resp.isError()) {
                                        Pago res = resp.getData();
                                        res.setUsuario(usuario);
                                        res.setPedido(pedido);
                                        if (pagoExistente != null) {
                                            pedido.editPago(res);
                                            Toast.makeText(getContext(), "Pago editado", Toast.LENGTH_SHORT).show();
                                        } else {
                                            pedido.addPago(res);
                                            Toast.makeText(getContext(), "Pago agregado", Toast.LENGTH_SHORT).show();
                                        }
                                        pagoAdapter.notifyDataSetChanged();
                                        actualizarVistaPedido();
                                    } else {
                                        Toast.makeText(getContext(), resp.getMensaje(), Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onError(@NonNull HttpException e) {
                                    Toast.makeText(getContext(), "Error guardando pago", Toast.LENGTH_SHORT).show();
                                }
                            });
                });

        if (pagoExistente != null) {
            builder.setNeutralButton("Eliminar", (dialog, which) -> {
                confirmarEliminarPago(pagoExistente);
            });
        }

        builder.setNegativeButton("Cancelar", null);
        builder.create().show();
    }

    private void confirmarEliminarPago(Pago pago) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar pago")
                .setMessage("¿Deseas eliminar este pago?")
                .setPositiveButton("Eliminar", (d, w) -> {
                    JSONObject obj = new JSONObject();
                    try {
                        obj.put("id", pago.getId());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    EasyHttp.post(getContext(), Constants.API_BASE_URL + "pago/delete")
                            .stringBody("application/json; charset=utf-8", obj.toString())
                            .build()
                            .getJsonAsObject(PagoRespuesta.class, new JsonResponseListener<PagoRespuesta>() {
                                @Override
                                public void onSuccess(@NonNull Headers headers, @NonNull PagoRespuesta resp) {
                                    if (!resp.isError()) {
                                        Pago res = resp.getData();
                                        res.setUsuario(usuario);
                                        res.setPedido(pedido);
                                        pedido.deletePago(res);
                                        pagoAdapter.notifyDataSetChanged();
                                        actualizarVistaPedido();
                                        Toast.makeText(getContext(), "Pago eliminado", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                @Override
                                public void onError(@NonNull HttpException e) {
                                    Toast.makeText(getContext(), "Error eliminando pago", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void actualizarFila(TableRow row, ProductoPedido pp) {
        TextView tvCant = row.findViewWithTag("cantidad");
        tvCant.setText(String.valueOf(pp.getCantidad()));
        TextView tvTotal = row.findViewWithTag("total");
        tvTotal.setText(String.valueOf(pp.getTotal()));
        MaterialButton btnEdit = row.findViewWithTag("btnEdit");
        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> dialogoEditarProductoPedido(pp));
        }
    }

    private void cargarProductos() {
        EasyHttp.get(getContext(), Constants.API_BASE_URL + "productos/activos")
                .build()
                .getAsString(new StringResponseListener() {
                    @Override
                    public void onSuccess(@NonNull Headers headers, @NonNull String body) {
                        try {
                            Type tipoLista = new TypeToken<RespuestaLista<Producto>>() {}.getType();
                            RespuestaLista<Producto> respuesta = new Gson().fromJson(body, tipoLista);
                            if (!respuesta.isError()) {
                                listaProductos.clear();
                                listaProductos.addAll(respuesta.getData());
                                nombresProductos.clear();

                                for (Producto p : listaProductos) {
                                    nombresProductos.add(p.getNombre());
                                    Log.d("INFO", "Producto cargado: " + p.getNombre());
                                }
                                if(productoAdapter != null)
                                    productoAdapter.notifyDataSetChanged();
                            } else {
                                Toast.makeText(getContext(),
                                        "Error: " + respuesta.getMensaje(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e("Error", "Error parseando JSON", e);
                        }
                    }
                    @Override
                    public void onError(@NonNull HttpException e) {
                        Toast.makeText(getContext(), "Error cargando productos", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void actualizarTotal(EditText etPrecio, TextView tvCantidad, TextView tvTotal) {
        String precioStr = etPrecio.getText().toString().trim();
        if (!precioStr.isEmpty()) {
            double precio = Double.parseDouble(precioStr);
            int cantidad = Integer.parseInt((String) tvCantidad.getText());
            double total = precio * cantidad;
            tvTotal.setText("Total: " + total);
        }
    }

    private void actualizarVistaPedido() {
        tvTotalPedido.setText(pedido.getTotal() + "");
        tvPagadoPedido.setText(pedido.getPagado() + "");
        tvSaldoPedido.setText(pedido.getSaldo() + "");
    }
}