package cl.goldenoddjobs.bff.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "informe")
public class Informe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador del evento publicado en el broker (idempotencia). */
    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "solicitante_sub", nullable = false)
    private String solicitanteSub;

    private String email;

    /** Tipo de informe pedido, p. ej. RESUMEN_SOLICITUDES. */
    @Column(name = "tipo_informe")
    private String tipoInforme;

    @Enumerated(EnumType.STRING)
    private EstadoInforme estado;

    @Column(length = 2000)
    private String resumen;

    @Column(name = "creado_en")
    private Instant creadoEn;

    @Column(name = "generado_en")
    private Instant generadoEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public String getSolicitanteSub() { return solicitanteSub; }
    public void setSolicitanteSub(String solicitanteSub) { this.solicitanteSub = solicitanteSub; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTipoInforme() { return tipoInforme; }
    public void setTipoInforme(String tipoInforme) { this.tipoInforme = tipoInforme; }
    public EstadoInforme getEstado() { return estado; }
    public void setEstado(EstadoInforme estado) { this.estado = estado; }
    public String getResumen() { return resumen; }
    public void setResumen(String resumen) { this.resumen = resumen; }
    public Instant getCreadoEn() { return creadoEn; }
    public void setCreadoEn(Instant creadoEn) { this.creadoEn = creadoEn; }
    public Instant getGeneradoEn() { return generadoEn; }
    public void setGeneradoEn(Instant generadoEn) { this.generadoEn = generadoEn; }
}
