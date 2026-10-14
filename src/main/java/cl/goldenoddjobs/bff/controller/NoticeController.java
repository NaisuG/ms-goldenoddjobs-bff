package cl.goldenoddjobs.bff.controller;

import cl.goldenoddjobs.bff.dto.NoticeRequest;
import cl.goldenoddjobs.bff.service.NoticeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @PostMapping("/notice")
    public ResponseEntity<Map<String, Object>> notice(@RequestBody @Valid NoticeRequest req) {
        boolean creada = noticeService.registrar(req);
        return ResponseEntity
                .status(creada ? HttpStatus.CREATED : HttpStatus.OK)
                .body(Map.of("recibido", true, "duplicado", !creada, "eventId", req.getEventId()));
    }
}
