package com.pedrox.controlpagos;

import static androidx.constraintlayout.motion.utils.Oscillator.TAG;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jimmyworks.easyhttp.EasyHttp;
import com.jimmyworks.easyhttp.builder.BodyRequestBuilder;
import com.jimmyworks.easyhttp.builder.RequestBuilder;
import com.jimmyworks.easyhttp.exception.HttpException;
import com.jimmyworks.easyhttp.listener.JsonResponseListener;
import com.jimmyworks.easyhttp.listener.StringResponseListener;
import com.pedrox.controlpagos.adapters.ProductoAdapter;
import com.pedrox.controlpagos.models.Producto;
import com.pedrox.controlpagos.responses.ProductoRespuesta;
import com.pedrox.controlpagos.responses.RespuestaLista;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;

import okhttp3.Headers;

public class ProductosFragment extends Fragment {

    private RecyclerView rvProductos;
    private ProductoAdapter productoAdapter;
    private FloatingActionButton fabAddProducto;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_admin_productos, container, false);
        fabAddProducto = v.findViewById(R.id.fabAddProducto);
        rvProductos    = v.findViewById(R.id.rvProductos);
        rvProductos.setLayoutManager(new LinearLayoutManager(getContext()));
        DividerItemDecoration divider = new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL);
        divider.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.divider_1px));
        rvProductos.addItemDecoration(divider);
        productoAdapter = new ProductoAdapter(new ArrayList<>());
        rvProductos.setAdapter(productoAdapter);
        cargarProductos();
        fabAddProducto.setOnClickListener(view -> mostrarAgregarProducto());
        return v;
    }

    private void cargarProductos() {
        // ← interceptor agregado
        RequestBuilder builder = EasyHttp.get(getContext(), Constants.API_BASE_URL + "productos");
        builder.getOkHttpClientBuilder().addInterceptor(HttpClient.interceptor(getContext()));
        builder.build().getAsString(new StringResponseListener() {
            @Override
            public void onSuccess(@NonNull Headers headers, @NonNull String body) {
                try {
                    Type tipo = new TypeToken<RespuestaLista<Producto>>() {}.getType();
                    RespuestaLista<Producto> respuesta = new Gson().fromJson(body, tipo);
                    if (!respuesta.isError()) {
                        productoAdapter.updateProductos(respuesta.getData());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parseando JSON", e);
                }
            }
            @Override
            public void onError(@NonNull HttpException e) {
                Log.e(TAG, "Error HTTP: " + e.getMessage());
            }
        });
    }

    private void mostrarAgregarProducto() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_accion_producto, null);
        EditText etNombre        = view.findViewById(R.id.etNombreProducto);
        SwitchMaterial swHabilitado = view.findViewById(R.id.swProductoHabilitado);
        builder.setView(view)
                .setTitle("Agregar producto")
                .setPositiveButton("Guardar", (dialog, id) -> {
                    String nombre = etNombre.getText().toString().trim();
                    if (nombre.isEmpty()) {
                        Toast.makeText(getContext(), "¡El nombre no puede ser vacío!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    JSONObject obj = new JSONObject();
                    try {
                        obj.put("nombre",     nombre);
                        obj.put("habilitado", swHabilitado.isChecked());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    // ← interceptor agregado
                    BodyRequestBuilder rb = EasyHttp.post(getContext(), Constants.API_BASE_URL + "producto/add");
                    rb.getOkHttpClientBuilder().addInterceptor(HttpClient.interceptor(getContext()));
                    rb.stringBody("application/json; charset=utf-8", obj.toString())
                            .build()
                            .getJsonAsObject(ProductoRespuesta.class, new JsonResponseListener<ProductoRespuesta>() {
                                @Override
                                public void onSuccess(@NonNull Headers headers, @NonNull ProductoRespuesta respuesta) {
                                    if (!respuesta.isError()) {
                                        productoAdapter.addProducto(respuesta.getData());
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
