package com.pedrox.controlpagos;

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
import java.util.List;

import okhttp3.Headers;

public class CarteraVivaFragment extends Fragment {

    private static final String TAG = "CarteraVivaFragment";

    private TextView tvTotalCartera;
    private TextView tvTotalClientes;
    private TextView tvTotalMorosos;       // NUEVO — saldo total de morosos
    private TextView tvCantidadMorosos;    // NUEVO — cuántos morosos
    private TextView tvTotalCarteraPie;    // NUEVO — total al pie de tabla
    private RecyclerView rvCartera;
    private CarteraVivaAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_cartera_viva, container, false);

        tvTotalCartera    = v.findViewById(R.id.tvTotalCartera);
        tvTotalClientes   = v.findViewById(R.id.tvTotalClientes);
        tvTotalMorosos    = v.findViewById(R.id.tvTotalMorosos);
        tvCantidadMorosos = v.findViewById(R.id.tvCantidadMorosos);
        tvTotalCarteraPie = v.findViewById(R.id.tvTotalCarteraPie);
        rvCartera         = v.findViewById(R.id.rvCartera);

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
                        List<CarteraCliente> lista = respuesta.getData();

                        double saldoMorosos    = 0;
                        int    cantidadMorosos = 0;
                        for (CarteraCliente c : lista) {
                            if (c.isEsMoroso()) {
                                saldoMorosos += c.getSaldoTotal();
                                cantidadMorosos++;
                            }
                        }
                        final double  _saldoMorosos    = saldoMorosos;
                        final int     _cantidadMorosos = cantidadMorosos;
                        final double  _totalGeneral    = respuesta.getTotalGeneral();

                        // ✅ UI siempre desde hilo principal
                        requireActivity().runOnUiThread(() -> {
                            if (!isAdded()) return; // Fragment ya no está activo

                            tvTotalCartera.setText("Bs " + String.format("%.1f", _totalGeneral));
                            tvTotalClientes.setText(lista.size() + " clientes con saldo");
                            tvTotalMorosos.setText("Bs " + String.format("%.1f", _saldoMorosos));
                            tvCantidadMorosos.setText(_cantidadMorosos + " cliente"
                                    + (_cantidadMorosos != 1 ? "s" : "") + " moroso"
                                    + (_cantidadMorosos != 1 ? "s" : ""));
                            tvTotalCarteraPie.setText("Bs " + String.format("%.1f", _totalGeneral));
                            adapter.updateCartera(lista);
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parseando cartera", e);
                }
            }

            @Override
            public void onError(@NonNull HttpException e) {
                Log.e(TAG, "Error HTTP cartera: " + e.getMessage());
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
            }
        });
    }
}