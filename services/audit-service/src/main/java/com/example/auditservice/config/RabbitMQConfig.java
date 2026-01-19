package com.example.auditservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitMQConfig {

    @Bean
    public DefaultClassMapper classMapper() {
        DefaultClassMapper classMapper = new DefaultClassMapper();
        classMapper.setTrustedPackages("com.example.kinopoiskeventscontract.events.*");

        classMapper.setIdClassMapping(Map.of(
                // События Topic Exchange (из kinopoisk-service)
                "com.example.kinopoiskeventscontract.events.ContentCreatedEvent",
                com.example.kinopoiskeventscontract.events.ContentCreatedEvent.class,
                "com.example.kinopoiskeventscontract.events.ContentDeletedEvent",
                com.example.kinopoiskeventscontract.events.ContentDeletedEvent.class,
                "com.example.kinopoiskeventscontract.events.PersonCreatedEvent",
                com.example.kinopoiskeventscontract.events.PersonCreatedEvent.class,
                "com.example.kinopoiskeventscontract.events.PersonDeletedEvent",
                com.example.kinopoiskeventscontract.events.PersonDeletedEvent.class,

                // События Fanout Exchange (из user-service)
                "com.example.kinopoiskeventscontract.events.ContentRatedEvent",
                com.example.kinopoiskeventscontract.events.ContentRatedEvent.class,
                "com.example.kinopoiskeventscontract.events.ReviewCreatedEvent",
                com.example.kinopoiskeventscontract.events.ReviewCreatedEvent.class
        ));
        return classMapper;
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter(DefaultClassMapper classMapper) {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setClassMapper(classMapper);
        return converter;
    }
}
