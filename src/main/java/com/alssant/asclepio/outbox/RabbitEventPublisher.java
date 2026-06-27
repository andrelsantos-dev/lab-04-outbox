package com.alssant.asclepio.outbox;

import com.alssant.asclepio.outbox.dto.EventEnvelope;
import com.alssant.asclepio.outbox.dto.EventEnvelopeMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RabbitEventPublisher implements EventPublisher {
    private final EventEnvelopeMapper  mapper;
    private final RabbitTemplate rabbitTemplate;
    private final String exchangeName;

    public RabbitEventPublisher(EventEnvelopeMapper mapper,
                                RabbitTemplate rabbitTemplate,
                                @Value("${app.rabbitmq.exchange}")
                                String exchangeName) {
        this.mapper = mapper;
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeName = exchangeName;
    }

    @Override
    public void publish(OutboxEvent event) {
        EventEnvelope envelope = this.mapper.map(event);
        rabbitTemplate.convertAndSend(exchangeName,
                event.getEventType().getRoutingKey(),
                envelope);
    }

}
