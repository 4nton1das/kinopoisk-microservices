package com.example.userservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ===== EXCHANGES =====

    // Fanout Exchange для отправки событий
    public static final String CONTENT_RATED_FANOUT = "content-rated-fanout";
    public static final String REVIEW_CREATED_FANOUT = "review-created-fanout";

    // Topic Exchange для получения событий
    public static final String KINOPOISK_EXCHANGE = "kinopoisk-exchange";

    // ===== ROUTING KEYS =====
    public static final String ROUTING_KEY_CONTENT_CREATED = "content.created";
    public static final String ROUTING_KEY_CONTENT_DELETED = "content.deleted";

    // ===== QUEUE NAMES =====
    public static final String USER_SERVICE_CONTENT_CREATED_QUEUE = "user-service.content-created.queue";
    public static final String USER_SERVICE_CONTENT_DELETED_QUEUE = "user-service.content-deleted.queue";

    // 1. Topic Exchange для получения событий от kinopoisk-service
    @Bean
    public TopicExchange kinopoiskExchange() {
        return new TopicExchange(KINOPOISK_EXCHANGE, true, false);
    }

    // 2. Fanout Exchange для отправки событий
    @Bean
    public FanoutExchange contentRatedExchange() {
        // durable=true - Exchange сохранится после перезагрузки RabbitMQ
        // autoDelete=false - не удалять при отсутствии подписчиков
        return new FanoutExchange(CONTENT_RATED_FANOUT, true, false);
    }

    // Объявляем Fanout Exchange для рецензий
    @Bean
    public FanoutExchange reviewCreatedExchange() {
        return new FanoutExchange(REVIEW_CREATED_FANOUT, true, false);
    }

    // 3. Очереди для подписки на события контента
    @Bean
    public Queue contentCreatedQueue() {
        return new Queue(USER_SERVICE_CONTENT_CREATED_QUEUE, true);
    }

    @Bean
    public Queue contentDeletedQueue() {
        return new Queue(USER_SERVICE_CONTENT_DELETED_QUEUE, true);
    }

    // 4. Привязка очередей к Topic Exchange
    @Bean
    public Binding contentCreatedBinding() {
        return BindingBuilder.bind(contentCreatedQueue())
                .to(kinopoiskExchange())
                .with(ROUTING_KEY_CONTENT_CREATED);
    }

    @Bean
    public Binding contentDeletedBinding() {
        return BindingBuilder.bind(contentDeletedQueue())
                .to(kinopoiskExchange())
                .with(ROUTING_KEY_CONTENT_DELETED);
    }

    // 5. Конвертер и RabbitTemplate
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}