package com.pedrox.controlpagos;

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

    private static final String TAG = "ReportePagosFragment";

    private TextView tvTotalGeneral;
    private TextView tvRangoFechas;
    private TextView tvTotalPie;          // NUEVO — total al pie de la tabla
    private LinearLayout layoutTotalesCobradores;
    private RecyclerView rv;
    private ReportePagosAdapter reportePagosAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_reporte_pagos, container, false);
        tvTotalGeneral          = v.findViewById(R.id.tvTotalGeneral);
        tvRangoFechas           = v.findViewById(R.id.tvRangoFechas);
        tvTotalPie              = v.findViewById(R.id.tvTotalPie);
        layoutTotalesCobradores = v.findViewById(R.id.layoutTotalesCobradores);

        rv = v.findViewById(R.id.rvReportePagos);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        DividerItemDecoration divider = new DividerItemDecoration(
                requireContext(), DividerItemDecoration.VERTICAL);
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
                    Log.d(TAG, "onSuccess: " + respuesta.getData().size());
                    // ── Total general en la card ──────────────────────────────
                    tvTotalGeneral.setText("Bs " + respuesta.getTotal_general());

                    // ── Total al pie de la tabla ──────────────────────────────
                    tvTotalPie.setText("Bs " + respuesta.getTotal_general());

                    // ── Rango de fechas centrado ──────────────────────────────
                    String[] start = ini.split("-");
                    String[] end   = fin.split("-");
                    String fechaIni = String.format(Locale.getDefault(), "%02d/%02d/%04d",
                            Integer.parseInt(start[2]),
                            Integer.parseInt(start[1]),
                            Integer.parseInt(start[0]));
                    String fechaFin = String.format(Locale.getDefault(), "%02d/%02d/%04d",
                            Integer.parseInt(end[2]),
                            Integer.parseInt(end[1]),
                            Integer.parseInt(end[0]));
                    tvRangoFechas.setText(fechaIni + " — " + fechaFin);

                    // ── Cobradores con icono y monto alineado ─────────────────
                    layoutTotalesCobradores.removeAllViews();
                    for (TotalPorCobrador tpc : respuesta.getTotales_por_cobrador()) {
                        LinearLayout fila = new LinearLayout(getContext());
                        fila.setOrientation(LinearLayout.HORIZONTAL);
                        LinearLayout.LayoutParams filaParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        filaParams.setMargins(0, 0, 0, 4);
                        fila.setLayoutParams(filaParams);

                        TextView tvNombre = new TextView(getContext());
                        LinearLayout.LayoutParams nombreParams = new LinearLayout.LayoutParams(
                                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                        tvNombre.setLayoutParams(nombreParams);
                        tvNombre.setText(tpc.getNombre());
                        tvNombre.setTextSize(12f);
                        tvNombre.setTextColor(
                                ContextCompat.getColor(requireContext(),
                                        android.R.color.tab_indicator_text));

                        TextView tvMonto = new TextView(getContext());
                        tvMonto.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT));
                        tvMonto.setText("Bs " + tpc.getTotal());
                        tvMonto.setTextSize(12f);
                        tvMonto.setTypeface(null, android.graphics.Typeface.BOLD);
                        tvMonto.setTextColor(
                                ContextCompat.getColor(requireContext(), R.color.colorSuccess));

                        fila.addView(tvNombre);
                        fila.addView(tvMonto);
                        layoutTotalesCobradores.addView(fila);
                    }

                    // ── Lista de pagos ────────────────────────────────────────
                    List<Pago> pagos = respuesta.getData();
                    reportePagosAdapter.updatePagos(pagos);

                } catch (Exception e) {
                    Log.e(TAG, "Error parseando JSON", e);
                }
            }

            @Override
            public void onError(@NonNull HttpException e) {
                Toast.makeText(getContext(),
                        "Error cargando pagos: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}