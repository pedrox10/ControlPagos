package com.pedrox.controlpagos;

import static androidx.constraintlayout.motion.utils.Oscillator.TAG;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jimmyworks.easyhttp.EasyHttp;
import com.jimmyworks.easyhttp.builder.RequestBuilder;
import com.jimmyworks.easyhttp.exception.HttpException;
import com.jimmyworks.easyhttp.listener.StringResponseListener;
import com.pedrox.controlpagos.adapters.CarteraVivaAdapter;
import com.pedrox.controlpagos.models.CarteraCliente;
import com.pedrox.controlpagos.responses.RespuestaCarteraViva;

import java.lang.reflect.Type;
import java.util.ArrayList;

import okhttp3.Headers;

public class CarteraVivaFragment extends Fragment {

    private TextView tvTotalCartera;
    private TextView tvTotalClientes;
    private RecyclerView rvCartera;
    private CarteraVivaAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_cartera_viva, container, false);
        tvTotalCartera  = v.findViewById(R.id.tvTotalCartera);
        tvTotalClientes = v.findViewById(R.id.tvTotalClientes);
        rvCartera       = v.findViewById(R.id.rvCartera);

        rvCartera.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CarteraVivaAdapter(new ArrayList<>());
        rvCartera.setAdapter(adapter);

        cargarCartera();
        return v;
    }

    private void cargarCartera() {
        RequestBuilder builder = EasyHttp.get(getContext(), Constants.API_BASE_URL + "cartera");
        builder.getOkHttpClientBuilder().addInterceptor(HttpClient.interceptor(getContext()));
        builder.build().getAsString(new StringResponseListener() {
            @Override
            public void onSuccess(@NonNull Headers headers, @NonNull String body) {
                try {
                    Type tipo = new TypeToken<RespuestaCarteraViva>() {}.getType();
                    RespuestaCarteraViva respuesta = new Gson().fromJson(body, tipo);
                    if (!respuesta.isError()) {
                        tvTotalCartera.setText("Bs " + String.format("%.1f", respuesta.getTotalGeneral()));
                        tvTotalClientes.setText(respuesta.getData().size() + " clientes con saldo");
                        adapter.updateCartera(respuesta.getData());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parseando cartera", e);
                }
            }
            @Override
            public void onError(@NonNull HttpException e) {
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
