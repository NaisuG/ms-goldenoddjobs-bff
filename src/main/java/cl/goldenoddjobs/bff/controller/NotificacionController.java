package cl.goldenoddjobs.bff.controller;

import cl.goldenoddjobs.bff.model.Notificacion;
import cl.goldenoddjobs.bff.service.NoticeService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class NotificacionController {

    private final NoticeService noticeService;

    public NotificacionController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping("/notificaciones")
    public List<Notificacion> mias(@AuthenticationPrincipal Jwt jwt) {
        return noticeService.listar(jwt.getSubject());
    }
}
