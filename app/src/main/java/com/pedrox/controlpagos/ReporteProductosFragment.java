package com.pedrox.controlpagos;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.pedrox.controlpagos.adapters.ReporteProductosAdapter;
import com.pedrox.controlpagos.models.ProductoVendido;
import com.pedrox.controlpagos.responses.RespuestaProductosVendidos;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Headers;

public class ReporteProductosFragment extends Fragment {

    private static final String TAG = "ReporteProductosFragment";

    private TextView tvRangoFechas;
    private TextView tvTotalMonto;
    private TextView tvTotalUnidades;
    private TextView tvTotalUnidadesPie;
    private TextView tvTotalMontoPie;
    private RecyclerView rv;
    private ReporteProductosAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_reporte_productos, container, false);

        tvRangoFechas      = v.findViewById(R.id.tvRangoFechasProductos);
        tvTotalMonto       = v.findViewById(R.id.tvTotalMontoProductos);
        tvTotalUnidades    = v.findViewById(R.id.tvTotalUnidades);
        tvTotalUnidadesPie = v.findViewById(R.id.tvTotalUnidadesPie);
        tvTotalMontoPie    = v.findViewById(R.id.tvTotalMontoPie);

        rv = v.findViewById(R.id.rvProductosVendidos);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        DividerItemDecoration divider = new DividerItemDecoration(
                requireContext(), DividerItemDecoration.VERTICAL);
        divider.setDrawable(
                ContextCompat.getDrawable(requireContext(), R.drawable.divider_1px));
        rv.addItemDecoration(divider);

        adapter = new ReporteProductosAdapter(new ArrayList<>());
        rv.setAdapter(adapter);

        String inicio = getArguments().getString("inicio");
        String fin    = getArguments().getString("fin");
        cargarReporte(inicio, fin);

        return v;
    }

    private void cargarReporte(String ini, String fin) {
        String url = Constants.API_BASE_URL
                + "productos/vendidos/ini/" + ini + "/fin/" + fin;

        Context ctx = requireContext();
        RequestBuilder rb = EasyHttp.get(ctx, url);
        rb.getOkHttpClientBuilder().addInterceptor(HttpClient.interceptor(ctx));

        rb.build().getAsString(new StringResponseListener() {
            @Override
            public void onSuccess(@NonNull Headers headers, @NonNull String body) {
                try {
                    Type tipo = new TypeToken<RespuestaProductosVendidos>() {}.getType();
                    RespuestaProductosVendidos respuesta =
                            new Gson().fromJson(body, tipo);

                    if (respuesta.isError()) {
                        Log.e(TAG, "Error: " + respuesta.getMensaje());
                        return;
                    }

                    // Formatear fechas dd/MM/yyyy igual que ReportePagos
                    String[] start = ini.split("-");
                    String[] end   = fin.split("-");
                    String fechaIni = String.format(Locale.getDefault(),
                            "%02d/%02d/%04d",
                            Integer.parseInt(start[2]),
                            Integer.parseInt(start[1]),
                            Integer.parseInt(start[0]));
                    String fechaFin = String.format(Locale.getDefault(),
                            "%02d/%02d/%04d",
                            Integer.parseInt(end[2]),
                            Integer.parseInt(end[1]),
                            Integer.parseInt(end[0]));

                    List<ProductoVendido> lista = respuesta.getData();
                    int    totalUnidades = respuesta.getTotalUnidades();
                    double totalMonto    = respuesta.getTotalMonto();

                    requireActivity().runOnUiThread(() -> {
                        tvRangoFechas.setText(fechaIni + " — " + fechaFin);
                        tvTotalMonto.setText(
                                "Bs " + String.format("%.1f", totalMonto));
                        tvTotalUnidades.setText(String.valueOf(totalUnidades));
                        tvTotalUnidadesPie.setText(totalUnidades + " unid.");
                        tvTotalMontoPie.setText(
                                "Bs " + String.format("%.1f", totalMonto));
                        adapter.updateProductos(lista);
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Error parseando JSON", e);
                }
            }

            @Override
            public void onError(@NonNull HttpException e) {
                Log.e(TAG, "Error HTTP: " + e.getMessage());
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(ctx,
                                "Error cargando reporte: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
            }
        });
    }
}
