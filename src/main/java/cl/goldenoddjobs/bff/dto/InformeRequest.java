package cl.goldenoddjobs.bff.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class InformeRequest {

    @Size(max = 60)
    private String tipoInforme;

    @Email
    @Size(max = 200)
    private String email;

    @Size(max = 120)
    private String nombre;

    public String getTipoInforme() { return tipoInforme; }
    public void setTipoInforme(String tipoInforme) { this.tipoInforme = tipoInforme; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
