package com.alssant.asclepio.support;

import com.alssant.asclepio.integration.AbstractPostgresContainer;
import com.alssant.asclepio.outbox.OutboxEvent;
import com.alssant.asclepio.outbox.OutboxService;
import com.alssant.asclepio.patient.dto.PatientResponse;
import com.alssant.asclepio.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.function.Supplier;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest extends AbstractPostgresContainer {
    protected static final String TENANT_A = "11111111-1111-1111-1111-111111111111";
    protected static final String TENANT_B = "22222222-2222-2222-2222-222222222222";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper mapper;

    @Autowired
    OutboxService outboxService;

    protected <T> T executeAsTenant(String tenantId, Supplier<T> action) {
        try {
            TenantContext.setTenantId(tenantId);
            return action.get();
        } finally {
            TenantContext.clear();
        }
    }

    protected PatientResponse createPatient(String tenant, String patientName) throws Exception {
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

    protected OutboxEvent findEvent(
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

}
