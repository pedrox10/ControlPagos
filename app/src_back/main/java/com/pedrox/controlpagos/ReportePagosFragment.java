package com.pedrox.controlpagos;

import static androidx.constraintlayout.motion.utils.Oscillator.TAG;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.jimmyworks.easyhttp.EasyHttp;
import com.jimmyworks.easyhttp.builder.RequestBuilder;
import com.jimmyworks.easyhttp.exception.HttpException;
import com.jimmyworks.easyhttp.listener.StringResponseListener;
import com.pedrox.controlpagos.adapters.ReportePagosAdapter;
import com.pedrox.controlpagos.models.Pago;
import com.pedrox.controlpagos.models.TotalPorCobrador;
import com.pedrox.controlpagos.responses.RespuestaReportePagos;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Headers;

public class ReportePagosFragment extends Fragment {

    TextView tvTotalGeneral;
    TextView tvRangoFechas;
    LinearLayout layoutTotalesCobradores;
    private RecyclerView rv;
    private ReportePagosAdapter reportePagosAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_reporte_pagos, container, false);
        tvTotalGeneral           = v.findViewById(R.id.tvTotalGeneral);
        tvRangoFechas            = v.findViewById(R.id.tvRangoFechas);
        layoutTotalesCobradores  = v.findViewById(R.id.layoutTotalesCobradores);
        rv = v.findViewById(R.id.rvReportePagos);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        DividerItemDecoration divider = new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL);
        divider.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.divider_1px));
        rv.addItemDecoration(divider);
        reportePagosAdapter = new ReportePagosAdapter(new ArrayList<>());
        rv.setAdapter(reportePagosAdapter);

        String inicio = getArguments().getString("inicio");
        String fin    = getArguments().getString("fin");
        cargarPagos(inicio, fin);
        return v;
    }

    private void cargarPagos(String ini, String fin) {
        String url = Constants.API_BASE_URL + "pagos/ini/" + ini + "/fin/" + fin;

        // ← interceptor agregado
        RequestBuilder builder = EasyHttp.get(getContext(), url);
        builder.getOkHttpClientBuilder()
               .addInterceptor(HttpClient.interceptor(getContext()));

        builder.build().getAsString(new StringResponseListener() {
            @Override
            public void onSuccess(@NonNull Headers headers, @NonNull String body) {
                try {
                    Type tipo = new TypeToken<RespuestaReportePagos>() {}.getType();
                    RespuestaReportePagos respuesta = new Gson().fromJson(body, tipo);
                    if (respuesta.isError()) {
                        Log.e(TAG, "Error: " + respuesta.getMensaje());
                        return;
                    }
                    tvTotalGeneral.setText("Total General: " + respuesta.getTotal_general() + " Bs");

                    String[] start = ini.split("-");
                    String[] end   = fin.split("-");
                    String fechaIni = String.format(Locale.getDefault(), "%02d/%02d/%04d",
                            Integer.parseInt(start[2]), Integer.parseInt(start[1]), Integer.parseInt(start[0]));
                    String fechaFin = String.format(Locale.getDefault(), "%02d/%02d/%04d",
                            Integer.parseInt(end[2]), Integer.parseInt(end[1]), Integer.parseInt(end[0]));
                    tvRangoFechas.setText("Desde " + fechaIni + " hasta " + fechaFin);

                    layoutTotalesCobradores.removeAllViews();
                    for (TotalPorCobrador tpc : respuesta.getTotales_por_cobrador()) {
                        TextView txt = new TextView(getContext());
                        txt.setText(tpc.getNombre() + ": " + tpc.getTotal() + " Bs");
                        txt.setTextSize(14);
                        txt.setPadding(0, 4, 0, 4);
                        layoutTotalesCobradores.addView(txt);
                    }

                    List<Pago> pagos = respuesta.getData();
                    reportePagosAdapter.updatePagos(pagos);

                } catch (Exception e) {
                    Log.e("Error", "Parseo JSON pagos", e);
                }
            }

            @Override
            public void onError(@NonNull HttpException e) {
                Toast.makeText(getContext(), "Error cargando pagos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
