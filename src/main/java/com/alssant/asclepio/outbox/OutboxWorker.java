package com.alssant.asclepio.outbox;

import com.alssant.asclepio.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class OutboxWorker {
    private final OutboxWorkerRepository repository;
    private final EventPublisher eventPublisher;
    private final OutboxService outboxService;

    public OutboxWorker(OutboxWorkerRepository repository,
                        EventPublisher eventPublisher, OutboxService outboxService) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.outboxService = outboxService;
    }


    public  void processPending() {
        List<OutboxEvent> events = repository.findPending();

        events.forEach(event -> {
            try {
                TenantContext.setTenantId(event.getTenantId().toString());
                eventPublisher.publish(event);
                event.setPublishedAt(Instant.now());
                outboxService.save(event);
            } catch (Exception e) {
                event.incrementAttemptCount();
                event.setLastError(e.getMessage());

                outboxService.save(event);

            } finally {

                TenantContext.clear();
            }
        });
    }
}
