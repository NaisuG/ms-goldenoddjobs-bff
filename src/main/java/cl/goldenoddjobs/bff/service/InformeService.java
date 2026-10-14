package cl.goldenoddjobs.bff.service;

import cl.goldenoddjobs.bff.dto.InformeEvent;
import cl.goldenoddjobs.bff.messaging.InformePublisher;
import cl.goldenoddjobs.bff.messaging.MensajeriaException;
import cl.goldenoddjobs.bff.model.EstadoInforme;
import cl.goldenoddjobs.bff.model.Informe;
import cl.goldenoddjobs.bff.repository.InformeRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class InformeService {

    static final String TIPO_POR_DEFECTO = "RESUMEN_GENERAL";

    private final InformeRepository repository;
    private final InformePublisher publisher;
    private final InformeGenerador generador;

    public InformeService(InformeRepository repository, InformePublisher publisher, InformeGenerador generador) {
        this.repository = repository;
        this.publisher = publisher;
        this.generador = generador;
    }

    public Informe solicitar(String sub, String nombre, String email, String tipoInforme) {
        Informe informe = new Informe();
        informe.setEventId(UUID.randomUUID());
        informe.setSolicitanteSub(sub);
        informe.setSolicitanteNombre(nombre);
        informe.setEmail(email);
        informe.setTipoInforme(tipoInforme == null || tipoInforme.isBlank() ? TIPO_POR_DEFECTO : tipoInforme);
        informe.setEstado(EstadoInforme.PENDIENTE);
        informe.setCreadoEn(Instant.now());
        informe = repository.save(informe);

        InformeEvent evento = new InformeEvent(
                informe.getEventId(),
                InformeEvent.TIPO_INFORME_SOLICITADO,
                informe.getId(),
                sub,
                nombre,
                email,
                informe.getTipoInforme(),
                informe.getCreadoEn());
        try {
            publisher.publicar(evento);
        } catch (MensajeriaException e) {
            informe.setEstado(EstadoInforme.ERROR);
            informe.setResumen("No se pudo encolar: " + e.getMessage());
            repository.save(informe);
            throw e;
        }
        return informe;
    }

    public List<Informe> listar(String sub, boolean esAdmin) {
        return esAdmin
                ? repository.findAllByOrderByCreadoEnDesc()
                : repository.findBySolicitanteSubOrderByCreadoEnDesc(sub);
    }

    public void generar(UUID eventId) {
        repository.findByEventId(eventId).ifPresent(informe -> {
            if (informe.getEstado() == EstadoInforme.GENERADO) {
                return;
            }
            informe.setResumen(generador.resumenGeneral());
            informe.setEstado(EstadoInforme.GENERADO);
            informe.setGeneradoEn(Instant.now());
            repository.save(informe);
        });
    }
}
