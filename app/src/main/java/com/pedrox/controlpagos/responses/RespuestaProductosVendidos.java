package com.pedrox.controlpagos.responses;

import com.pedrox.controlpagos.models.ProductoVendido;
import java.util.List;

public class RespuestaProductosVendidos {
    private boolean              error;
    private String               mensaje;
    private int                  totalUnidades;
    private double               totalMonto;
    private List<ProductoVendido> data;

    public boolean               isError()          { return error; }
    public String                getMensaje()        { return mensaje; }
    public int                   getTotalUnidades()  { return totalUnidades; }
    public double                getTotalMonto()     { return totalMonto; }
    public List<ProductoVendido> getData()           { return data; }
}
