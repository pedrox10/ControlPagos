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
import com.pedrox.controlpagos.adapters.UsuarioAdapter;
import com.pedrox.controlpagos.models.Usuario;
import com.pedrox.controlpagos.responses.RespuestaLista;
import com.pedrox.controlpagos.responses.UsuarioRespuesta;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;

import okhttp3.Headers;

public class UsuariosFragment extends Fragment {

    private RecyclerView rvUsuarios;
    private UsuarioAdapter usuarioAdapter;
    private FloatingActionButton fabAddUsuario;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_admin_usuarios, container, false);
        fabAddUsuario = v.findViewById(R.id.fabAddUsuario);
        rvUsuarios    = v.findViewById(R.id.rvUsuarios);
        rvUsuarios.setLayoutManager(new LinearLayoutManager(getContext()));
        DividerItemDecoration divider = new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL);
        divider.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.divider_1px));
        rvUsuarios.addItemDecoration(divider);
        usuarioAdapter = new UsuarioAdapter(new ArrayList<>());
        rvUsuarios.setAdapter(usuarioAdapter);
        cargarUsuarios();
        fabAddUsuario.setOnClickListener(view -> mostrarAgregarUsuario());
        return v;
    }

    private void cargarUsuarios() {
        // ← interceptor agregado
        RequestBuilder builder = EasyHttp.get(getContext(), Constants.API_BASE_URL + "usuarios");
        builder.getOkHttpClientBuilder().addInterceptor(HttpClient.interceptor(getContext()));
        builder.build().getAsString(new StringResponseListener() {
            @Override
            public void onSuccess(@NonNull Headers headers, @NonNull String body) {
                try {
                    Type tipo = new TypeToken<RespuestaLista<Usuario>>() {}.getType();
                    RespuestaLista<Usuario> respuesta = new Gson().fromJson(body, tipo);
                    if (!respuesta.isError()) {
                        usuarioAdapter.updateUsuarios(respuesta.getData());
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

    private void mostrarAgregarUsuario() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_accion_usuario, null);
        EditText etNombre        = view.findViewById(R.id.etNombreUsuario);
        EditText etAlias         = view.findViewById(R.id.etAliasUsuario);
        EditText etClave         = view.findViewById(R.id.etClaveUsuario);
        Spinner  spRol           = view.findViewById(R.id.spRol);
        SwitchMaterial swHabilitado = view.findViewById(R.id.swHabilitado);
        spRol.setSelection(0);

        builder.setView(view)
                .setTitle("Agregar usuario")
                .setPositiveButton("Guardar", (dialog, id) -> {
                    String nombre = etNombre.getText().toString().trim();
                    if (nombre.isEmpty()) {
                        Toast.makeText(getContext(), "¡El nombre no puede ser vacío!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    JSONObject obj = new JSONObject();
                    try {
                        obj.put("nombre",     nombre);
                        obj.put("alias",      etAlias.getText().toString().trim());
                        obj.put("clave",      etClave.getText().toString().trim());
                        obj.put("rol",        spRol.getSelectedItem().toString());
                        obj.put("habilitado", swHabilitado.isChecked());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    // ← interceptor agregado
                    BodyRequestBuilder rb = EasyHttp.post(getContext(), Constants.API_BASE_URL + "usuario/add");
                    rb.getOkHttpClientBuilder().addInterceptor(HttpClient.interceptor(getContext()));
                    rb.stringBody("application/json; charset=utf-8", obj.toString())
                            .build()
                            .getJsonAsObject(UsuarioRespuesta.class, new JsonResponseListener<UsuarioRespuesta>() {
                                @Override
                                public void onSuccess(@NonNull Headers headers, @NonNull UsuarioRespuesta respuesta) {
                                    if (!respuesta.isError()) {
                                        usuarioAdapter.addUsuario(respuesta.getData());
                                        Toast.makeText(getContext(), "Usuario agregado", Toast.LENGTH_SHORT).show();
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
