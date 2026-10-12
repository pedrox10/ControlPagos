package com.pedrox.controlpagos;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jimmyworks.easyhttp.EasyHttp;
import com.jimmyworks.easyhttp.builder.BodyRequestBuilder;
import com.jimmyworks.easyhttp.builder.RequestBuilder;
import com.jimmyworks.easyhttp.exception.HttpException;
import com.jimmyworks.easyhttp.listener.StringResponseListener;
import com.pedrox.controlpagos.adapters.SectorAdapter;
import com.pedrox.controlpagos.models.Sector;
import com.pedrox.controlpagos.responses.RespuestaLista;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;

import okhttp3.Headers;

public class SectoresFragment extends Fragment implements SectorAdapter.OnSectorActionListener {

    private static final String TAG = "SectoresFragment";
    private RecyclerView rvSectores;
    private SectorAdapter sectorAdapter;
    private EditText etNuevoSector;
    private MaterialButton btnAgregarSector;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_sectores, container, false);
        rvSectores        = v.findViewById(R.id.rvSectores);
        etNuevoSector     = v.findViewById(R.id.etNuevoSector);
        btnAgregarSector  = v.findViewById(R.id.btnAgregarSector);

        rvSectores.setLayoutManager(new LinearLayoutManager(getContext()));
        sectorAdapter = new SectorAdapter(new ArrayList<>());
        sectorAdapter.setOnSectorActionListener(this);
        rvSectores.setAdapter(sectorAdapter);

        cargarSectores();

        btnAgregarSector.setOnClickListener(v2 -> {
            String nombre = etNuevoSector.getText().toString().trim().toUpperCase();
            if (nombre.isEmpty()) {
                Toast.makeText(getContext(), "Escribe un nombre para el sector", Toast.LENGTH_SHORT).show();
                return;
            }
            agregarSector(nombre);
        });

        return v;
    }

    private void cargarSectores() {
        RequestBuilder builder = EasyHttp.get(getContext(), Constants.API_BASE_URL + "sectores");
        builder.getOkHttpClientBuilder().addInterceptor(HttpClient.interceptor(getContext()));
        builder.build().getAsString(new StringResponseListener() {
            @Override
            public void onSuccess(@NonNull Headers headers, @NonNull String body) {
                try {
                    Type tipo = new TypeToken<RespuestaLista<Sector>>() {}.getType();
                    RespuestaLista<Sector> respuesta = new Gson().fromJson(body, tipo);
                    if (!respuesta.isError()) {
                        sectorAdapter.updateSectores(respuesta.getData());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parseando sectores", e);
                }
            }
            @Override
            public void onError(@NonNull HttpException e) {
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void agregarSector(String nombre) {
        JSONObject obj = new JSONObject();
        try { obj.put("nombre", nombre); } catch (JSONException e) { e.printStackTrace(); }

        BodyRequestBuilder rb = EasyHttp.post(getContext(), Constants.API_BASE_URL + "sector/add");
        rb.getOkHttpClientBuilder().addInterceptor(HttpClient.interceptor(getContext()));
        rb.stringBody("application/json; charset=utf-8", obj.toString())
                .build()
                .getAsString(new StringResponseListener() {
                    @Override
                    public void onSuccess(@NonNull Headers headers, @NonNull String body) {
                        try {
                            Type tipo = new TypeToken<RespuestaLista<Sector>>() {}.getType();
                            RespuestaLista<Sector> respuesta = new Gson().fromJson(body, tipo);
                            if (!respuesta.isError()) {
                                etNuevoSector.setText("");
                                sectorAdapter.updateSectores(respuesta.getData());
                                Bundle result = new Bundle();
                                result.putBoolean("sectoresModificados", true);
                                getParentFragmentManager().setFragmentResult("sectorResult", result);
                                Toast.makeText(getContext(), "Sector agregado", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), respuesta.getMensaje(), Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error", e);
                        }
                    }
                    @Override
                    public void onError(@NonNull HttpException e) {
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onEditarSector(Sector sector) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_accion_sector, null);
        EditText etNombre = view.findViewById(R.id.etNombreSector);
        etNombre.setText(sector.getNombre());

        builder.setView(view)
                .setTitle("Editar sector")
                .setPositiveButton("Guardar", (dialog, id) -> {
                    String nombre = etNombre.getText().toString().trim().toUpperCase();
                    if (nombre.isEmpty()) return;
                    JSONObject obj = new JSONObject();
                    try {
                        obj.put("id",     sector.getId());
                        obj.put("nombre", nombre);
                    } catch (JSONException e) { e.printStackTrace(); }

                    BodyRequestBuilder rb = EasyHttp.post(getContext(), Constants.API_BASE_URL + "sector/edit");
                    rb.getOkHttpClientBuilder().addInterceptor(HttpClient.interceptor(getContext()));
                    rb.stringBody("application/json; charset=utf-8", obj.toString())
                            .build()
                            .getAsString(new StringResponseListener() {
                                @Override
                                public void onSuccess(@NonNull Headers headers, @NonNull String body) {
                                    try {
                                        Type tipo = new TypeToken<RespuestaLista<Sector>>() {}.getType();
                                        RespuestaLista<Sector> respuesta = new Gson().fromJson(body, tipo);
                                        if (!respuesta.isError()) {
                                            sectorAdapter.updateSectores(respuesta.getData());
                                            Bundle result = new Bundle();
                                            result.putBoolean("sectoresModificados", true);
                                            getParentFragmentManager().setFragmentResult("sectorResult", result);
                                            Toast.makeText(getContext(), "Sector actualizado", Toast.LENGTH_SHORT).show();
                                        }
                                    } catch (Exception e) { Log.e(TAG, "Error", e); }
                                }
                                @Override
                                public void onError(@NonNull HttpException e) {
                                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancelar", (dialog, id) -> dialog.dismiss());
        builder.create().show();
    }

    @Override
    public void onEliminarSector(Sector sector) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar sector")
                .setMessage("¿Eliminar el sector " + sector.getNombre() + "?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    JSONObject obj = new JSONObject();
                    try { obj.put("id", sector.getId()); } catch (JSONException e) { e.printStackTrace(); }

                    BodyRequestBuilder rb = EasyHttp.post(getContext(), Constants.API_BASE_URL + "sector/delete");
                    rb.getOkHttpClientBuilder().addInterceptor(HttpClient.interceptor(getContext()));
                    rb.stringBody("application/json; charset=utf-8", obj.toString())
                            .build()
                            .getAsString(new StringResponseListener() {
                                @Override
                                public void onSuccess(@NonNull Headers headers, @NonNull String body) {
                                    try {
                                        Type tipo = new TypeToken<RespuestaLista<Sector>>() {}.getType();
                                        RespuestaLista<Sector> respuesta = new Gson().fromJson(body, tipo);
                                        if (!respuesta.isError()) {
                                            sectorAdapter.updateSectores(respuesta.getData());
                                            Bundle result = new Bundle();
                                            result.putBoolean("sectoresModificados", true);
                                            getParentFragmentManager().setFragmentResult("sectorResult", result);
                                            Toast.makeText(getContext(), "Sector eliminado", Toast.LENGTH_SHORT).show();
                                        } else {
                                            // Mostrar el mensaje del servidor directamente
                                            // Ej: "No se puede eliminar, tiene 5 clientes asignados"
                                            Toast.makeText(getContext(), respuesta.getMensaje(), Toast.LENGTH_LONG).show();
                                        }
                                    } catch (Exception e) { Log.e(TAG, "Error", e); }
                                }
                                @Override
                                public void onError(@NonNull HttpException e) {
                                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
