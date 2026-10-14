package cl.goldenoddjobs.bff.messaging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

@Configuration
@ConditionalOnProperty(name = "messaging.provider", havingValue = "sns", matchIfMissing = true)
public class SnsConfig {

    /** Usa la cadena de credenciales por defecto (AWS_ACCESS_KEY_ID / SECRET / SESSION_TOKEN del .env). */
    @Bean
    public SnsClient snsClient(@Value("${aws.region}") String region) {
        return SnsClient.builder().region(Region.of(region)).build();
    }
}
