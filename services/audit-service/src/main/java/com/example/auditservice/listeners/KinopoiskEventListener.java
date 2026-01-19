package com.example.auditservice.listeners;

import com.example.kinopoiskeventscontract.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import com.rabbitmq.client.Channel;

import java.io.IOException;

@Component
public class KinopoiskEventListener {
    private static final Logger log = LoggerFactory.getLogger(KinopoiskEventListener.class);

    // ========== ТОПИК EXCHANGE ОБРАБОТЧИКИ (из kinopoisk-service) ==========

    // 1. Создание контента
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "audit-content-created-queue", durable = "true",
                            arguments = {
                                    @Argument(name = "x-dead-letter-exchange", value = "dlx-exchange"),
                                    @Argument(name = "x-dead-letter-routing-key", value = "dlq.audit")
                            }
                    ),
                    exchange = @Exchange(name = "kinopoisk-exchange", type = "topic"),
                    key = "content.created"
            )
    )
    public void handleContentCreated(@Payload ContentCreatedEvent event, Channel channel,
                                     @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.info("Content created event received: ID={}, Title='{}'",
                    event.contentId(), event.title());

            // Тестирование DLQ: если в названии есть "CRASH", имитируем ошибку
            if (event.title() != null && event.title().toUpperCase().contains("CRASH")) {
                log.warn("Simulating processing error for DLQ test - title contains 'CRASH'");
                throw new RuntimeException("Simulating processing error for DLQ test - title contains 'CRASH'");
            }

            log.info("AUDIT: New content created - {}", event.title());
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("Audit processing failed for event: {}. Sending to DLQ.", event, e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("Failed to nack message", ioException);
            }
        }
    }

    // 2. Удаление контента
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "audit-content-deleted-queue", durable = "true",
                            arguments = {
                                    @Argument(name = "x-dead-letter-exchange", value = "dlx-exchange"),
                                    @Argument(name = "x-dead-letter-routing-key", value = "dlq.audit")
                            }
                    ),
                    exchange = @Exchange(name = "kinopoisk-exchange", type = "topic"),
                    key = "content.deleted"
            )
    )
    public void handleContentDeleted(@Payload ContentDeletedEvent event, Channel channel,
                                     @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.info("Content deleted event received: ID={}", event.contentId());
            log.info("AUDIT: Content deleted - ID: {}", event.contentId());
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("Audit processing failed for event: {}. Sending to DLQ.", event, e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("Failed to nack message", ioException);
            }
        }
    }

    // 3. Создание персоны
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "audit-person-created-queue", durable = "true",
                            arguments = {
                                    @Argument(name = "x-dead-letter-exchange", value = "dlx-exchange"),
                                    @Argument(name = "x-dead-letter-routing-key", value = "dlq.audit")
                            }
                    ),
                    exchange = @Exchange(name = "kinopoisk-exchange", type = "topic"),
                    key = "person.created"
            )
    )
    public void handlePersonCreated(@Payload PersonCreatedEvent event, Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("Person created event received: ID={}, Name='{}'",
                    event.personId(), event.primaryName());
            log.info("AUDIT: New person created - {}", event.primaryName());
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("Audit processing failed for event: {}. Sending to DLQ.", event, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    // 4. Удаление персоны
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "audit-person-deleted-queue", durable = "true",
                            arguments = {
                                    @Argument(name = "x-dead-letter-exchange", value = "dlx-exchange"),
                                    @Argument(name = "x-dead-letter-routing-key", value = "dlq.audit")
                            }
                    ),
                    exchange = @Exchange(name = "kinopoisk-exchange", type = "topic"),
                    key = "person.deleted"
            )
    )
    public void handlePersonDeleted(@Payload PersonDeletedEvent event, Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("Person deleted event received: ID={}", event.personId());
            log.info("AUDIT: Person deleted - ID: {}", event.personId());
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("Audit processing failed for event: {}. Sending to DLQ.", event, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    // ========== FANOUT EXCHANGE ОБРАБОТЧИКИ (из user-service) ==========

    // 5. Оценка контента (Fanout Exchange)
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "audit-content-rated-queue", durable = "true"),
                    exchange = @Exchange(name = "content-rated-fanout", type = "fanout")
            )
    )
    public void handleContentRated(@Payload ContentRatedEvent event) {
        log.info("FANOUT AUDIT: Пользователь {} оценил контент {} на {} звезд. " +
                        "Средний рейтинг теперь: {} (на основе {} оценок)",
                event.userId(), event.contentId(), event.rating(),
                String.format("%.2f", event.averageRating()), event.totalRatings());
    }

    // 6. Создание/обновление рецензии (Fanout Exchange)
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "audit-review-created-queue", durable = "true"),
                    exchange = @Exchange(name = "review-created-fanout", type = "fanout")
            )
    )
    public void handleReviewCreated(@Payload ReviewCreatedEvent event) {
        String action = event.isNewReview() ? "написал" : "обновил";
        log.info("FANOUT AUDIT: Пользователь {} {} рецензию на контент {} (ID рецензии: {}): \"{}\" (оценка: {} звезд)",
                event.userId(), action, event.contentId(), event.reviewId(), event.title(), event.rating());
    }

    // Обработчик для DLQ (для отладки)
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "audit-content-created-queue.dlq", durable = "true"),
                    exchange = @Exchange(name = "dlx-exchange", type = "topic"),
                    key = "dlq.audit"
            )
    )
    public void handleDlqMessages(@Payload Object failedMessage,
                                  @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String originalRoutingKey) {
        log.error("Received message in DLQ. Original routing key: {}, Message: {}",
                originalRoutingKey, failedMessage);
    }
}
