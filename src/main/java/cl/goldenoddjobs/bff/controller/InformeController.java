package cl.goldenoddjobs.bff.controller;

import cl.goldenoddjobs.bff.dto.InformeRequest;
import cl.goldenoddjobs.bff.model.Informe;
import cl.goldenoddjobs.bff.service.InformeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class InformeController {

    private final InformeService informeService;

    public InformeController(InformeService informeService) {
        this.informeService = informeService;
    }

    /** Responde 202 de inmediato: el informe se genera de forma asíncrona (SNS -> SQS -> Lambda). */
    @PostMapping("/informes")
    public ResponseEntity<Map<String, Object>> solicitar(@RequestBody(required = false) @Valid InformeRequest body,
                                                         @AuthenticationPrincipal Jwt jwt) {
        String tipo = body == null ? null : body.getTipoInforme();
        Informe informe = informeService.solicitar(jwt.getSubject(), emailDe(jwt), tipo);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "mensaje", "Solicitud recibida con éxito",
                "informeId", informe.getId(),
                "eventId", informe.getEventId(),
                "estado", informe.getEstado()));
    }

    @GetMapping("/informes")
    public List<Informe> listar(@AuthenticationPrincipal Jwt jwt) {
        List<String> grupos = jwt.getClaimAsStringList("cognito:groups");
        boolean admin = grupos != null && grupos.contains("ADMIN");
        return informeService.listar(jwt.getSubject(), admin);
    }

    /** El access token de Cognito no trae "email": se usa "username" como respaldo. */
    private String emailDe(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        return email != null ? email : jwt.getClaimAsString("username");
    }
}
