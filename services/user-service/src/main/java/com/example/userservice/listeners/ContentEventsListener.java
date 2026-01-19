package com.example.userservice.listeners;

import com.example.kinopoiskeventscontract.events.ContentCreatedEvent;
import com.example.kinopoiskeventscontract.events.ContentDeletedEvent;
import com.example.userservice.config.RabbitMQConfig;
import com.example.userservice.storage.InMemoryRatingStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ContentEventsListener {

    private static final Logger log = LoggerFactory.getLogger(ContentEventsListener.class);
    private final InMemoryRatingStorage storage;

    public ContentEventsListener(InMemoryRatingStorage storage) {
        this.storage = storage;
    }

    @RabbitListener(queues = RabbitMQConfig.USER_SERVICE_CONTENT_CREATED_QUEUE)
    public void handleContentCreated(ContentCreatedEvent event) {
        log.info("Content created event received: ID={}, Title='{}'",
                event.contentId(), event.title());

        // Добавляем контент в кэш существующих
        storage.addContentId(event.contentId());
    }

    @RabbitListener(queues = RabbitMQConfig.USER_SERVICE_CONTENT_DELETED_QUEUE)
    public void handleContentDeleted(ContentDeletedEvent event) {
        log.info("Content deleted event received: ID={}", event.contentId());

        // Удаляем контент из кэша и все связанные данные
        storage.removeContentAndData(event.contentId());
    }
}
