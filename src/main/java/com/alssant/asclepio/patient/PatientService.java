package com.alssant.asclepio.patient;

import com.alssant.asclepio.outbox.OutboxService;
import com.alssant.asclepio.outbox.dto.EventMetadata;
import com.alssant.asclepio.outbox.dto.EventType;
import com.alssant.asclepio.outbox.dto.PatientCreatedEvent;
import com.alssant.asclepio.patient.dto.CreatePatientRequest;
import com.alssant.asclepio.patient.dto.PatientResponse;
import com.alssant.asclepio.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class PatientService {
    private final PatientRepository repository;
    private final OutboxService outboxService;

    public PatientService(PatientRepository repository, OutboxService outboxService) {
        this.repository = repository;
        this.outboxService = outboxService;
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> findAll() {
        return repository
                .findAll()
                .stream()
                .map(
                        patient -> new PatientResponse(
                                patient.getId(),
                                patient.getTenantId(),
                                patient.getName()
                        )).toList();
    }

    @Transactional
    public PatientResponse create(CreatePatientRequest request) {
        if (!StringUtils.hasText(TenantContext.getTenantId())) {
            throw new IllegalStateException("Tenant not set");
        }

        if (!StringUtils.hasText(request.name())) {
            throw new IllegalStateException("Name not set");
        }

        Patient p = repository.save(new Patient(request.name()));
        outboxService
                .record(
                        new PatientCreatedEvent(p.getId(), p.getName()),
                        new EventMetadata(EventType.PATIENT_CREATED, p.getId(), "PATIENT", null));

        return new PatientResponse(
                p.getId(),
                p.getTenantId(),
                p.getName()
        );
    }
}
