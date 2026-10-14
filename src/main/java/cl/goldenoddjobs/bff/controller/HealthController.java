package cl.goldenoddjobs.bff.controller;

import cl.goldenoddjobs.bff.messaging.InformePublisher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final InformePublisher publisher;

    public HealthController(InformePublisher publisher) {
        this.publisher = publisher;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "ms-goldenoddjobs-bff",
                "broker", publisher.proveedor(),
                "time", Instant.now().toString());
    }
}
