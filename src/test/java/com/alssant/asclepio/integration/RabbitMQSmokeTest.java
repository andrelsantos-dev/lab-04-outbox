package com.alssant.asclepio.integration;

import com.alssant.asclepio.support.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class RabbitMQSmokeTest extends BaseIntegrationTest {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Test
    void shouldSendAndReceiveMessagesWithRabbitMQ() {
        assertThat(rabbitTemplate).isNotNull();
        assertThat(amqpAdmin).isNotNull();

        String queueName = "fila-smoke-test-43";
        String message = "{\"message\":\"any message\"}";

        Queue tempQueue = QueueBuilder
                .durable(queueName)
                .autoDelete()
                .build();
        amqpAdmin.declareQueue(tempQueue);

        rabbitTemplate.convertAndSend(queueName, message);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            Object received = rabbitTemplate.receiveAndConvert(queueName);

            assertThat(received).isNotNull();
            assertThat(received.toString()).isEqualTo(message);
        });
    }
}
