package com.alssant.asclepio.outbox.dto;

import java.util.UUID;

public record EventMetadata(
        EventType eventType,
        UUID aggregateId,
        String aggregateType
) {
}
