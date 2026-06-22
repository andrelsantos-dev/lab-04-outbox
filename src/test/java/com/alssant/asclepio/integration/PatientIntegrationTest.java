package com.alssant.asclepio.integration;

import com.alssant.asclepio.outbox.OutboxEvent;
import com.alssant.asclepio.outbox.dto.EventType;
import com.alssant.asclepio.patient.dto.PatientResponse;
import com.alssant.asclepio.support.BaseIntegrationTest;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PatientIntegrationTest extends BaseIntegrationTest {

    @Test
    void shouldFilterPatientsByHeader() throws Exception {

        mockMvc
                .perform(
                        get("/patients")
                                .header("X-Tenant-Id", TENANT_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(hasItem("Alice")))
                .andExpect(jsonPath("$[*].tenantId").value(everyItem(equalTo(TENANT_A))));

    }


    @Test
    void shouldNotLeakTenantBetweenRequests() throws Exception {
        final String patientName = "Alice";
        String unknownTenant = UUID.randomUUID().toString();

        mockMvc.perform(
                        get("/patients")
                                .header("X-Tenant-Id", TENANT_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(hasItem(patientName)))
                .andExpect(jsonPath("$[*].tenantId").value(everyItem(equalTo(TENANT_A))));


        mockMvc.perform(
                        get("/patients")
                                .header("X-Tenant-Id", unknownTenant))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldClearTenantBetweenRequests() throws Exception {
        final String patientName = "Alice";

        mockMvc.perform(
                        get("/patients")
                                .header("X-Tenant-Id", TENANT_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(hasItem(patientName)))
                .andExpect(jsonPath("$[*].tenantId").value(everyItem(equalTo(TENANT_A))));

        mockMvc.perform(
                        get("/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

    }

    @Test
    void shouldInsertPatientIntoCurrentTenant() throws Exception {
        final String patientName = "Charlie";
        createPatient(TENANT_A, patientName);

        mockMvc.perform(
                        get("/patients")
                                .header("X-Tenant-Id", TENANT_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(hasItem(patientName)))
                .andExpect(jsonPath("$[*].tenantId").value(everyItem(equalTo(TENANT_A))));

    }

    @Test
    void shouldRejectInsertWithoutTenant() {
        String patientCharlie = """
                {
                  "name": "Charlie"
                }
                """;

        ServletException exception = Assertions.assertThrows(ServletException.class, () ->
                mockMvc.perform(post("/patients")
                        .contentType(APPLICATION_JSON)
                        .content(patientCharlie)));

        assertThat(exception.getCause())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Tenant not set");
    }

    @Test
    void shouldNotReturnPatientFromAnotherTenant() throws Exception {
        final String patientName = "Dave";
        createPatient(TENANT_A, patientName);

        mockMvc.perform(
                        get("/patients")
                                .header("X-Tenant-Id", TENANT_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(not(hasItem(patientName))))
                .andExpect(jsonPath("$[*].tenantId").value(everyItem(equalTo(TENANT_B))));

    }

    @Test
    void shouldCreatePatientAndOutboxEvent() throws Exception {
        PatientResponse response = createPatient(TENANT_A, "CLIENT NEW");

        OutboxEvent persistedEvent = findEvent(response.id());

        assertThat(persistedEvent).isNotNull();
        assertThat(persistedEvent.getEventType()).isEqualTo(EventType.PATIENT_CREATED);
        assertThat(persistedEvent.getAggregateId()).isEqualTo(response.id());
    }

}
