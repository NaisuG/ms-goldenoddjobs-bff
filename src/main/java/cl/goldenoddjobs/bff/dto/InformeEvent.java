package cl.goldenoddjobs.bff.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento único que viaja por el broker (mismo JSON para SNS/SQS y RabbitMQ).
 * Las dos Lambdas (o los dos listeners) reciben exactamente este mensaje.
 */
public record InformeEvent(
        UUID eventId,
        String tipo,            // INFORME_SOLICITADO
        Long informeId,
        String solicitanteSub,
        String email,
        String tipoInforme,
        Instant solicitadoEn
) {
    public static final String TIPO_INFORME_SOLICITADO = "INFORME_SOLICITADO";
}
