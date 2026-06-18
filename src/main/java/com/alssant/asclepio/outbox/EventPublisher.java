package com.alssant.asclepio.outbox;


@FunctionalInterface
public interface EventPublisher {
    void publish(OutboxEvent event);
}
