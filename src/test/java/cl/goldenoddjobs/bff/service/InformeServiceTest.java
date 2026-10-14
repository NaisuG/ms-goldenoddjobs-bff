package cl.goldenoddjobs.bff.service;

import cl.goldenoddjobs.bff.dto.InformeEvent;
import cl.goldenoddjobs.bff.messaging.InformePublisher;
import cl.goldenoddjobs.bff.messaging.MensajeriaException;
import cl.goldenoddjobs.bff.model.EstadoInforme;
import cl.goldenoddjobs.bff.model.Informe;
import cl.goldenoddjobs.bff.repository.InformeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InformeServiceTest {

    private InformeRepository repo;
    private InformePublisher publisher;
    private InformeGenerador generador;
    private InformeService service;

    @BeforeEach
    void setUp() {
        repo = mock(InformeRepository.class);
        publisher = mock(InformePublisher.class);
        generador = mock(InformeGenerador.class);
        service = new InformeService(repo, publisher, generador);
        when(repo.save(any(Informe.class))).thenAnswer(inv -> {
            Informe i = inv.getArgument(0);
            if (i.getId() == null) i.setId(7L);
            return i;
        });
    }

    @Test
    void solicitarGuardaPendienteYPublicaElMismoEventId() {
        Informe informe = service.solicitar("sub-1", "Ana Admin", "a@b.cl", null);

        assertEquals(EstadoInforme.PENDIENTE, informe.getEstado());
        assertEquals("RESUMEN_GENERAL", informe.getTipoInforme());

        ArgumentCaptor<InformeEvent> captor = ArgumentCaptor.forClass(InformeEvent.class);
        verify(publisher).publicar(captor.capture());
        assertEquals(informe.getEventId(), captor.getValue().eventId());
        assertEquals("sub-1", captor.getValue().solicitanteSub());
        assertEquals("Ana Admin", captor.getValue().solicitanteNombre());
        assertEquals("a@b.cl", captor.getValue().email());
        assertEquals(InformeEvent.TIPO_INFORME_SOLICITADO, captor.getValue().tipo());
    }

    @Test
    void siElBrokerFallaElInformeQuedaEnErrorYSePropagaLaExcepcion() {
        doThrow(new MensajeriaException("caído", null)).when(publisher).publicar(any());

        assertThrows(MensajeriaException.class, () -> service.solicitar("sub-1", null, null, "X"));

        ArgumentCaptor<Informe> captor = ArgumentCaptor.forClass(Informe.class);
        verify(repo, times(2)).save(captor.capture());
        assertEquals(EstadoInforme.ERROR, captor.getValue().getEstado());
    }

    @Test
    void generarEsIdempotente() {
        UUID id = UUID.randomUUID();
        Informe ya = new Informe();
        ya.setEstado(EstadoInforme.GENERADO);
        when(repo.findByEventId(id)).thenReturn(Optional.of(ya));

        service.generar(id);

        verify(repo, never()).save(any());
        verifyNoInteractions(generador);
    }

    @Test
    void generarMarcaGeneradoUnInformePendiente() {
        UUID id = UUID.randomUUID();
        Informe pendiente = new Informe();
        pendiente.setEstado(EstadoInforme.PENDIENTE);
        when(repo.findByEventId(id)).thenReturn(Optional.of(pendiente));
        when(generador.resumenGeneral()).thenReturn("resumen");

        service.generar(id);

        assertEquals(EstadoInforme.GENERADO, pendiente.getEstado());
        assertEquals("resumen", pendiente.getResumen());
        assertNotNull(pendiente.getGeneradoEn());
    }
}
