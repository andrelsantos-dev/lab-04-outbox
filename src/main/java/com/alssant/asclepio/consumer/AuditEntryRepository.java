package com.alssant.asclepio.consumer;

import com.alssant.asclepio.outbox.dto.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditEntryRepository extends JpaRepository<AuditEntry, UUID> {
    Optional<AuditEntry> findByAggregateIdAndEventType(UUID aggregateId, EventType eventType);
}
