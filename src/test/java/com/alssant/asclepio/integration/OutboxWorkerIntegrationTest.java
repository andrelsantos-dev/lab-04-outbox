package com.alssant.asclepio.integration;

import com.alssant.asclepio.outbox.EventPublisher;
import com.alssant.asclepio.outbox.OutboxEvent;
import com.alssant.asclepio.outbox.OutboxWorker;
import com.alssant.asclepio.outbox.OutboxWorkerRepository;
import com.alssant.asclepio.outbox.dto.EventType;
import com.alssant.asclepio.patient.dto.PatientResponse;
import com.alssant.asclepio.support.BaseIntegrationTest;
import org.awaitility.Durations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
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
    @ExtendWith(OutputCaptureExtension.class)
    void shouldPublishPendingEvents(CapturedOutput output) throws Exception {

        createPatient(TENANT_A, "CLIENT PENDING");

        List<OutboxEvent> pending = workerRepository.findPending();
        assertThat(pending).hasSize(1);

        OutboxEvent event = pending.getFirst();
        assertThat(event.getPublishedAt()).isNull();

        outboxWorker.processPending();

        List<OutboxEvent> remaining = workerRepository.findPending();
        assertThat(remaining).isEmpty();

        assertThat(output.getOut()).contains("Publishing ");
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

    protected void simulatePublishFailure(Runnable action) {
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
