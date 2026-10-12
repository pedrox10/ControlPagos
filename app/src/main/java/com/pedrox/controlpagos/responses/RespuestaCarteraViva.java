package com.pedrox.controlpagos.responses;

import com.pedrox.controlpagos.models.CarteraCliente;
import java.util.List;

public class RespuestaCarteraViva {
    private boolean error;
    private String mensaje;
    private double totalGeneral;
    private List<CarteraCliente> data;

    public boolean isError()                        { return error; }
    public String getMensaje()                      { return mensaje; }
    public double getTotalGeneral()                 { return Math.round(totalGeneral * 10.0) / 10.0; }
    public List<CarteraCliente> getData()           { return data; }
}
