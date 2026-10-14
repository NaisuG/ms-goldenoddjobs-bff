package cl.goldenoddjobs.bff.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConditionalOnProperty(name = "messaging.provider", havingValue = "rabbit")
public class RabbitConfig {

    @Bean
    public FanoutExchange informeExchange(@Value("${messaging.rabbit.exchange}") String name) {
        return new FanoutExchange(name, true, false);
    }

    @Bean
    public Queue informeQueue(@Value("${messaging.rabbit.queue-informe}") String name) {
        return QueueBuilder.durable(name).build();
    }

    @Bean
    public Queue dbInformeQueue(@Value("${messaging.rabbit.queue-db}") String name) {
        return QueueBuilder.durable(name).build();
    }

    @Bean
    public Binding informeBinding(Queue informeQueue, FanoutExchange informeExchange) {
        return BindingBuilder.bind(informeQueue).to(informeExchange);
    }

    @Bean
    public Binding dbInformeBinding(Queue dbInformeQueue, FanoutExchange informeExchange) {
        return BindingBuilder.bind(dbInformeQueue).to(informeExchange);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
