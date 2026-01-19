package com.example.kinopoiskservice.controllers;

import com.kinopoisk.grpc.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {

    @GrpcClient("user-service")
    private UserRatingServiceGrpc.UserRatingServiceBlockingStub ratingStub;

    @PostMapping("/content/{contentId}/rate")
    public ResponseEntity<String> rateContent(
            @PathVariable Long contentId,
            @RequestParam Long userId,
            @RequestParam Integer rating) {

        if (rating < 1 || rating > 10) {
            return ResponseEntity.badRequest()
                    .body("Рейтинг должен быть от 1 до 10");
        }

        RateContentRequest request = RateContentRequest.newBuilder()
                .setUserId(userId)
                .setContentId(contentId)
                .setRating(rating)
                .build();

        try {
            RatingResponse response = ratingStub.rateContent(request);
            return ResponseEntity.ok(
                    String.format("Контент %d оценен пользователем %d на %d звезд. ID оценки: %d",
                            contentId, userId, rating, response.getRatingId())
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Ошибка при сохранении оценки: " + e.getMessage());
        }
    }

    @GetMapping("/content/{contentId}/average")
    public ResponseEntity<String> getAverageRating(@PathVariable Long contentId) {
        GetAverageRatingRequest request = GetAverageRatingRequest.newBuilder()
                .setContentId(contentId)
                .build();

        try {
            AverageRatingResponse response = ratingStub.getAverageRating(request);
            return ResponseEntity.ok(
                    String.format("Средний рейтинг контента %d: %.2f/10 (на основе %d оценок)",
                            contentId, response.getAverageRating(), response.getTotalRatings())
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Ошибка при получении рейтинга: " + e.getMessage());
        }
    }

    @DeleteMapping("/content/{contentId}/rate")
    public ResponseEntity<String> deleteRating(
            @PathVariable Long contentId,
            @RequestParam Long userId) {

        DeleteRatingRequest request = DeleteRatingRequest.newBuilder()
                .setUserId(userId)
                .setContentId(contentId)
                .build();

        try {
            DeleteRatingResponse response = ratingStub.deleteRating(request);
            if (response.getSuccess()) {
                return ResponseEntity.ok("Оценка удалена");
            } else {
                return ResponseEntity.status(404).body("Оценка не найдена");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Ошибка при удалении оценки: " + e.getMessage());
        }
    }

    @GetMapping("/test")
    public ResponseEntity<String> testConnection() {
        try {
            // Простой тестовый запрос
            GetAverageRatingRequest request = GetAverageRatingRequest.newBuilder()
                    .setContentId(999L)  // несуществующий контент
                    .build();

            AverageRatingResponse response = ratingStub.getAverageRating(request);
            return ResponseEntity.ok("gRPC соединение работает! User-service отвечает.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Ошибка gRPC соединения: " + e.getMessage());
        }
    }
}