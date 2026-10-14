package cl.goldenoddjobs.bff.messaging;

import cl.goldenoddjobs.bff.dto.InformeEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

/**
 * Adaptador AWS: publica en un topic SNS. SNS hace fanout a las dos colas SQS suscritas
 * (cola A -> Lambda A, cola B -> Lambda B).
 */
@Component
@ConditionalOnProperty(name = "messaging.provider", havingValue = "sns", matchIfMissing = true)
public class SnsInformePublisher implements InformePublisher {

    private static final Logger log = LoggerFactory.getLogger(SnsInformePublisher.class);

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;
    private final String topicArn;

    public SnsInformePublisher(SnsClient snsClient,
                               ObjectMapper objectMapper,
                               @Value("${messaging.sns.topic-arn:}") String topicArn) {
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
        this.topicArn = topicArn;
    }

    @Override
    public void publicar(InformeEvent evento) {
        if (topicArn == null || topicArn.isBlank()) {
            throw new MensajeriaException("SNS_TOPIC_ARN no está configurado", null);
        }
        try {
            String json = objectMapper.writeValueAsString(evento);
            PublishResponse resp = snsClient.publish(PublishRequest.builder()
                    .topicArn(topicArn)
                    .subject(evento.tipo())
                    .message(json)
                    .build());
            log.info("Evento {} publicado en SNS (messageId={})", evento.eventId(), resp.messageId());
        } catch (JsonProcessingException e) {
            throw new MensajeriaException("No se pudo serializar el evento", e);
        } catch (RuntimeException e) {
            throw new MensajeriaException("No se pudo publicar en SNS: " + e.getMessage(), e);
        }
    }

    @Override
    public String proveedor() {
        return "sns";
    }
}
