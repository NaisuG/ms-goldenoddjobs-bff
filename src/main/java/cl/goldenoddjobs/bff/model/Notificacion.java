package cl.goldenoddjobs.bff.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notificacion")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mismo eventId del evento original: evita notificaciones duplicadas si el broker reentrega. */
    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "destinatario_sub", nullable = false)
    private String destinatarioSub;

    private String tipo;

    private String titulo;

    @Column(length = 1000)
    private String mensaje;

    private boolean leida;

    @Column(name = "creada_en")
    private Instant creadaEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }
    public Instant getCreadaEn() { return creadaEn; }
    public void setCreadaEn(Instant creadaEn) { this.creadaEn = creadaEn; }
}
