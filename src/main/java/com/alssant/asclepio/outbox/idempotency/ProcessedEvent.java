package com.alssant.asclepio.outbox.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity(name = "processed_events")
public class ProcessedEvent {
    @Id
    private UUID eventId;

    @Column
    private Instant processedAt;

    public ProcessedEvent() {
    }

    public ProcessedEvent(UUID eventId, Instant processedAt) {
        this.eventId = eventId;
        this.processedAt = processedAt;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
}
