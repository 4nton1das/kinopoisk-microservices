package com.example.userservice.messaging;

import com.example.kinopoiskeventscontract.events.ContentRatedEvent;
import com.example.kinopoiskeventscontract.events.ReviewCreatedEvent;
import com.example.userservice.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public EventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishContentRated(ContentRatedEvent event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.CONTENT_RATED_FANOUT, "", event);
            log.info("Published ContentRatedEvent: contentId={}, userId={}, rating={}",
                    event.contentId(), event.userId(), event.rating());
        } catch (Exception e) {
            log.error("Failed to publish ContentRatedEvent: {}", event, e);
        }
    }

    public void publishReviewCreated(ReviewCreatedEvent event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.REVIEW_CREATED_FANOUT, "", event);
            log.info("Published ReviewCreatedEvent: contentId={}, userId={}, title={}",
                    event.contentId(), event.userId(), event.title());
        } catch (Exception e) {
            log.error("Failed to publish ReviewCreatedEvent: {}", event, e);
        }
    }
}