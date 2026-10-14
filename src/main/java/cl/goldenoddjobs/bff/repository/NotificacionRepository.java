package cl.goldenoddjobs.bff.repository;

import cl.goldenoddjobs.bff.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findByDestinatarioSubOrderByCreadaEnDesc(String destinatarioSub);
    boolean existsByEventId(UUID eventId);
}
