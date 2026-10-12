package com.pedrox.controlpagos;

import static androidx.constraintlayout.motion.utils.Oscillator.TAG;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.jimmyworks.easyhttp.EasyHttp;
import com.jimmyworks.easyhttp.builder.BodyRequestBuilder;
import com.jimmyworks.easyhttp.exception.HttpException;
import com.jimmyworks.easyhttp.listener.JsonResponseListener;
import com.pedrox.controlpagos.models.Usuario;
import com.pedrox.controlpagos.responses.UsuarioRespuesta;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Headers;


public class LoginFragment extends Fragment {

    private TextInputEditText etUsername, etPassword;
    private static final String AUTH_URL = "https://lime-chamois-834233.hostingersite.com/rest/auth";
    MaterialButton btnLogin;

    public LoginFragment() {
        // Constructor vacío obligatorio
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etUsername = view.findViewById(R.id.etUsername);
        etPassword = view.findViewById(R.id.etPassword);
        btnLogin = view.findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            String alias = etUsername.getText().toString().trim();
            String clave = etPassword.getText().toString().trim();
            if (alias.isEmpty() || clave.isEmpty()) {
                Toast.makeText(this.getContext(), "Por favor, ingresa tu alias y clave.", Toast.LENGTH_SHORT).show();
                return;
            }
            authenticateUser(alias, clave, view);
        });
    }

    private void authenticateUser(String alias, String clave, View view) {
        btnLogin.setEnabled(false);
        // Guardar contexto ANTES de entrar al callback asíncrono
        Context ctx = getContext();
        if (ctx == null) return; // fragment no adjunto, no hacer nada
        JSONObject obj = new JSONObject();
        try {
            obj.put("alias", alias);
            obj.put("clave", clave);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // LoginFragment.java
// En lugar de HttpClient.post(), usar EasyHttp.post() directamente
// y agregar el interceptor al OkHttpClient

        BodyRequestBuilder builder = EasyHttp.post(ctx, Constants.API_BASE_URL + "auth");
        builder.getOkHttpClientBuilder()
                .addInterceptor(HttpClient.interceptor(ctx));
        builder.stringBody("application/json; charset=utf-8", obj.toString())
                .build()
                .getJsonAsObject(UsuarioRespuesta.class, new JsonResponseListener<UsuarioRespuesta>() {
                    @Override
                    public void onSuccess(@NonNull Headers headers, @NonNull UsuarioRespuesta respuesta) {
                        if (respuesta.isError()) {
                            Toast.makeText(ctx, respuesta.getMensaje(), Toast.LENGTH_SHORT).show();
                        } else {
                            System.out.println("loginnn");
                            AppSession.guardar(ctx, respuesta.getData());
                            Navigation.findNavController(view)
                                    .navigate(R.id.action_loginFragment_to_clientesFragment);
                        }
                        btnLogin.setEnabled(true);
                    }
                    @Override
                    public void onError(@NonNull HttpException e) {
                        System.out.println(e);
                        Toast.makeText(ctx, "Error de red o servidor", Toast.LENGTH_SHORT).show();
                        btnLogin.setEnabled(true);
                    }
                });

    }
}

