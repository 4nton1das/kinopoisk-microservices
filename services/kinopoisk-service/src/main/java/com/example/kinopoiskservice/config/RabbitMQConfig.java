package com.example.kinopoiskservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    // Topic Exchange
    public static final String EXCHANGE_NAME = "kinopoisk-exchange";
    public static final String ROUTING_KEY_CONTENT_CREATED = "content.created";
    public static final String ROUTING_KEY_CONTENT_DELETED = "content.deleted";
    public static final String ROUTING_KEY_PERSON_CREATED = "person.created";
    public static final String ROUTING_KEY_PERSON_DELETED = "person.deleted";

    // Fanout Exchange
    public static final String CONTENT_RATED_FANOUT = "content-rated-fanout";
    public static final String REVIEW_CREATED_FANOUT = "review-created-fanout";

    @Bean
    public TopicExchange kinopoiskExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public FanoutExchange contentRatedExchange() {
        return new FanoutExchange(CONTENT_RATED_FANOUT, true, false);
    }

    @Bean
    public FanoutExchange reviewCreatedExchange() {
        return new FanoutExchange(REVIEW_CREATED_FANOUT, true, false);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter(new ObjectMapper().findAndRegisterModules());
    }
}
