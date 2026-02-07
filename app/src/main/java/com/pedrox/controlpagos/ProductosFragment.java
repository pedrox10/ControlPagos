package com.pedrox.controlpagos;

import static androidx.constraintlayout.motion.utils.Oscillator.TAG;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
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
import com.jimmyworks.easyhttp.exception.HttpException;
import com.jimmyworks.easyhttp.listener.JsonResponseListener;
import com.jimmyworks.easyhttp.listener.StringResponseListener;
import com.pedrox.controlpagos.adapters.ProductoAdapter;
import com.pedrox.controlpagos.adapters.UsuarioAdapter;
import com.pedrox.controlpagos.models.Producto;
import com.pedrox.controlpagos.models.Usuario;
import com.pedrox.controlpagos.responses.ProductoRespuesta;
import com.pedrox.controlpagos.responses.RespuestaLista;
import com.pedrox.controlpagos.responses.UsuarioRespuesta;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;

import okhttp3.Headers;

public class ProductosFragment extends Fragment {
    TextView tvNombreProducto;
    TextView tvEstadoProducto;
    private RecyclerView rvProductos;
    private ProductoAdapter productoAdapter;
    private FloatingActionButton fabAddProducto;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_admin_productos, container, false);
        tvNombreProducto = v.findViewById(R.id.tvNombreProducto);
        tvEstadoProducto = v.findViewById(R.id.tvEstadoProducto);
        fabAddProducto = v.findViewById(R.id.fabAddProducto);
        rvProductos = v.findViewById(R.id.rvProductos);
        rvProductos.setLayoutManager(new LinearLayoutManager(getContext()));
        DividerItemDecoration divider = new DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
        );
        divider.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.divider_1px));
        rvProductos.addItemDecoration(divider);
        productoAdapter = new ProductoAdapter(new ArrayList<>());
        rvProductos.setAdapter(productoAdapter);
        cargarProductos();
        fabAddProducto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mostrarAgregarProducto();
            }
        });
        return v;
    }

    private void cargarProductos(){
        EasyHttp.get(getContext(), Constants.API_BASE_URL + "productos")
                .build()
                .getAsString(new StringResponseListener() {
                    @Override
                    public void onSuccess(@NonNull Headers headers, @NonNull String body) {
                        try {
                            Type tipoLista = new TypeToken<RespuestaLista<Producto>>() {}.getType();
                            RespuestaLista<Producto> respuesta = new Gson().fromJson(body, tipoLista);
                            if (respuesta.isError()) {
                                Log.e(TAG, "Error: " + respuesta.getMensaje());
                            } else {
                                Log.d(TAG, "Productos recibidos: " + respuesta.getData());
                                productoAdapter.updateProductos(respuesta.getData());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parseando JSON", e);
                        } finally {
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
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View view = inflater.inflate(R.layout.dialog_accion_producto, null);
        EditText etNombre = view.findViewById(R.id.etNombreProducto);
        SwitchMaterial swHabilitado = view.findViewById(R.id.swProductoHabilitado);
        builder.setView(view)
                .setTitle("Agregar producto")
                .setPositiveButton("Guardar", (dialog, id) -> {
                    String nombre = etNombre.getText().toString().trim();
                    if(!nombre.equals("")) {
                        boolean habilitado = swHabilitado.isChecked();
                        JSONObject obj = new JSONObject();
                        try {
                            obj.put("nombre", nombre);
                            obj.put("habilitado", habilitado);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        EasyHttp.post(getContext(), Constants.API_BASE_URL + "producto/add")
                                .stringBody("application/json; charset=utf-8", obj.toString())
                                .build()
                                .getJsonAsObject(ProductoRespuesta.class, new JsonResponseListener<ProductoRespuesta>() {
                                    @Override
                                    public void onSuccess(@NonNull Headers headers, @NonNull ProductoRespuesta respuesta) {
                                        if (respuesta.isError()) {
                                            Log.d(TAG, respuesta.getMensaje());
                                        } else {
                                            Producto productoNuevo = respuesta.getData();
                                            productoAdapter.addProducto(productoNuevo);
                                            Toast.makeText(getContext(), respuesta.getMensaje(), Toast.LENGTH_SHORT).show();
                                            Log.d(TAG, "onSuccess: " + productoNuevo.toString());
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
}
