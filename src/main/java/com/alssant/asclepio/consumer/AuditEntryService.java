package com.alssant.asclepio.consumer;

import com.alssant.asclepio.outbox.dto.EventEnvelope;
import com.alssant.asclepio.outbox.dto.EventType;
import com.alssant.asclepio.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class AuditEntryService {
    private final AuditEntryRepository repository;

    public AuditEntryService(AuditEntryRepository repository) {
        this.repository = repository;
    }

    public Optional<AuditEntry> findByAggregateIdAndEventType(UUID aggregateId, EventType eventType) {
        return repository
                .findByAggregateIdAndEventType(aggregateId, eventType);
    }

    public void consumeEvent(EventEnvelope envelope) {
        AuditEntry auditEntry = new AuditEntry();
        auditEntry.setAggregateId(envelope.metadata().aggregateId());
        auditEntry.setTenantId(envelope.metadata().tenantId());
        auditEntry.setEventType(envelope.metadata().eventType());
        auditEntry.setPayload(envelope.payload());
        auditEntry.setAggregateType(envelope.metadata().aggregateType());

        executeAsTenant(envelope.metadata().tenantId().toString(), () -> repository.save(auditEntry));
    }

    protected <T> void executeAsTenant(String tenantId, Supplier<T> action) {
        try {
            TenantContext.setTenantId(tenantId);
            action.get();
        } finally {
            TenantContext.clear();
        }
    }

}
