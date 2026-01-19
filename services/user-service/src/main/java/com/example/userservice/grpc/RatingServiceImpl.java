package com.example.userservice.grpc;

import com.example.kinopoiskeventscontract.events.ContentRatedEvent;
import com.example.kinopoiskeventscontract.events.ReviewCreatedEvent;
import com.example.userservice.config.RabbitMQConfig;
import com.example.userservice.storage.InMemoryRatingStorage;
import com.kinopoisk.grpc.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import jakarta.annotation.PostConstruct;
import java.util.List;

@GrpcService
public class RatingServiceImpl extends UserRatingServiceGrpc.UserRatingServiceImplBase {

    private final InMemoryRatingStorage storage;
    private final RabbitTemplate rabbitTemplate;

    public RatingServiceImpl(InMemoryRatingStorage storage, RabbitTemplate rabbitTemplate) {
        this.storage = storage;
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostConstruct
    public void init() {
        storage.initSampleData();
    }

    // ===== Методы для оценок =====

    @Override
    public void rateContent(RateContentRequest request, StreamObserver<RatingResponse> responseObserver) {
        try {
            System.out.println("DEBUG: Starting rateContent for user " + request.getUserId() +
                    ", content " + request.getContentId() +
                    ", rating " + request.getRating());

            // 1. ПРОВЕРКА существования контента
            if (!storage.contentExists(request.getContentId())) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Контент с ID " + request.getContentId() + " не найден")
                        .asRuntimeException());
                return;
            }

            // 2. Сохраняем оценку
            storage.addRating(request.getUserId(), request.getContentId(), request.getRating());

            // 3. Рассчитываем новую статистику
            double average = storage.getAverageRating(request.getContentId());
            int total = storage.getTotalRatings(request.getContentId());

            System.out.println("DEBUG: Rating saved. Average: " + average + ", Total: " + total);

            // 4. Публикуем событие в Fanout Exchange
            ContentRatedEvent event = new ContentRatedEvent(
                    request.getContentId(),
                    request.getUserId(),
                    request.getRating(),
                    average,
                    total
            );

            System.out.println("DEBUG: Publishing ContentRatedEvent to fanout: " + event);
            rabbitTemplate.convertAndSend(RabbitMQConfig.CONTENT_RATED_FANOUT, "", event);
            System.out.println("DEBUG: Event published successfully");

            // 5. Отправляем ответ клиенту
            RatingResponse response = RatingResponse.newBuilder()
                    .setRatingId(0)
                    .setUserId(request.getUserId())
                    .setContentId(request.getContentId())
                    .setRating(request.getRating())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            System.out.println("DEBUG: Response sent to client");

        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Ошибка при сохранении оценки: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void deleteRating(DeleteRatingRequest request, StreamObserver<DeleteRatingResponse> responseObserver) {
        try {
            boolean deleted = storage.deleteRating(request.getUserId(), request.getContentId());

            DeleteRatingResponse response = DeleteRatingResponse.newBuilder()
                    .setSuccess(deleted)
                    .setMessage(deleted ? "Оценка удалена" : "Оценка не найдена")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Ошибка при удалении оценки: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getUserRating(GetUserRatingRequest request, StreamObserver<RatingResponse> responseObserver) {
        try {
            Integer rating = storage.getUserRating(request.getUserId(), request.getContentId());

            if (rating == null) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Оценка не найдена")
                        .asRuntimeException());
                return;
            }

            RatingResponse response = RatingResponse.newBuilder()
                    .setRatingId(0)
                    .setUserId(request.getUserId())
                    .setContentId(request.getContentId())
                    .setRating(rating)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Ошибка при получении оценки: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getAverageRating(GetAverageRatingRequest request,
                                 StreamObserver<AverageRatingResponse> responseObserver) {
        try {
            if (!storage.contentExists(request.getContentId())) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Контент с ID " + request.getContentId() + " не найден")
                        .asRuntimeException());
                return;
            }

            double average = storage.getAverageRating(request.getContentId());
            int total = storage.getTotalRatings(request.getContentId());

            AverageRatingResponse response = AverageRatingResponse.newBuilder()
                    .setContentId(request.getContentId())
                    .setAverageRating(average)
                    .setTotalRatings(total)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Ошибка при расчете среднего рейтинга: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getContentRatings(GetContentRatingsRequest request,
                                  StreamObserver<ContentRatingsResponse> responseObserver) {
        try {
            List<InMemoryRatingStorage.RatingInfo> ratings = storage.getContentRatings(
                    request.getContentId(), request.getPage(), request.getSize());

            ContentRatingsResponse.Builder builder = ContentRatingsResponse.newBuilder();

            for (InMemoryRatingStorage.RatingInfo rating : ratings) {
                builder.addRatings(RatingResponse.newBuilder()
                        .setRatingId(0)
                        .setUserId(rating.userId())
                        .setContentId(request.getContentId())
                        .setRating(rating.rating())
                        .build());
            }

            builder.setTotal(storage.getTotalRatings(request.getContentId()))
                    .setPage(request.getPage())
                    .setSize(request.getSize());

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Ошибка при получении оценок: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    // ===== Методы для рецензий =====

    @Override
    public void addReview(AddReviewRequest request, StreamObserver<ReviewResponse> responseObserver) {
        try {
            // 1. ПРОВЕРКА существования контента
            if (!storage.contentExists(request.getContentId())) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Контент с ID " + request.getContentId() + " не найден")
                        .asRuntimeException());
                return;
            }

            // 2. Проверяем существование рецензии
            boolean hasExistingReview = storage.hasUserReview(request.getUserId(), request.getContentId());

            if (request.getRating() < 1 || request.getRating() > 10) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("Оценка обязательна для рецензии и должна быть в диапазоне от 1 до 10. Получено: " + request.getRating())
                        .asRuntimeException());
                return;
            }

            // 3. Добавляем или обновляем рецензию
            InMemoryRatingStorage.Review review = storage.addOrUpdateReview(
                    request.getUserId(),
                    request.getContentId(),
                    request.getTitle(),
                    request.getText(),
                    request.getRating()
            );

            System.out.println("DEBUG: Review saved/updated. ID: " + review.id() +
                    ", isNew: " + !hasExistingReview);

            // 4. Публикуем событие в Fanout Exchange
            ReviewCreatedEvent event = new ReviewCreatedEvent(
                    request.getContentId(),
                    request.getUserId(),
                    request.getTitle(),
                    request.getRating(),
                    !hasExistingReview, // isNewReview
                    review.id()
            );

            System.out.println("DEBUG: Publishing ReviewCreatedEvent to fanout: " + event);
            rabbitTemplate.convertAndSend(RabbitMQConfig.REVIEW_CREATED_FANOUT, "", event);
            System.out.println("DEBUG: Event published successfully");

            // 5. Отправляем ответ клиенту
            ReviewResponse response = ReviewResponse.newBuilder()
                    .setReviewId(review.id())
                    .setUserId(review.userId())
                    .setContentId(review.contentId())
                    .setTitle(review.title())
                    .setText(review.text())
                    .setRating(review.rating())
                    .setCreatedAt(review.createdAt())
                    .setUpdatedAt(review.updatedAt())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            System.out.println("DEBUG: Response sent to client");

        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Ошибка при добавлении/обновлении рецензии: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void deleteReview(DeleteReviewRequest request, StreamObserver<DeleteReviewResponse> responseObserver) {
        try {
            boolean deleted = storage.deleteReview(request.getUserId(), request.getContentId());

            DeleteReviewResponse response = DeleteReviewResponse.newBuilder()
                    .setSuccess(deleted)
                    .setMessage(deleted ? "Рецензия удалена" : "Рецензия не найдена")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Ошибка при удалении рецензии: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getUserReview(GetUserReviewRequest request, StreamObserver<ReviewResponse> responseObserver) {
        try {
            InMemoryRatingStorage.Review review = storage.getUserReview(
                    request.getUserId(), request.getContentId());

            if (review == null) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Рецензия не найдена")
                        .asRuntimeException());
                return;
            }

            ReviewResponse response = ReviewResponse.newBuilder()
                    .setReviewId(review.id())
                    .setUserId(review.userId())
                    .setContentId(review.contentId())
                    .setTitle(review.title())
                    .setText(review.text())
                    .setRating(review.rating())
                    .setCreatedAt(review.createdAt())
                    .setUpdatedAt(review.updatedAt())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Ошибка при получении рецензии: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getContentReviews(GetContentReviewsRequest request,
                                  StreamObserver<ContentReviewsResponse> responseObserver) {
        try {
            List<InMemoryRatingStorage.Review> reviews = storage.getContentReviews(
                    request.getContentId(), request.getPage(), request.getSize());

            ContentReviewsResponse.Builder builder = ContentReviewsResponse.newBuilder();

            for (InMemoryRatingStorage.Review review : reviews) {
                builder.addReviews(ReviewResponse.newBuilder()
                        .setReviewId(review.id())
                        .setUserId(review.userId())
                        .setContentId(review.contentId())
                        .setTitle(review.title())
                        .setText(review.text())
                        .setRating(review.rating())
                        .setCreatedAt(review.createdAt())
                        .setUpdatedAt(review.updatedAt())
                        .build());
            }

            builder.setTotal(storage.getTotalReviews(request.getContentId()))
                    .setPage(request.getPage())
                    .setSize(request.getSize());

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Ошибка при получении рецензий: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    // ===== Методы для избранного =====

    @Override
    public void addToFavorites(AddToFavoritesRequest request,
                               StreamObserver<FavoriteResponse> responseObserver) {
        try {
            // 1. ПРОВЕРКА существования контента
            if (!storage.contentExists(request.getContentId())) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Контент с ID " + request.getContentId() + " не найден")
                        .asRuntimeException());
                return;
            }

            // 2. Добавляем в избранное
            boolean added = storage.addToFavorites(request.getUserId(), request.getContentId());

            if (!added) {
                responseObserver.onError(Status.ALREADY_EXISTS
                        .withDescription("Контент уже в избранном")
                        .asRuntimeException());
                return;
            }

            FavoriteResponse response = FavoriteResponse.newBuilder()
                    .setFavoriteId(0)
                    .setUserId(request.getUserId())
                    .setContentId(request.getContentId())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Ошибка при добавлении в избранное: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void removeFromFavorites(RemoveFromFavoritesRequest request,
                                    StreamObserver<FavoriteResponse> responseObserver) {
        try {
            boolean removed = storage.removeFromFavorites(request.getUserId(), request.getContentId());

            if (!removed) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Контент не найден в избранном")
                        .asRuntimeException());
                return;
            }

            FavoriteResponse response = FavoriteResponse.newBuilder()
                    .setFavoriteId(0)
                    .setUserId(request.getUserId())
                    .setContentId(request.getContentId())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Ошибка при удалении из избранного: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void isInFavorites(IsInFavoritesRequest request,
                              StreamObserver<IsInFavoritesResponse> responseObserver) {
        try {
            boolean isInFavorites = storage.isInFavorites(request.getUserId(), request.getContentId());

            IsInFavoritesResponse response = IsInFavoritesResponse.newBuilder()
                    .setIsInFavorites(isInFavorites)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Ошибка при проверке избранного: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getUserFavorites(GetUserFavoritesRequest request,
                                 StreamObserver<UserFavoritesResponse> responseObserver) {
        try {
            List<InMemoryRatingStorage.FavoriteInfo> favorites = storage.getUserFavoritesPage(
                    request.getUserId(), request.getPage(), request.getSize());

            UserFavoritesResponse.Builder builder = UserFavoritesResponse.newBuilder();

            for (InMemoryRatingStorage.FavoriteInfo favorite : favorites) {
                builder.addFavorites(FavoriteResponse.newBuilder()
                        .setFavoriteId(0)
                        .setUserId(request.getUserId())
                        .setContentId(favorite.contentId())
                        .build());
            }

            builder.setTotal(storage.getUserFavorites(request.getUserId()).size())
                    .setPage(request.getPage())
                    .setSize(request.getSize());

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Ошибка при получении избранного: " + e.getMessage())
                    .asRuntimeException());
        }
    }
}