package cl.goldenoddjobs.bff.messaging;

import cl.goldenoddjobs.bff.dto.InformeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Adaptador RabbitMQ / Amazon MQ: publica en el exchange fanout "informe". */
@Component
@ConditionalOnProperty(name = "messaging.provider", havingValue = "rabbit")
public class RabbitInformePublisher implements InformePublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitInformePublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public RabbitInformePublisher(RabbitTemplate rabbitTemplate,
                                  @Value("${messaging.rabbit.exchange}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    @Override
    public void publicar(InformeEvent evento) {
        try {
            rabbitTemplate.convertAndSend(exchange, "", evento);
            log.info("Evento {} publicado en RabbitMQ (exchange={})", evento.eventId(), exchange);
        } catch (RuntimeException e) {
            throw new MensajeriaException("No se pudo publicar en RabbitMQ: " + e.getMessage(), e);
        }
    }

    @Override
    public String proveedor() {
        return "rabbit";
    }
}
