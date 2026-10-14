package cl.goldenoddjobs.bff.dto;

import java.time.Instant;
import java.util.UUID;

public record InformeEvent(
        UUID eventId,
        String tipo,           
        Long informeId,
        String solicitanteSub,
        String solicitanteNombre,
        String email,
        String tipoInforme,
        Instant solicitadoEn
) {
    public static final String TIPO_INFORME_SOLICITADO = "INFORME_SOLICITADO";
}
