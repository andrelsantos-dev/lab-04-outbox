package com.alssant.asclepio.integration;

import com.alssant.asclepio.outbox.*;
import com.alssant.asclepio.outbox.dto.EventType;
import com.alssant.asclepio.patient.dto.PatientResponse;
import com.alssant.asclepio.support.BaseIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
public class PatientIntegrationTest extends BaseIntegrationTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    OutboxService outboxService;

    @Autowired
    OutboxWorkerRepository workerRepository;

    @Autowired
    OutboxWorker outboxWorker;

    @MockitoSpyBean
    private EventPublisher publisher;


    @Test
    void shouldFilterPatientsByHeader()
            throws Exception {

        mockMvc.perform(
                        get("/patients")
                                .header(
                                        "X-Tenant-Id",
                                        TENANT_A
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.length()"
                        )
                                .value(1)
                );

    }


    @Test
    void shouldNotLeakTenantBetweenRequests() throws Exception {
        String hospitalAId = TENANT_A;
        String hospitalXId = UUID.randomUUID().toString();

        mockMvc.perform(
                        get("/patients")
                                .header("X-Tenant-Id", hospitalAId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Alice"))
                .andExpect(jsonPath("$[0].tenantId").value(hospitalAId))
                .andExpect(jsonPath("$[0].id").exists());


        mockMvc.perform(
                        get("/patients")
                                .header("X-Tenant-Id", hospitalXId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldClearTenantBetweenRequests()
            throws Exception {

        mockMvc.perform(
                        get("/patients")
                                .header(
                                        "X-Tenant-Id",
                                        TENANT_A
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name")
                        .value(hasItem("Alice")))
                .andExpect(jsonPath("$[*].tenantId")
                        .value(
                                everyItem(
                                        equalTo(
                                                TENANT_A
                                        )
                                )));

        mockMvc.perform(
                        get("/patients")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

    }

    @Test
    void shouldInsertPatientIntoCurrentTenant() throws Exception {

        mockMvc.perform(post("/patients")
                        .header("X-Tenant-Id", TENANT_A)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name":
                                  "Charlie"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc
                .perform(get("/patients")
                        .header("X-Tenant-Id", TENANT_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name")
                        .value(hasItem("Charlie")))
                .andExpect(jsonPath("$[*].tenantId")
                        .value(
                                everyItem(
                                        equalTo(
                                                TENANT_A
                                        )
                                )));

    }

    @Test
    void shouldRejectInsertWithoutTenant() {
        ServletException exception = Assertions.assertThrows(ServletException.class, () -> mockMvc.perform(
                post("/patients")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                    "name":"Ghost"
                                }
                                """)
        ));


        assertInstanceOf(

                IllegalStateException.class,
                exception.getCause()
        );

        assertTrue(
                exception
                        .getCause()
                        .getMessage()
                        .contains(
                                "Tenant not set"
                        )
        );
    }

    @Test
    void shouldNotReturnPatientFromAnotherTenant() throws Exception {
        mockMvc.perform(post("/patients")
                        .header("X-Tenant-Id", TENANT_A)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name":
                                  "Dave"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc
                .perform(get("/patients")
                        .header("X-Tenant-Id", TENANT_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name")
                        .value(not(hasItem("Dave"))))
                .andExpect(jsonPath("$[*].tenantId")
                        .value(
                                everyItem(
                                        equalTo(
                                                TENANT_B
                                        )
                                )));

    }

    @Test
    void shouldCreatePatientAndOutboxEvent() throws Exception {
        PatientResponse response = createPatient(TENANT_A, "CLIENT NEW");

        OutboxEvent persistedEvent = findEvent(response.id());

        assertEquals(EventType.PATIENT_CREATED, persistedEvent.getEventType());
        assertEquals(response.id(), persistedEvent.getAggregateId());
    }

    @Test
    void shouldFindPendingEvents() throws Exception {

        PatientResponse response = createPatient(TENANT_A, "CLIENT PENDING");

        List<OutboxEvent> pending = workerRepository.findPending();
        assertFalse(pending.isEmpty());

        OutboxEvent pendingClient = pending
                .stream()
                .filter(event -> response.id().equals(event.getAggregateId()))
                .findFirst()
                .orElseThrow(NoSuchElementException::new);

        assertEquals(EventType.PATIENT_CREATED, pendingClient.getEventType());
        assertEquals(response.id(), pendingClient.getAggregateId());
    }

    @Test
    void shouldPublishPendingEvents(CapturedOutput output) throws Exception {

        createPatient(TENANT_A, "CLIENT PENDING");

        List<OutboxEvent> pending = workerRepository.findPending();
        assertEquals(1, pending.size());

        OutboxEvent event = pending.getFirst();
        assertNull(event.getPublishedAt());

        outboxWorker.processPending();
        List<OutboxEvent> remaining = workerRepository.findPending();
        assertTrue(remaining.isEmpty());
        assertThat(output.getOut()).contains("Publishing ");
    }

    @Test
    void shouldKeepEventPendingWhenPublishFails() throws Exception {
        PatientResponse response = createPatient(TENANT_A, "CLIENT WITH FAIL");

        simulatePublishFailure();

        outboxWorker.processPending();


        OutboxEvent persistedEvent = findEvent(response.id());

        assertNull(persistedEvent.getPublishedAt());
        assertEquals(1, persistedEvent.getAttemptCount());
        assertEquals("Publishing failed", persistedEvent.getLastError());
    }

    private PatientResponse createPatient(String tenant, String patientName) throws Exception {
        String content = "{\"name\": \"%s\"}".formatted(patientName);

        MvcResult result = mockMvc.perform(post("/patients")
                        .header("X-Tenant-Id", tenant)
                        .contentType(APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isCreated())
                .andReturn();

        return mapper.readValue(
                result.getResponse().getContentAsString(),
                PatientResponse.class
        );
    }

    private OutboxEvent findEvent(
            UUID aggregateId
    ) {

        return executeAsTenant(
                TENANT_A,
                () ->
                        outboxService
                                .findByAggregateId(
                                        aggregateId
                                )
                                .orElseThrow()
        );

    }

    private void simulatePublishFailure() {
        doThrow(
                new RuntimeException(
                        "Publishing failed"
                )
        )
                .when(publisher)
                .publish(any(OutboxEvent.class));
    }

}
