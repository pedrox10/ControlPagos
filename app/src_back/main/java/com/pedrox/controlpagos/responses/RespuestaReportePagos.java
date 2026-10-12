package com.pedrox.controlpagos.responses;

import com.pedrox.controlpagos.models.Pago;
import com.pedrox.controlpagos.models.TotalPorCobrador;

import java.util.List;

public class RespuestaReportePagos {
    private boolean error;
    private String mensaje;
    private List<Pago> data;
    private double total_general;
    private List<TotalPorCobrador> totales_por_cobrador;

    public boolean isError() {
        return error;
    }

    public String getMensaje() {
        return mensaje;
    }

    public List<Pago> getData() {
        return data;
    }

    public double getTotal_general() {
        return total_general;
    }

    public List<TotalPorCobrador> getTotales_por_cobrador() {
        return totales_por_cobrador;
    }
}
