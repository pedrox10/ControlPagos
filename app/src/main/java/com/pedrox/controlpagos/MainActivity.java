package com.pedrox.controlpagos;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    private NavController navController;

    // Escucha el broadcast de sesión expirada (401 desde cualquier endpoint)
    private final BroadcastReceiver sessionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Limpiar el back stack completo y volver al login
            navController.navigate(
                R.id.loginFragment,
                null,
                new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build()
            );
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NavHostFragment navHostFragment = (NavHostFragment)
            getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        // Si hay sesión guardada en SharedPreferences, saltar el login
        if (AppSession.restaurar(this)) {
            navController.navigate(
                R.id.clientesFragment,
                null,
                new NavOptions.Builder()
                    .setPopUpTo(R.id.loginFragment, true)
                    .build()
            );
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Registrar receptor de sesión expirada
        LocalBroadcastManager.getInstance(this).registerReceiver(
            sessionReceiver,
            new IntentFilter(HttpClient.ACTION_SESSION_EXPIRED)
        );
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Desregistrar para evitar leaks
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(sessionReceiver);
    }
}
