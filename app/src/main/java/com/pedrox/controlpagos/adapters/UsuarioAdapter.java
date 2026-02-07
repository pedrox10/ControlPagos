package com.pedrox.controlpagos.adapters;

import static androidx.constraintlayout.motion.utils.Oscillator.TAG;

import android.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.jimmyworks.easyhttp.EasyHttp;
import com.jimmyworks.easyhttp.exception.HttpException;
import com.jimmyworks.easyhttp.listener.JsonResponseListener;
import com.pedrox.controlpagos.Constants;
import com.pedrox.controlpagos.R;
import com.pedrox.controlpagos.models.Pago;
import com.pedrox.controlpagos.models.Producto;
import com.pedrox.controlpagos.models.Usuario;
import com.pedrox.controlpagos.responses.ClienteRespuesta;
import com.pedrox.controlpagos.responses.UsuarioRespuesta;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import okhttp3.Headers;

public class UsuarioAdapter extends RecyclerView.Adapter<UsuarioAdapter.UsuarioViewHolder> {

    private List<Usuario> listaUsuarios;

    public UsuarioAdapter(List<Usuario> listaUsuarios) {
        this.listaUsuarios = listaUsuarios;
    }

    public void updateUsuarios(List<Usuario> usuarios) {
        this.listaUsuarios.clear();
        this.listaUsuarios.addAll(usuarios);
        notifyDataSetChanged();
    }
    
    @Override
    public UsuarioAdapter.UsuarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_usuario, parent, false);
        return new UsuarioAdapter.UsuarioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UsuarioAdapter.UsuarioViewHolder holder, int position) {
        Usuario usuario = listaUsuarios.get(position);
        int pos = holder.getAdapterPosition();
        holder.tvNombre.setText(usuario.getNombre());
        holder.tvAlias.setText(usuario.getAlias());
        holder.tvRol.setText(usuario.getRol());
        holder.btnEditarUsuario.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            LayoutInflater inflater = LayoutInflater.from(v.getContext());
            View view = inflater.inflate(R.layout.dialog_accion_usuario, null);
            EditText etNombre = view.findViewById(R.id.etNombreUsuario);
            EditText etAlias = view.findViewById(R.id.etAliasUsuario);
            EditText etClave = view.findViewById(R.id.etClaveUsuario);
            Spinner spRol = view.findViewById(R.id.spRol);
            SwitchMaterial swHabilitado = view.findViewById(R.id.swHabilitado);
            // Prellenar datos
            etNombre.setText(usuario.getNombre());
            etAlias.setText(usuario.getAlias());
            etClave.setText(usuario.getClave());
            // Seleccionar grupo correcto
            String[] roles = view.getResources().getStringArray(R.array.roles_array);
            for (int i = 0; i < roles.length; i++) {
                if (roles[i].equalsIgnoreCase(usuario.getRol())) {
                    spRol.setSelection(i);
                    break;
                }
            }
            swHabilitado.setChecked(usuario.getHabilitado() == 1);
            builder.setView(view)
                    .setTitle("Editar usuario")
                    .setPositiveButton("Guardar", (dialog, id) -> {
                        String nombre = etNombre.getText().toString().trim();
                        if(!nombre.equals("")) {
                            String alias = etAlias.getText().toString().trim();
                            String clave = etClave.getText().toString().trim();
                            String rol = spRol.getSelectedItem().toString();
                            boolean habilitado = swHabilitado.isChecked();
                            JSONObject obj = new JSONObject();
                            try {
                                obj.put("id", usuario.getId());
                                obj.put("nombre", nombre);
                                obj.put("alias", alias);
                                obj.put("clave", clave);
                                obj.put("rol", rol);
                                obj.put("habilitado", habilitado);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            holder.btnEditarUsuario.setEnabled(false);
                            holder.btnEditarUsuario.setAlpha(0.5f);
                            EasyHttp.post(v.getContext(), Constants.API_BASE_URL + "usuario/edit")
                                    .stringBody("application/json; charset=utf-8", obj.toString())
                                    .build()
                                    .getJsonAsObject(UsuarioRespuesta.class, new JsonResponseListener<UsuarioRespuesta>() {
                                        @Override
                                        public void onSuccess(@NonNull Headers headers, @NonNull UsuarioRespuesta respuesta) {
                                            if (respuesta.isError()) {
                                                Log.d(TAG, respuesta.getMensaje());
                                            } else {
                                                Usuario usuarioActualizado = respuesta.getData();
                                                listaUsuarios.set(pos, usuarioActualizado);
                                                notifyItemChanged(pos);
                                                Toast.makeText(v.getContext(), "usuario actualizado", Toast.LENGTH_SHORT).show();
                                                Log.d(TAG, "onSuccess: " + usuarioActualizado.toString());
                                                holder.btnEditarUsuario.setEnabled(true);
                                                holder.btnEditarUsuario.setAlpha(1f);
                                            }
                                        }
                                        @Override
                                        public void onError(@NonNull HttpException e) {
                                            Log.d(TAG, e.getMessage());
                                            Toast.makeText(v.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                            holder.btnEditarUsuario.setEnabled(true);
                                            holder.btnEditarUsuario.setAlpha(1f);
                                        }
                                    });
                        } else {
                            Toast.makeText(v.getContext(), "¡El nombre no puede ser vacio!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancelar", (dialog, id) -> dialog.dismiss());
            AlertDialog dialog = builder.create();
            dialog.show();
        });
        holder.btnEliminarUsuario.setOnClickListener(view -> {
            new androidx.appcompat.app.AlertDialog.Builder(view.getContext())
                    .setTitle("Eliminar Cliente")
                    .setMessage("¿Seguro que deseas eliminar a " + usuario.getNombre() + "?")
                    .setPositiveButton("Eliminar", (dialog, which) -> {
                        JSONObject obj = new JSONObject();
                        try {
                            obj.put("id", usuario.getId());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        EasyHttp.post(view.getContext(), Constants.API_BASE_URL + "usuario/delete")
                                .stringBody("application/json; charset=utf-8", obj.toString())
                                .build()
                                .getJsonAsObject(UsuarioRespuesta.class, new JsonResponseListener<UsuarioRespuesta>() {
                                    @Override
                                    public void onSuccess(@NonNull Headers headers, @NonNull UsuarioRespuesta respuesta) {
                                        if (!respuesta.isError()) {
                                            Toast.makeText(view.getContext(), "usuario eliminado", Toast.LENGTH_SHORT).show();
                                            listaUsuarios.remove(pos);
                                            notifyItemRemoved(pos);
                                        } else {
                                            Toast.makeText(view.getContext(), respuesta.getMensaje(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                    @Override
                                    public void onError(@NonNull HttpException e) {
                                        Toast.makeText(view.getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    })
                    .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    @Override
    public int getItemCount() {
       return listaUsuarios.size();
    }

    static class UsuarioViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre;
        TextView tvAlias;
        TextView tvRol;
        MaterialButton btnEditarUsuario;
        MaterialButton btnEliminarUsuario;

        public UsuarioViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvAlias = itemView.findViewById(R.id.tvAlias);
            tvRol = itemView.findViewById(R.id.tvRol);
            btnEditarUsuario = itemView.findViewById(R.id.btnEditarUsuario);
            btnEliminarUsuario = itemView.findViewById(R.id.btnEliminarUsuario);
        }
    }

    public void addUsuario(Usuario usuario) {
        int position = listaUsuarios.size();   // última posición
        listaUsuarios.add(usuario);
        notifyItemInserted(position);
    }
}
