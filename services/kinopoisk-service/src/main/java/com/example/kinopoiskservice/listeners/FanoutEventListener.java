package com.example.kinopoiskservice.listeners;

import com.example.kinopoiskeventscontract.events.ContentRatedEvent;
import com.example.kinopoiskeventscontract.events.ReviewCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

@Component
public class FanoutEventListener {

    private static final Logger log = LoggerFactory.getLogger(FanoutEventListener.class);

    // Подписываемся на Fanout Exchange для оценок контента
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "kinopoisk-content-rated-queue", durable = "true",
                            arguments = {
                                    @Argument(name = "x-dead-letter-exchange", value = "dlx-exchange"),
                                    @Argument(name = "x-dead-letter-routing-key", value = "dlq.kinopoisk")
                            }
                    ),
                    exchange = @Exchange(name = "content-rated-fanout", type = "fanout")
            )
    )
    public void handleContentRated(ContentRatedEvent event) {
        log.info("FANOUT [KINOPOISK]: Пользователь {} оценил контент {} на {} звезд. " +
                        "Средний рейтинг теперь: {} (на основе {} оценок). ",
                event.userId(), event.contentId(), event.rating(),
                String.format("%.2f", event.averageRating()), event.totalRatings());

        // В реальном проекте здесь можно:
        // 1. Обновить кэш популярного контента
        // 2. Пересчитать рекомендации для пользователя
    }

    // Подписываемся на Fanout Exchange для рецензий
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "kinopoisk-review-created-queue", durable = "true",
                            arguments = {
                                    @Argument(name = "x-dead-letter-exchange", value = "dlx-exchange"),
                                    @Argument(name = "x-dead-letter-routing-key", value = "dlq.kinopoisk")
                            }
                    ),
                    exchange = @Exchange(name = "review-created-fanout", type = "fanout")
            )
    )
    public void handleReviewCreated(ReviewCreatedEvent event) {
        String action = event.isNewReview() ? "написана" : "обновлена";
        log.info("FANOUT [KINOPOISK]: {} рецензия на контент {} от пользователя {}. " +
                        "Заголовок: \"{}\", оценка: {} звезд (ID рецензии: {}). ",
                action, event.contentId(), event.userId(),
                event.title(), event.rating(), event.reviewId());

        // В реальном проекте здесь можно:
        // 1. Обновить список последних рецензий в кэше
        // 2. Проверить на спам/токсичность рецензию (через отдельный сервис)
    }
}
