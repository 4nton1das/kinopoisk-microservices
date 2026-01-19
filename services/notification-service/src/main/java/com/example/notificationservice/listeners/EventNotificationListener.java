package com.example.notificationservice.listeners;

import com.example.kinopoiskeventscontract.events.ContentRatedEvent;
import com.example.kinopoiskeventscontract.events.ReviewCreatedEvent;
import com.example.notificationservice.handler.NotificationWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class EventNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(EventNotificationListener.class);

    private final NotificationWebSocketHandler webSocketHandler;

    public EventNotificationListener(NotificationWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "notify.content-rated.queue", durable = "true"),
                    exchange = @Exchange(name = "content-rated-fanout", type = "fanout")
            )
    )
    public void handleContentRated(@Payload ContentRatedEvent event) {
        String jsonMessage = String.format(
                "{\"type\": \"RATING_UPDATE\", \"contentId\": %d, \"userId\": %d, \"rating\": %d, \"message\": \"Пользователь %d оценил контент %d на %d звезд\"}",
                event.contentId(), event.userId(), event.rating(),
                event.userId(), event.contentId(), event.rating()
        );

        log.info("Отправка JSON уведомления: {}", jsonMessage);
        webSocketHandler.broadcast(jsonMessage);
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "notify.review-created.queue", durable = "true"),
                    exchange = @Exchange(name = "review-created-fanout", type = "fanout")
            )
    )
    public void handleReviewCreated(@Payload ReviewCreatedEvent event) {
        // Создаем JSON сообщение для рецензии
        String jsonMessage = String.format(
                "{\"type\": \"REVIEW_CREATED\", \"contentId\": %d, \"userId\": %d, \"title\": \"%s\", \"isNewReview\": %b, \"message\": \"Пользователь %d %s рецензию на контент %d: \\\"%s\\\"\"}",
                event.contentId(),
                event.userId(),
                escapeJson(event.title()),
                event.isNewReview(),
                event.userId(),
                event.isNewReview() ? "написал" : "обновил",
                event.contentId(),
                escapeJson(event.title())
        );

        log.info("Отправка JSON уведомления о рецензии: {}", jsonMessage);
        webSocketHandler.broadcast(jsonMessage);
    }

    // Вспомогательный метод для экранирования JSON
    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}