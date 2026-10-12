package com.pedrox.controlpagos.adapters;

import static androidx.constraintlayout.motion.utils.Oscillator.TAG;

import android.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.jimmyworks.easyhttp.EasyHttp;
import com.jimmyworks.easyhttp.builder.BodyRequestBuilder;
import com.jimmyworks.easyhttp.exception.HttpException;
import com.jimmyworks.easyhttp.listener.JsonResponseListener;
import com.pedrox.controlpagos.Constants;
import com.pedrox.controlpagos.HttpClient;
import com.pedrox.controlpagos.R;
import com.pedrox.controlpagos.models.Producto;
import com.pedrox.controlpagos.models.Usuario;
import com.pedrox.controlpagos.responses.ProductoRespuesta;
import com.pedrox.controlpagos.responses.UsuarioRespuesta;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import okhttp3.Headers;

public class ProductoAdapter extends RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder> {

    private List<Producto> listaProductos;


    public ProductoAdapter(List<Producto> listaProductos) {
        this.listaProductos = listaProductos;
    }

    public void updateProductos(List<Producto> productos) {
        this.listaProductos.clear();
        this.listaProductos.addAll(productos);
        notifyDataSetChanged();
    }

    @Override
    public ProductoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_producto, parent, false);
        return new ProductoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductoAdapter.ProductoViewHolder holder, int position) {
        Producto producto = listaProductos.get(position);
        int pos = holder.getAdapterPosition();
        holder.tvNombreProducto.setText(producto.getNombre());
        holder.tvEstadoProducto.setText(producto.getHabilitado() == 1 ? "Si" : "No");
        holder.btnEditarProducto.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            LayoutInflater inflater = LayoutInflater.from(v.getContext());
            View view = inflater.inflate(R.layout.dialog_accion_producto, null);
            EditText etNombre = view.findViewById(R.id.etNombreProducto);
            SwitchMaterial swHabilitado = view.findViewById(R.id.swProductoHabilitado);
            // Prellenar datos
            etNombre.setText(producto.getNombre());
            swHabilitado.setChecked(producto.getHabilitado() == 1);
            builder.setView(view)
                    .setTitle("Editar producto")
                    .setPositiveButton("Guardar", (dialog, id) -> {
                        String nombre = etNombre.getText().toString().trim();
                        if(!nombre.equals("")) {
                            boolean habilitado = swHabilitado.isChecked();
                            JSONObject obj = new JSONObject();
                            try {
                                obj.put("id", producto.getId());
                                obj.put("nombre", nombre);
                                obj.put("habilitado", habilitado);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            holder.btnEditarProducto.setEnabled(false);
                            holder.btnEditarProducto.setAlpha(0.5f);
                            BodyRequestBuilder rb = EasyHttp.post(v.getContext(), Constants.API_BASE_URL + "producto/edit");
                            rb.getOkHttpClientBuilder().addInterceptor(HttpClient.interceptor(view.getContext()));
                            rb.stringBody("application/json; charset=utf-8", obj.toString())
                                    .build()
                                    .getJsonAsObject(ProductoRespuesta.class, new JsonResponseListener<ProductoRespuesta>() {
                                        @Override
                                        public void onSuccess(@NonNull Headers headers, @NonNull ProductoRespuesta respuesta) {
                                            if (respuesta.isError()) {
                                                Log.d(TAG, respuesta.getMensaje());
                                            } else {
                                                Producto productoActualizado = respuesta.getData();
                                                listaProductos.set(pos, productoActualizado);
                                                notifyItemChanged(pos);
                                                Toast.makeText(v.getContext(), respuesta.getMensaje(), Toast.LENGTH_SHORT).show();
                                                Log.d(TAG, "onSuccess: " + productoActualizado.toString());
                                                holder.btnEditarProducto.setEnabled(true);
                                                holder.btnEditarProducto.setAlpha(1f);
                                            }
                                        }
                                        @Override
                                        public void onError(@NonNull HttpException e) {
                                            Log.d(TAG, e.getMessage());
                                            Toast.makeText(v.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                            holder.btnEditarProducto.setEnabled(true);
                                            holder.btnEditarProducto.setAlpha(1f);
                                        }
                                    });
                        } else {
                            Toast.makeText(v.getContext(), "¡El nombre no puede ser vacio!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancelar", (dialog, id) -> dialog.dismiss());
            AlertDialog dialog = builder.create();
            dialog.show();
        });
        holder.btnEliminarProducto.setOnClickListener(view -> {
            new androidx.appcompat.app.AlertDialog.Builder(view.getContext())
                    .setTitle("Eliminar Producto")
                    .setMessage("¿Seguro que deseas eliminar a " + producto.getNombre() + "?")
                    .setPositiveButton("Eliminar", (dialog, which) -> {
                        JSONObject obj = new JSONObject();
                        try {
                            obj.put("id", producto.getId());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        BodyRequestBuilder rb = EasyHttp.post(view.getContext(), Constants.API_BASE_URL + "producto/delete");
                        rb.getOkHttpClientBuilder().addInterceptor(HttpClient.interceptor(view.getContext()));
                        rb.stringBody("application/json; charset=utf-8", obj.toString())
                                .build()
                                .getJsonAsObject(ProductoRespuesta.class, new JsonResponseListener<ProductoRespuesta>() {
                                    @Override
                                    public void onSuccess(@NonNull Headers headers, @NonNull ProductoRespuesta respuesta) {
                                        if (!respuesta.isError()) {
                                            Toast.makeText(view.getContext(), "Producto eliminado", Toast.LENGTH_SHORT).show();
                                            listaProductos.remove(pos);
                                            notifyItemRemoved(pos);
                                        } else {
                                            Toast.makeText(view.getContext(), respuesta.getMensaje(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                    @Override
                                    public void onError(@NonNull HttpException e) {
                                        Toast.makeText(view.getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    })
                    .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    @Override
    public int getItemCount() {
       return listaProductos.size();
    }

    static class ProductoViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombreProducto;
        TextView tvEstadoProducto;
        MaterialButton btnEditarProducto;
        MaterialButton btnEliminarProducto;

        public ProductoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreProducto = itemView.findViewById(R.id.tvNombreProducto);
            tvEstadoProducto = itemView.findViewById(R.id.tvEstadoProducto);
            btnEditarProducto = itemView.findViewById(R.id.btnEditarProducto);
            btnEliminarProducto = itemView.findViewById(R.id.btnEliminarProducto);
        }
    }

    public void addProducto(Producto producto) {
        int position = listaProductos.size();   // última posición
        listaProductos.add(producto);
        notifyItemInserted(position);
    }
}
