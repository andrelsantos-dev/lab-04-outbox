package com.alssant.asclepio.integration;

import com.alssant.asclepio.patient.Patient;
import com.alssant.asclepio.patient.PatientRepository;
import com.alssant.asclepio.support.BaseIntegrationTest;
import com.alssant.asclepio.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class PatientRepositoryIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private PatientRepository repository;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void shouldReturnOnlyPatientsFromHospitalA() {
        TenantContext.setTenantId(TENANT_A);

        List<Patient> patients = repository.findAll();

        assertFalse(patients.isEmpty());
        assertThat(patients)
                .isNotEmpty()
                .extracting(Patient::getTenantId)
                .containsOnly(UUID.fromString(TENANT_A));

        assertThat(patients)
                .extracting(Patient::getName)
                .contains("Alice");
    }

    @Test
    void shouldReturnOnlyPatientsFromHospitalB() {
        TenantContext.setTenantId(TENANT_B);

        List<Patient> patients = repository.findAll();

        assertFalse(patients.isEmpty());
        assertThat(patients)
                .isNotEmpty()
                .extracting(Patient::getTenantId)
                .containsOnly(UUID.fromString(TENANT_B));

        assertThat(patients)
                .extracting(Patient::getName)
                .contains("Bob");
    }

    @Test
    void shouldReturnEmptyWhenTenantHasNoPatients() {
        TenantContext.setTenantId(UUID.randomUUID().toString());

        List<Patient> patients = repository.findAll();

        assertTrue(patients.isEmpty());
    }

}
