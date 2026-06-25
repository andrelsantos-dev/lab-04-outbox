package com.alssant.asclepio.outbox.idempotency;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class IdempotencyService {
    private final ProcessedEventRepository repository;

    public IdempotencyService(ProcessedEventRepository repository) {
        this.repository = repository;
    }

    public boolean alreadyProcessed(UUID eventId) {
        return repository.existsById(eventId);
    }

    public void markProcessed(UUID eventId){
        ProcessedEvent processed = new ProcessedEvent(eventId, Instant.now());
        repository.save(processed);
    }
}
