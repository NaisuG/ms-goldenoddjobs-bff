package cl.goldenoddjobs.bff.repository;

import cl.goldenoddjobs.bff.model.Informe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InformeRepository extends JpaRepository<Informe, Long> {
    List<Informe> findBySolicitanteSubOrderByCreadoEnDesc(String solicitanteSub);
    List<Informe> findAllByOrderByCreadoEnDesc();
    Optional<Informe> findByEventId(UUID eventId);
}
