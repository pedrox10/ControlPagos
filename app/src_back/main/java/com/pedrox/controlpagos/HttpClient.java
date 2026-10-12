package com.pedrox.controlpagos;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.jimmyworks.easyhttp.EasyHttp;
import com.jimmyworks.easyhttp.builder.RequestBuilder;

import okhttp3.Request;
import okhttp3.Response;

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

    public static okhttp3.Interceptor interceptor(Context context) {
        return chain -> {
            Request original = chain.request();
            Request request = original.newBuilder()
                    .header("X-Empresa-DB", Constants.EMPRESA_DB)
                    .header("Content-Type", "application/json")
                    .build();

            // Log temporal — verificar que el header se agrega
            Log.d("HttpClient", "X-Empresa-DB: " + request.header("X-Empresa-DB"));
            Log.d("HttpClient", "URL: " + request.url());

            Response response = chain.proceed(request);

            if (response.code() == 401) {
                AppSession.logout(context.getApplicationContext());
                LocalBroadcastManager.getInstance(context.getApplicationContext())
                        .sendBroadcast(new Intent(ACTION_SESSION_EXPIRED));
            }

            return response;
        };
    }
}
