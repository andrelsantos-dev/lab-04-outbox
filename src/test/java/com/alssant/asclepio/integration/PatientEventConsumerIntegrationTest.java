package com.alssant.asclepio.integration;

import com.alssant.asclepio.consumer.AuditEntry;
import com.alssant.asclepio.consumer.AuditEntryService;
import com.alssant.asclepio.outbox.dto.EventType;
import com.alssant.asclepio.patient.dto.PatientResponse;
import com.alssant.asclepio.support.BaseIntegrationTest;
import org.awaitility.Durations;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class PatientEventConsumerIntegrationTest extends BaseIntegrationTest {
    @Autowired
    AuditEntryService service;

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
