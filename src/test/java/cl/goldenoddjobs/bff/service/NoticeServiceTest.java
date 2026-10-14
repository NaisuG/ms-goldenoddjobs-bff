package cl.goldenoddjobs.bff.service;

import cl.goldenoddjobs.bff.dto.NoticeRequest;
import cl.goldenoddjobs.bff.model.Notificacion;
import cl.goldenoddjobs.bff.repository.NotificacionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NoticeServiceTest {

    private NoticeRequest request(UUID id) {
        NoticeRequest r = new NoticeRequest();
        r.setEventId(id);
        r.setDestinatarioSub("sub-1");
        r.setTitulo("Informe listo");
        return r;
    }

    @Test
    void creaLaNotificacionLaPrimeraVez() {
        NotificacionRepository repo = mock(NotificacionRepository.class);
        UUID id = UUID.randomUUID();
        when(repo.existsByEventId(id)).thenReturn(false);

        assertTrue(new NoticeService(repo).registrar(request(id)));
        verify(repo).save(any(Notificacion.class));
    }

    @Test
    void ignoraUnEventoDuplicado() {
        NotificacionRepository repo = mock(NotificacionRepository.class);
        UUID id = UUID.randomUUID();
        when(repo.existsByEventId(id)).thenReturn(true);

        assertFalse(new NoticeService(repo).registrar(request(id)));
        verify(repo, never()).save(any());
    }

    @Test
    void ignoraLaCarreraConcurrenteDeDosEntregas() {
        NotificacionRepository repo = mock(NotificacionRepository.class);
        UUID id = UUID.randomUUID();
        when(repo.existsByEventId(id)).thenReturn(false);
        when(repo.save(any())).thenThrow(new DataIntegrityViolationException("unique"));

        assertFalse(new NoticeService(repo).registrar(request(id)));
    }
}
