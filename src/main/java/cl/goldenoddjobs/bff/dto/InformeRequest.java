package cl.goldenoddjobs.bff.dto;

import jakarta.validation.constraints.Size;

public class InformeRequest {

    /** Opcional. Si no viene, se usa RESUMEN_GENERAL. */
    @Size(max = 60)
    private String tipoInforme;

    public String getTipoInforme() { return tipoInforme; }
    public void setTipoInforme(String tipoInforme) { this.tipoInforme = tipoInforme; }
}
