package com.alssant.asclepio.outbox;

import com.alssant.asclepio.outbox.dto.EventMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class OutboxService {
    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public Optional<OutboxEvent> findByAggregateId(UUID aggregateId) {
        return repository.findByAggregateId(aggregateId);
    }

    public <T> void record(T payload, EventMetadata metadata) {

        OutboxEvent event = new OutboxEvent();
        event.setEventType(metadata.eventType());
        event.setAggregateType(metadata.aggregateType());
        event.setAggregateId(metadata.aggregateId());
        event.setPayload(objectMapper.valueToTree(payload));

        repository.save(event);
    }

}
