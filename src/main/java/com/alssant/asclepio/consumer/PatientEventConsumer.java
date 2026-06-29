package com.alssant.asclepio.consumer;

import com.alssant.asclepio.outbox.dto.EventEnvelope;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PatientEventConsumer {
    private final AuditEntryService service;

    public PatientEventConsumer(AuditEntryService service) {
        this.service = service;
    }

    @RabbitListener(queues = "${app.rabbitmq.queue}")
    public void onReceive(EventEnvelope envelope) {
        service.consumeEvent(envelope);
    }

}
