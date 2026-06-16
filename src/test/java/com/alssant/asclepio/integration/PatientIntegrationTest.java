package com.alssant.asclepio.integration;

import com.alssant.asclepio.support.BaseIntegrationTest;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class PatientIntegrationTest extends BaseIntegrationTest {
    @Autowired
    MockMvc mockMvc;

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
    void shouldRejectInsertWithoutTenant() throws Exception {
        ServletException exception = Assertions.assertThrows(ServletException.class, () -> {
            mockMvc.perform(
                    post("/patients")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                        "name":"Ghost"
                                    }
                                    """)
            );
        });


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

}
