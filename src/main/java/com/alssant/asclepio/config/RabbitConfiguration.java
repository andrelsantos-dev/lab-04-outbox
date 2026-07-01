package com.alssant.asclepio.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableConfigurationProperties(RabbitProperties.class)
public class RabbitConfiguration {
    private final RabbitProperties properties;

    public RabbitConfiguration(RabbitProperties properties) {
        this.properties = properties;
    }

    @Bean
    @Primary
    public TopicExchange topicExchange() {
        return new TopicExchange(properties.exchange());
    }

    @Bean
    @Primary
    public Queue queue() {
        return QueueBuilder
                .durable(properties.queue())
                .deadLetterExchange(properties.deadLetterExchange())
                .deadLetterRoutingKey(properties.deadLetterRoutingKey())
                .build();
    }

    @Bean
    public Binding bindingPatient(
            Queue patientQueue,
            TopicExchange patientExchange) {
        return BindingBuilder
                .bind(patientQueue)
                .to(patientExchange)
                .with(properties.bindingPattern());
    }

    @Bean("deadLetterExchange")
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(properties.deadLetterExchange());
    }

    @Bean("deadLetterQueue")
    public Queue deadLetterQueue() {
        return QueueBuilder
                .durable(properties.deadLetterQueue())
                .build();
    }

    @Bean("bindingDeadLetterPatient")
    public Binding bindingDeadLetterPatient(
            @Qualifier("deadLetterQueue") Queue deadLetterQueue,
            @Qualifier("deadLetterExchange") TopicExchange deadLetterExchange) {
        return BindingBuilder
                .bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(properties.deadLetterRoutingKey());
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
