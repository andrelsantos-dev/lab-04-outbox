package com.alssant.asclepio.consumer;

import com.alssant.asclepio.outbox.dto.EventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PatientEventConsumer {
    private final AuditEntryService service;
    Logger logger = LoggerFactory.getLogger(this.getClass());

    public PatientEventConsumer(AuditEntryService service) {
        this.service = service;
    }

    @RabbitListener(queues = "${app.rabbitmq.queue}")
    public void onReceive(EventEnvelope envelope) {
        logger.info("Received event: [{}]", envelope.metadata().aggregateId());
        service.consumeEvent(envelope);
    }

}
