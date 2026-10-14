package cl.goldenoddjobs.bff.service;

import cl.goldenoddjobs.bff.dto.InformeEvent;
import cl.goldenoddjobs.bff.dto.NoticeRequest;
import cl.goldenoddjobs.bff.model.Notificacion;
import cl.goldenoddjobs.bff.repository.NotificacionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class NoticeService {

    private final NotificacionRepository repository;

    public NoticeService(NotificacionRepository repository) {
        this.repository = repository;
    }

    /**
     * Guarda la notificación. Idempotente por eventId: SQS/Rabbit pueden entregar un mensaje más de una vez.
     * @return true si se creó, false si ya existía (duplicado ignorado)
     */
    public boolean registrar(NoticeRequest req) {
        if (repository.existsByEventId(req.getEventId())) {
            return false;
        }
        Notificacion n = new Notificacion();
        n.setEventId(req.getEventId());
        n.setDestinatarioSub(req.getDestinatarioSub());
        n.setTipo(req.getTipo());
        n.setTitulo(req.getTitulo());
        n.setMensaje(req.getMensaje());
        n.setLeida(false);
        n.setCreadaEn(Instant.now());
        try {
            repository.save(n);
            return true;
        } catch (DataIntegrityViolationException e) {
            return false; // otra entrega concurrente ganó la carrera
        }
    }

    /** Versión para el modo RabbitMQ, donde el evento llega directo al BFF. */
    public boolean registrarDesdeEvento(InformeEvent evento) {
        NoticeRequest req = new NoticeRequest();
        req.setEventId(evento.eventId());
        req.setDestinatarioSub(evento.solicitanteSub());
        req.setTipo(evento.tipo());
        req.setTitulo("Informe procesado");
        req.setMensaje("Tu informe " + evento.tipoInforme() + " (#" + evento.informeId() + ") fue procesado.");
        return registrar(req);
    }

    public List<Notificacion> listar(String sub) {
        return repository.findByDestinatarioSubOrderByCreadaEnDesc(sub);
    }
}
