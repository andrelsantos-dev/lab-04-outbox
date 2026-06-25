package com.alssant.asclepio.outbox;

import com.alssant.asclepio.outbox.idempotency.IdempotencyService;
import com.alssant.asclepio.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class OutboxWorker {
    private final OutboxWorkerRepository repository;
    private final EventPublisher eventPublisher;
    private final OutboxService outboxService;
    private final IdempotencyService idempotencyService;

    public OutboxWorker(OutboxWorkerRepository repository,
                        EventPublisher eventPublisher,
                        OutboxService outboxService,
                        IdempotencyService idempotencyService) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.outboxService = outboxService;
        this.idempotencyService = idempotencyService;
    }


    public void processPending() {
        List<OutboxEvent> events = repository.findPending();

        events.forEach(this::processSingleEvent);
    }

    private void processSingleEvent(OutboxEvent event) {
        executeAsTenant(event.getTenantId().toString(), () -> {
            try {
                if (idempotencyService.alreadyProcessed(event.getId())) {
                    publishCompleted(event);
                    return;
                }

                eventPublisher.publish(event);
                idempotencyService.markProcessed(event.getId());
                publishCompleted(event);
            } catch (Exception e) {
                markError(event, e);
            }
        });

    }

    private void markError(OutboxEvent event, Exception e) {
        event.incrementAttemptCount();
        event.setLastError(e.getMessage());

        outboxService.save(event);
    }

    private void publishCompleted(OutboxEvent event) {
        event.setPublishedAt(Instant.now());
        outboxService.save(event);
    }

    protected void executeAsTenant(String tenantId, Runnable action) {
        try {
            TenantContext.setTenantId(tenantId);
            action.run();
        } finally {
            TenantContext.clear();
        }
    }
}
