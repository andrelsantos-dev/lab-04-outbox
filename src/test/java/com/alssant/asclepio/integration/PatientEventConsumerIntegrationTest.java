package com.alssant.asclepio.integration;

import com.alssant.asclepio.config.RabbitProperties;
import com.alssant.asclepio.consumer.AuditEntry;
import com.alssant.asclepio.consumer.AuditEntryService;
import com.alssant.asclepio.outbox.dto.EventEnvelope;
import com.alssant.asclepio.outbox.dto.EventMetadata;
import com.alssant.asclepio.outbox.dto.EventType;
import com.alssant.asclepio.patient.dto.PatientResponse;
import com.alssant.asclepio.support.BaseIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.awaitility.Durations;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;

public class PatientEventConsumerIntegrationTest extends BaseIntegrationTest {
    @MockitoSpyBean
    AuditEntryService service;

    @Autowired
    private RabbitProperties properties;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Test
    void shouldConsumePublishedPatientEvent() throws Exception {
        final String patientName = "CLIENT_" + UUID.randomUUID();
        PatientResponse response = createPatient(TENANT_A, patientName);

        await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
            validateCreatedPatient(response, TENANT_A);
        });
    }

    @Test
    void shouldRestoreTenantContextBeforeProcessing() throws Exception {
        final String patientNameA = "CLIENT_" + UUID.randomUUID();
        PatientResponse responseA = createPatient(TENANT_A, patientNameA);

        final String patientNameB = "CLIENT_" + UUID.randomUUID();
        PatientResponse responseB = createPatient(TENANT_B, patientNameB);

        await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
            validateCreatedPatient(responseA, TENANT_A);
            validateCreatedPatient(responseB, TENANT_B);
        });

    }

    @Test
    void shouldRetryConsumptionWhenAuditFails() throws Exception {
        final String patientName = "CLIENT_" + UUID.randomUUID();
        PatientResponse response = createPatient(TENANT_A, patientName);

        Mockito.doThrow(new RuntimeException("Failed to process event"))
                .doCallRealMethod()
                .when(service).consumeEvent(any());

        await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
            validateCreatedPatient(response, TENANT_A);
        });

        Mockito.verify(service, Mockito.times(2)).consumeEvent(any());

    }

    @Test
    void shouldNotRetryWhenMessageIsInvalid() throws Exception {
        final String patientName01 = "CLIENT_01";
        PatientResponse response1 = createPatient(TENANT_A, patientName01);

        final String patientName02 = "CLIENT_02";
        PatientResponse response2 = createPatient(TENANT_A, patientName02);

        Mockito.doThrow(new AmqpRejectAndDontRequeueException("Failed to process event"))
                .doCallRealMethod()
                .when(service).consumeEvent(any());

        await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
            validateCreatedPatient(response2, TENANT_A);

            Optional<AuditEntry> auditEntry = service.findByAggregateIdAndEventType(response1.id(), EventType.PATIENT_CREATED);
            assertThat(auditEntry.isPresent()).isFalse();
        });

        Mockito.verify(service, Mockito.times(2)).consumeEvent(any());

    }

    @Test
    void shouldSendMessageToDeadLetterQueue() throws Exception {
        final String patientName = "CLIENT_" + UUID.randomUUID();
        PatientResponse response1 = createPatient(TENANT_A, patientName);

        Mockito.doThrow(new AmqpRejectAndDontRequeueException("Failed to process event"))
                .doCallRealMethod()
                .when(service).consumeEvent(any());

        await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
            var props = amqpAdmin.getQueueProperties(properties.deadLetterQueue());
            assertThat(props).isNotNull();
            assertThat((int) props.get(RabbitAdmin.QUEUE_MESSAGE_COUNT)).isEqualTo(1);
        });

        EventEnvelope received = rabbitTemplate.receiveAndConvert(properties.deadLetterQueue(), new ParameterizedTypeReference<EventEnvelope>() {
        });

        assertThat(received).isNotNull();
        EventMetadata metadata = received.metadata();
        JsonNode payload = received.payload();

        assertThat(metadata).isNotNull();
        assertThat(payload).isNotNull();

        assertThat(payload.get("name").asText()).isEqualTo(patientName);
        assertThat(metadata.eventType()).isEqualTo(EventType.PATIENT_CREATED);

        Optional<AuditEntry> auditEntry = service.findByAggregateIdAndEventType(response1.id(), EventType.PATIENT_CREATED);
        assertThat(auditEntry).isEmpty();
    }

    private void validateCreatedPatient(PatientResponse response, String tenantId) {
        executeAsTenant(tenantId, () -> {
            Optional<AuditEntry> auditEntry = service.findByAggregateIdAndEventType(response.id(), EventType.PATIENT_CREATED);
            assertThat(auditEntry.isPresent()).isTrue();

            AuditEntry entry = auditEntry.get();

            assertThat(entry.getEventType()).isEqualTo(EventType.PATIENT_CREATED);
            assertThat(entry.getAggregateId()).isEqualTo(response.id());
            assertThat(entry.getTenantId()).isEqualTo(UUID.fromString(tenantId));
            assertThat(entry.getPayload().get("name").asText()).isEqualTo(response.name());
            return null;
        });
    }
}
