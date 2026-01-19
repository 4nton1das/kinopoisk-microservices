package com.example.auditservice.listeners;

import com.example.kinopoiskeventscontract.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
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
                    value = @Queue(
                        name = "audit-content-created-queue",
                        durable = "true",
                        arguments = {
                                @Argument(name = "x-dead-letter-exchange", value = "dlx-exchange"),
                                @Argument(name = "x-dead-letter-routing-key", value = "dlq.audit")
                        }
                    ),
                    exchange = @Exchange(name = "kinopoisk-exchange", type = "topic"),
                    key = "content.created"
            ),
            ackMode = "MANUAL"  // Явно указываем ручной режим
    )
    public void handleContentCreated(@Payload ContentCreatedEvent event, 
                                     Channel channel,
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
                throw new AmqpRejectAndDontRequeueException("Failed to process message", e);
            }
        }
    }

    // 2. Удаление контента
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                        name = "audit-content-deleted-queue",
                        durable = "true",
                        arguments = {
                                @Argument(name = "x-dead-letter-exchange", value = "dlx-exchange"),
                                @Argument(name = "x-dead-letter-routing-key", value = "dlq.audit")
                        }
                    ),
                    exchange = @Exchange(name = "kinopoisk-exchange", type = "topic"),
                    key = "content.deleted"
            ),
            ackMode = "MANUAL"
    )
    public void handleContentDeleted(@Payload ContentDeletedEvent event,
                                     Channel channel,
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
                throw new AmqpRejectAndDontRequeueException("Failed to process message", e);
            }
        }
    }

    // 3. Создание персоны
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                        name = "audit-person-created-queue",
                        durable = "true",
                        arguments = {
                                @Argument(name = "x-dead-letter-exchange", value = "dlx-exchange"),
                                @Argument(name = "x-dead-letter-routing-key", value = "dlq.audit")
                        }
                    ),
                    exchange = @Exchange(name = "kinopoisk-exchange", type = "topic"),
                    key = "person.created"
            ),
            ackMode = "MANUAL"
    )
    public void handlePersonCreated(@Payload PersonCreatedEvent event, 
                                    Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.info("Person created event received: ID={}, Name='{}'",
                    event.personId(), event.primaryName());
            log.info("AUDIT: New person created - {}", event.primaryName());
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("Audit processing failed for event: {}. Sending to DLQ.", event, e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("Failed to nack message", ioException);
                throw new AmqpRejectAndDontRequeueException("Failed to process message", e);
            }
        }
    }

    // 4. Удаление персоны
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                        name = "audit-person-deleted-queue",
                        durable = "true",
                        arguments = {
                                @Argument(name = "x-dead-letter-exchange", value = "dlx-exchange"),
                                @Argument(name = "x-dead-letter-routing-key", value = "dlq.audit")
                        }
                    ),
                    exchange = @Exchange(name = "kinopoisk-exchange", type = "topic"),
                    key = "person.deleted"
            ),
            ackMode = "MANUAL"
    )
    public void handlePersonDeleted(@Payload PersonDeletedEvent event,
                                    Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.info("Person deleted event received: ID={}", event.personId());
            log.info("AUDIT: Person deleted - ID: {}", event.personId());
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("Audit processing failed for event: {}. Sending to DLQ.", event, e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("Failed to nack message", ioException);
                throw new AmqpRejectAndDontRequeueException("Failed to process message", e);
            }
        }
    }

    // ========== FANOUT EXCHANGE ОБРАБОТЧИКИ (из user-service) ==========

    // 5. Оценка контента (Fanout Exchange)
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "audit-content-rated-queue", durable = "true"),
                    exchange = @Exchange(name = "content-rated-fanout", type = "fanout")
            ),
            ackMode = "MANUAL"  // Тоже ручной режим
    )
    public void handleContentRated(@Payload ContentRatedEvent event,
                                   Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.info("FANOUT AUDIT: Пользователь {} оценил контент {} на {} звезд. " +
                            "Средний рейтинг теперь: {} (на основе {} оценок)",
                    event.userId(), event.contentId(), event.rating(),
                    String.format("%.2f", event.averageRating()), event.totalRatings());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to process rating event", e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                throw new AmqpRejectAndDontRequeueException("Failed to nack message", e);
            }
        }
    }

    // 6. Создание/обновление рецензии (Fanout Exchange)
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "audit-review-created-queue", durable = "true"),
                    exchange = @Exchange(name = "review-created-fanout", type = "fanout")
            ),
            ackMode = "MANUAL"
    )
    public void handleReviewCreated(@Payload ReviewCreatedEvent event,
                                    Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            String action = event.isNewReview() ? "написал" : "обновил";
            log.info("FANOUT AUDIT: Пользователь {} {} рецензию на контент {} (ID рецензии: {}): \"{}\" (оценка: {} звезд)",
                    event.userId(), action, event.contentId(), event.reviewId(), event.title(), event.rating());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to process review event", e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                throw new AmqpRejectAndDontRequeueException("Failed to nack message", e);
            }
        }
    }

    // Обработчик для DLQ (для отладки)
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "audit-content-created-queue.dlq", durable = "true"),
                    exchange = @Exchange(name = "dlx-exchange", type = "topic"),
                    key = "dlq.audit"
            ),
            ackMode = "AUTO"  // DLQ можно в авто режиме
    )
    public void handleDlqMessages(@Payload Object failedMessage,
                                  @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String originalRoutingKey) {
        log.error("Received message in DLQ. Original routing key: {}, Message: {}",
                originalRoutingKey, failedMessage);
    }
}
