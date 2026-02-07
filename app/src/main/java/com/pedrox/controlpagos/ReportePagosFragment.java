package com.pedrox.controlpagos;

import static androidx.constraintlayout.motion.utils.Oscillator.TAG;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.jimmyworks.easyhttp.EasyHttp;
import com.jimmyworks.easyhttp.exception.HttpException;
import com.jimmyworks.easyhttp.listener.StringResponseListener;
import com.pedrox.controlpagos.adapters.ReportePagosAdapter;
import com.pedrox.controlpagos.models.Pago;
import com.pedrox.controlpagos.models.Pedido;
import com.pedrox.controlpagos.models.TotalPorCobrador;
import com.pedrox.controlpagos.responses.RespuestaLista;
import com.pedrox.controlpagos.responses.RespuestaReportePagos;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
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
        tvTotalGeneral = v.findViewById(R.id.tvTotalGeneral);
        tvRangoFechas = v.findViewById(R.id.tvRangoFechas);
        layoutTotalesCobradores = v.findViewById(R.id.layoutTotalesCobradores);
        rv = v.findViewById(R.id.rvReportePagos);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        DividerItemDecoration divider = new DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
        );
        divider.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.divider_1px));
        rv.addItemDecoration(divider);
        reportePagosAdapter = new ReportePagosAdapter(new ArrayList<>());
        rv.setAdapter(reportePagosAdapter);
        String inicio = getArguments().getString("inicio");
        String fin = getArguments().getString("fin");
        Log.d("Info", "onCreateView: " + inicio);
        Log.d("Info", "onCreateView: " + fin);
        cargarPagos(inicio, fin);
        return v;
    }

    private void cargarPagos(String ini, String fin) {
        String url = Constants.API_BASE_URL + "pagos/ini/" + ini + "/fin/" + fin;
        EasyHttp.get(getContext(), url)
                .build()
                .getAsString(new StringResponseListener() {
                    @Override
                    public void onSuccess(@NonNull Headers headers, @NonNull String body) {
                        try {
                            Type tipoLista = new TypeToken<RespuestaReportePagos>(){}.getType();
                            RespuestaReportePagos respuesta =
                                    new Gson().fromJson(body, tipoLista);
                            if (respuesta.isError()) {
                                Log.e(TAG, "Error: " + respuesta.getMensaje());
                            } else {
                                tvTotalGeneral.setText("Total General: " + respuesta.getTotal_general() + " Bs");
                                String[] start = ini.split("-");
                                String[] end = fin.split("-");
                                int diaIni = Integer.parseInt(start[2]);
                                int mesIni = Integer.parseInt(start[1]);
                                int añoIni = Integer.parseInt(start[0]);
                                int diaFin = Integer.parseInt(end[2]);
                                int mesFin = Integer.parseInt(end[1]);
                                int añoFin = Integer.parseInt(end[0]);
                                String fechaIni = String.format(Locale.getDefault(), "%02d/%02d/%04d", diaIni, mesIni, añoIni);
                                String fechaFin = String.format(Locale.getDefault(), "%02d/%02d/%04d", diaFin, mesFin, añoFin);
                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                tvRangoFechas.setText("Desde " + fechaIni +" hasta " + fechaFin);
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

                                for (TotalPorCobrador t : respuesta.getTotales_por_cobrador()) {
                                    Log.d("INFO", t.getNombre() + " -> " + t.getTotal());
                                }
                                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                                Log.d(TAG, "Respuesta completa:\n" + gson.toJson(respuesta));
                            }
                        } catch (Exception e) {
                            Log.e("Error", "Parseo JSON pagos", e);
                        }
                    }
                    @Override
                    public void onError(@NonNull HttpException e) {
                        Toast.makeText(getContext(), "Error cargando pagos " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

