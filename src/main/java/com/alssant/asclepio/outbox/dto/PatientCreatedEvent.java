package com.alssant.asclepio.outbox.dto;

import java.util.UUID;

public record PatientCreatedEvent(
        UUID patientId,
        String name
) {

}
