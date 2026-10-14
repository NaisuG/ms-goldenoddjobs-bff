package cl.goldenoddjobs.bff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Cuerpo que envía la Lambda B a POST /api/notice. */
public class NoticeRequest {

    @NotNull
    private UUID eventId;

    @NotBlank
    private String destinatarioSub;

    @Size(max = 60)
    private String tipo;

    @NotBlank
    @Size(max = 200)
    private String titulo;

    @Size(max = 1000)
    private String mensaje;

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public String getDestinatarioSub() { return destinatarioSub; }
    public void setDestinatarioSub(String destinatarioSub) { this.destinatarioSub = destinatarioSub; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}
