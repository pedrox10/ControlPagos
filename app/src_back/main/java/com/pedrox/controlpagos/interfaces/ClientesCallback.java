package com.pedrox.controlpagos.interfaces;

import com.pedrox.controlpagos.models.Cliente;
import java.util.List;

public interface ClientesCallback {
    void onClientesRecibidos(List<Cliente> clientes);
    void onError(String mensaje);
}