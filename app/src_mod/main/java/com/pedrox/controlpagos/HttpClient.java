package com.pedrox.controlpagos;

import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

/**
 * Cliente OkHttp compartido con interceptor global.
 * Agrega automáticamente el header X-Empresa-DB a cada request.
 * Si el servidor responde 401, emite un broadcast para cerrar sesión.
 *
 * Uso en cada ViewModel:
 *   RequestBuilder builder = EasyHttp.get(context, url);
 *   builder.getOkHttpClientBuilder()
 *          .addInterceptor(HttpClient.interceptor(context));
 *   builder.build().getAsString(...);
 */
public class HttpClient {

    public static final String ACTION_SESSION_EXPIRED = "SESSION_EXPIRED";

    /**
     * Retorna el interceptor que debe agregarse al OkHttpClient de cada request.
     */
    public static okhttp3.Interceptor interceptor(Context context) {
        return chain -> {
            // Agregar header de empresa a TODOS los requests
            Request original = chain.request();
            Request request = original.newBuilder()
                    .header("X-Empresa-DB", Constants.EMPRESA_DB)
                    .header("Content-Type", "application/json")
                    .build();

            Response response = chain.proceed(request);

            // Si el servidor rechaza la sesión, cerrar y redirigir al login
            if (response.code() == 401) {
                AppSession.logout(context.getApplicationContext());
                LocalBroadcastManager.getInstance(context.getApplicationContext())
                        .sendBroadcast(new Intent(ACTION_SESSION_EXPIRED));
            }

            return response;
        };
    }
}
