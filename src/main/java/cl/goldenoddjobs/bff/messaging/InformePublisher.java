package cl.goldenoddjobs.bff.messaging;

import cl.goldenoddjobs.bff.dto.InformeEvent;

/**
 * Puerto de salida hacia el broker. El resto de la app solo conoce esta interfaz:
 * cambiar de AWS SNS/SQS a RabbitMQ (o viceversa) es cambiar la propiedad messaging.provider.
 */
public interface InformePublisher {

    /** Publica el evento. Lanza {@link MensajeriaException} si el broker no lo acepta. */
    void publicar(InformeEvent evento);

    /** Nombre del adaptador activo (se muestra en /api/health). */
    String proveedor();
}
