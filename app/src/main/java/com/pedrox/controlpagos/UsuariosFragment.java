package com.pedrox.controlpagos;

import static androidx.constraintlayout.motion.utils.Oscillator.TAG;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import android.app.AlertDialog;
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
import com.pedrox.controlpagos.adapters.UsuarioAdapter;
import com.pedrox.controlpagos.models.Cliente;
import com.pedrox.controlpagos.models.Usuario;
import com.pedrox.controlpagos.responses.ClienteRespuesta;
import com.pedrox.controlpagos.responses.RespuestaLista;
import com.pedrox.controlpagos.responses.UsuarioRespuesta;

import java.lang.reflect.Type;
import java.util.ArrayList;
import okhttp3.Headers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;

import org.json.JSONException;
import org.json.JSONObject;

public class UsuariosFragment extends Fragment {
    TextView tvNombre;
    TextView tvAlias;
    TextView tvRol;
    private RecyclerView rvUsuarios;
    private UsuarioAdapter usuarioAdapter;
    private FloatingActionButton fabAddUsuario;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_admin_usuarios, container, false);
        tvNombre = v.findViewById(R.id.tvNombre);
        tvAlias = v.findViewById(R.id.tvAlias);
        tvRol = v.findViewById(R.id.tvRol);
        fabAddUsuario = v.findViewById(R.id.fabAddUsuario);
        rvUsuarios = v.findViewById(R.id.rvUsuarios);
        rvUsuarios.setLayoutManager(new LinearLayoutManager(getContext()));
        DividerItemDecoration divider = new DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
        );
        divider.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.divider_1px));
        rvUsuarios.addItemDecoration(divider);
        usuarioAdapter = new UsuarioAdapter(new ArrayList<>());
        rvUsuarios.setAdapter(usuarioAdapter);
        cargarUsuarios();
        fabAddUsuario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mostrarAgregarUsuario();
            }
        });
        return v;
    }

    private void cargarUsuarios(){
        EasyHttp.get(getContext(), Constants.API_BASE_URL + "usuarios")
                .build()
                .getAsString(new StringResponseListener() {
                    @Override
                    public void onSuccess(@NonNull Headers headers, @NonNull String body) {
                        try {
                            Type tipoLista = new TypeToken<RespuestaLista<Usuario>>() {}.getType();
                            RespuestaLista<Usuario> respuesta = new Gson().fromJson(body, tipoLista);
                            if (respuesta.isError()) {
                                Log.e(TAG, "Error: " + respuesta.getMensaje());
                            } else {
                                Log.d(TAG, "Usuarios recibidos: " + respuesta.getData());
                                usuarioAdapter.updateUsuarios(respuesta.getData());
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

    private void mostrarAgregarUsuario() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View view = inflater.inflate(R.layout.dialog_accion_usuario, null);
        EditText etNombre = view.findViewById(R.id.etNombreUsuario);
        EditText etAlias = view.findViewById(R.id.etAliasUsuario);
        EditText etClave = view.findViewById(R.id.etClaveUsuario);
        Spinner spRol = view.findViewById(R.id.spRol);
        spRol.setSelection(0);
        SwitchMaterial swHabilitado = view.findViewById(R.id.swHabilitado);
        builder.setView(view)
                .setTitle("Agregar usuario")
                .setPositiveButton("Guardar", (dialog, id) -> {
                    String nombre = etNombre.getText().toString().trim();
                    if(!nombre.equals("")) {
                        String alias = etAlias.getText().toString().trim();
                        String clave = etClave.getText().toString().trim();
                        String rol = spRol.getSelectedItem().toString();
                        boolean habilitado = swHabilitado.isChecked();
                        JSONObject obj = new JSONObject();
                        try {
                            obj.put("nombre", nombre);
                            obj.put("alias", alias);
                            obj.put("clave", clave);
                            obj.put("rol", rol);
                            obj.put("habilitado", habilitado);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        EasyHttp.post(getContext(), Constants.API_BASE_URL + "usuario/add")
                                .stringBody("application/json; charset=utf-8", obj.toString())
                                .build()
                                .getJsonAsObject(UsuarioRespuesta.class, new JsonResponseListener<UsuarioRespuesta>() {
                                    @Override
                                    public void onSuccess(@NonNull Headers headers, @NonNull UsuarioRespuesta respuesta) {
                                        if (respuesta.isError()) {
                                            Log.d(TAG, respuesta.getMensaje());
                                        } else {
                                            Usuario usuarioNuevo = respuesta.getData();
                                            usuarioAdapter.addUsuario(usuarioNuevo);
                                            Toast.makeText(getContext(), "usuario agregado", Toast.LENGTH_SHORT).show();
                                            Log.d(TAG, "onSuccess: " + usuarioNuevo.toString());
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
