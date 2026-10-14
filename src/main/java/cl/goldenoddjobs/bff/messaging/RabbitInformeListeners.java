package cl.goldenoddjobs.bff.messaging;

import cl.goldenoddjobs.bff.dto.InformeEvent;
import cl.goldenoddjobs.bff.service.InformeService;
import cl.goldenoddjobs.bff.service.NoticeService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;


@Component
@ConditionalOnProperty(name = "messaging.provider", havingValue = "rabbit")
public class RabbitInformeListeners {

    private final InformeService informeService;
    private final NoticeService noticeService;

    public RabbitInformeListeners(InformeService informeService, NoticeService noticeService) {
        this.informeService = informeService;
        this.noticeService = noticeService;
    }

    @RabbitListener(queues = "${messaging.rabbit.queue-informe}")
    public void generar(InformeEvent evento) {
        informeService.generar(evento.eventId());
    }

    @RabbitListener(queues = "${messaging.rabbit.queue-db}")
    public void notificar(InformeEvent evento) {
        noticeService.registrarDesdeEvento(evento);
    }
}
