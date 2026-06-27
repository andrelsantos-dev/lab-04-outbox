package com.alssant.asclepio.outbox.dto;

public enum EventType {
    PATIENT_CREATED("patient.created");

    private final String routingKey;

    EventType(String routingKey) {
        this.routingKey = routingKey;
    }

    public String getRoutingKey() {
        return routingKey;
    }
}
