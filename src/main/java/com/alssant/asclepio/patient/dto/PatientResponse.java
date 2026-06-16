package com.alssant.asclepio.patient.dto;

import java.util.UUID;

public record PatientResponse(
        UUID id,
        UUID tenantId,
        String name
) {
}
