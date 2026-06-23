package com.alssant.asclepio.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "worker.outbox.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OutboxScheduler {
    private final OutboxWorker worker;

    public OutboxScheduler(OutboxWorker worker) {
        this.worker = worker;
    }

    @Scheduled(fixedDelayString = "${worker.outbox.delay-ms}", initialDelay = 1000)
    public void processPendingEvents() {
        worker.processPending();
    }
}
