package com.alssant.asclepio.outbox.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record EventEnvelope(
        EventMetadata metadata,
        JsonNode payload
) {
}
