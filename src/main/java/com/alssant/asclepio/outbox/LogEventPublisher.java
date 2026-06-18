package com.alssant.asclepio.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogEventPublisher implements EventPublisher {
    private final Logger logger = LoggerFactory.getLogger(LogEventPublisher.class);

    @Override
    public void publish(OutboxEvent event) {
        logger.info("Publishing {}", event.getId());
    }
}
