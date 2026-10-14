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

    @PostMapping("/informes")
    public ResponseEntity<Map<String, Object>> solicitar(@RequestBody(required = false) @Valid InformeRequest body,
                                                         @AuthenticationPrincipal Jwt jwt) {
        String tipo = body == null ? null : body.getTipoInforme();
        String email = emailDe(body, jwt);
        String nombre = nombreDe(body, jwt, email);
        Informe informe = informeService.solicitar(jwt.getSubject(), nombre, email, tipo);
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

    private String emailDe(InformeRequest body, Jwt jwt) {
        String delToken = jwt.getClaimAsString("email");
        if (esCorreo(delToken)) {
            return delToken;
        }
        String delFront = body == null ? null : body.getEmail();
        if (esCorreo(delFront)) {
            return delFront;
        }
        String username = jwt.getClaimAsString("username");
        return esCorreo(username) ? username : null;
    }

    private String nombreDe(InformeRequest body, Jwt jwt, String email) {
        if (body != null && body.getNombre() != null && !body.getNombre().isBlank()) {
            return body.getNombre().trim();
        }
        String delToken = jwt.getClaimAsString("name");
        if (delToken != null && !delToken.isBlank()) {
            return delToken;
        }
        return email != null ? email : jwt.getClaimAsString("username");
    }

    private boolean esCorreo(String valor) {
        return valor != null && valor.contains("@");
    }
}
