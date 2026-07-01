package com.alssant.asclepio.integration;

import com.alssant.asclepio.outbox.EventPublisher;
import com.alssant.asclepio.outbox.OutboxEvent;
import com.alssant.asclepio.outbox.OutboxWorker;
import com.alssant.asclepio.outbox.OutboxWorkerRepository;
import com.alssant.asclepio.outbox.dto.EventType;
import com.alssant.asclepio.outbox.idempotency.IdempotencyService;
import com.alssant.asclepio.patient.dto.PatientResponse;
import com.alssant.asclepio.support.BaseIntegrationTest;
import org.awaitility.Durations;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class OutboxWorkerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    OutboxWorkerRepository workerRepository;

    @MockitoSpyBean
    OutboxWorker outboxWorker;

    @MockitoSpyBean
    private EventPublisher publisher;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${worker.outbox.max-attempts}")
    private int maxAttempts;

    @Value("${app.rabbitmq.queue}")
    private String queueName;


    @Test
    void shouldFindPendingEvents() throws Exception {

        PatientResponse response = createPatient(TENANT_A, "CLIENT PENDING");

        List<OutboxEvent> pending = workerRepository.findPending();
        assertThat(pending).isNotEmpty();

        OutboxEvent pendingClient = pending.stream()
                .filter(event -> response.id().equals(event.getAggregateId()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Pending event not found for patient: " + response.id()));

        assertThat(pendingClient.getEventType()).isEqualTo(EventType.PATIENT_CREATED);
        assertThat(pendingClient.getAggregateId()).isEqualTo(response.id());
    }

    @Test
    void shouldKeepEventPendingWhenPublishFails() throws Exception {
        final String patientName = "CLIENT_" + UUID.randomUUID();

        PatientResponse response = createPatient(TENANT_A, patientName);
        simulatePublishFailure(outboxWorker::processPending);

        OutboxEvent persistedEvent = findEvent(response.id());

        assertThat(persistedEvent.getPublishedAt()).isNull();
        assertThat(persistedEvent.getAttemptCount()).isEqualTo(1);
        assertThat(persistedEvent.getLastError()).isEqualTo("Publishing failed");
    }

    @Test
    void shouldProcessPendingEventsAutomatically() throws Exception {
        final String patientName = "CLIENT_" + UUID.randomUUID();

        PatientResponse response = createPatient(TENANT_A, patientName);

        await().atMost(Durations.TEN_SECONDS).untilAsserted(() -> {
            verify(outboxWorker, atLeastOnce()).processPending();
            OutboxEvent persistedEvent = findEvent(response.id());
            assertThat(persistedEvent.getPublishedAt()).isNotNull();
        });
    }

    @Test
    void shouldMarkEventAsProcessed() throws Exception {
        final String patientName = "CLIENT_" + UUID.randomUUID();

        PatientResponse response = createPatient(TENANT_A, patientName);
        outboxWorker.processPending();

        OutboxEvent persistedEvent = findEvent(response.id());
        Boolean processedEvent = idempotencyService.alreadyProcessed(persistedEvent.getId());

        assertThat(persistedEvent.getPublishedAt()).isNotNull();
        assertThat(processedEvent).isTrue();
    }

    @Test
    void shouldSkipAlreadyProcessedEvent() throws Exception {
        final String patientName = "CLIENT_" + UUID.randomUUID();

        PatientResponse response = createPatient(TENANT_A, patientName);
        OutboxEvent persistedEvent = findEvent(response.id());
        idempotencyService.markProcessed(persistedEvent.getId());

        outboxWorker.processPending();

        Mockito.verify(publisher, never()).publish(any());
        OutboxEvent updatedEvent = findEvent(response.id());
        assertThat(updatedEvent.getPublishedAt())
                .as("already processed event should be removed from pending state")
                .isNotNull();
    }

    @Test
    void shouldKeepEventPendingBeforeMaxAttempts() throws Exception {
        final String patientName = "CLIENT_" + UUID.randomUUID();
        PatientResponse response = createPatient(TENANT_A, patientName);

        simulatePublishFailure(() -> {
            for (int i = 1; i < maxAttempts; i++) {
                outboxWorker.processPending();
            }
        });

        OutboxEvent persistedEvent = findEvent(response.id());

        assertThat(persistedEvent.getPublishedAt()).isNull();
        assertThat(persistedEvent.getFailedAt()).isNull();
        assertThat(persistedEvent.getAttemptCount()).isEqualTo(maxAttempts - 1);
    }

    @Test
    void shouldMoveEventToDeadLetterAfterMaxAttempts() throws Exception {
        final String patientName = "CLIENT_" + UUID.randomUUID();
        PatientResponse response = createPatient(TENANT_A, patientName);

        // Simulate scheduler executions while the event is still eligible for retry.
        for (int i = 1; i <= maxAttempts; i++) {
            simulatePublishFailure(outboxWorker::processPending);
        }

        OutboxEvent persistedEvent = findEvent(response.id());

        assertThat(persistedEvent.getPublishedAt()).isNull();
        assertThat(persistedEvent.getFailedAt()).isNotNull();
        assertThat(persistedEvent.getDeadLetter()).isTrue();
        assertThat(persistedEvent.getAttemptCount()).isEqualTo(maxAttempts);
    }

    private void simulatePublishFailure(Runnable action) {
        try {
            doThrow(new RuntimeException("Publishing failed"))
                    .when(publisher)
                    .publish(any(OutboxEvent.class));

            action.run();
        } finally {
            Mockito.reset(publisher);
        }
    }

}
