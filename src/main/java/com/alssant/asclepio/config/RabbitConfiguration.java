package com.alssant.asclepio.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfiguration {
    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.queue}")
    private String queueName;

    @Value("${app.rabbitmq.binding-pattern}")
    private String bindingPattern;

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    public Queue queue() {
        return QueueBuilder
                .durable(queueName)
                .build();
    }

    @Bean
    public Binding bindingPatient(Queue patientQueue, TopicExchange patientExchange) {
        return BindingBuilder
                .bind(patientQueue)
                .to(patientExchange)
                .with(bindingPattern);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
