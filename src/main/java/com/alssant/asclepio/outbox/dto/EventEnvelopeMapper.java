package com.alssant.asclepio.outbox.dto;

import com.alssant.asclepio.outbox.OutboxEvent;
import org.springframework.stereotype.Component;

@Component
public class EventEnvelopeMapper {
    public EventEnvelope map(OutboxEvent event) {
        return new EventEnvelope(
                event.eventMetadata(),
                event.getPayload()
        );
    }
}
