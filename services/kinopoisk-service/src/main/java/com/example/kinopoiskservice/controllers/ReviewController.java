package com.example.kinopoiskservice.controllers;

import com.kinopoisk.grpc.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @GrpcClient("user-service")
    private UserRatingServiceGrpc.UserRatingServiceBlockingStub ratingStub;

    @PostMapping("/content/{contentId}")
    public ResponseEntity<String> addReview(
            @PathVariable Long contentId,
            @RequestParam Long userId,
            @RequestParam String title,
            @RequestParam String text,
            @RequestParam Integer rating) {

        AddReviewRequest request = AddReviewRequest.newBuilder()
                .setUserId(userId)
                .setContentId(contentId)
                .setTitle(title)
                .setText(text)
                .setRating(rating != null ? rating : 0)
                .build();

        try {
            ReviewResponse response = ratingStub.addReview(request);

            GetUserReviewRequest getRequest = GetUserReviewRequest.newBuilder()
                    .setUserId(userId)
                    .setContentId(contentId)
                    .build();

            var userReview = ratingStub.getUserReview(getRequest);
            boolean isUpdated = userReview.hasReviewId();

            return ResponseEntity.ok(
                    String.format("Рецензия %s. ID: %d",
                            isUpdated ? "обновлена" : "добавлена",
                            response.getReviewId())
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Ошибка при добавлении рецензии: " + e.getMessage());
        }
    }

    @DeleteMapping("/content/{contentId}")
    public ResponseEntity<String> deleteReview(
            @PathVariable Long contentId,
            @RequestParam Long userId) {

        DeleteReviewRequest request = DeleteReviewRequest.newBuilder()
                .setUserId(userId)
                .setContentId(contentId)
                .build();

        try {
            DeleteReviewResponse response = ratingStub.deleteReview(request);
            if (response.getSuccess()) {
                return ResponseEntity.ok("Рецензия удалена");
            } else {
                return ResponseEntity.status(404).body("Рецензия не найдена");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Ошибка при удалении рецензии: " + e.getMessage());
        }
    }

    @GetMapping("/content/{contentId}")
    public ResponseEntity<String> getContentReviews(
            @PathVariable Long contentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        GetContentReviewsRequest request = GetContentReviewsRequest.newBuilder()
                .setContentId(contentId)
                .setPage(page)
                .setSize(size)
                .build();

        try {
            ContentReviewsResponse response = ratingStub.getContentReviews(request);
            return ResponseEntity.ok(
                    String.format("Найдено %d рецензий для контента %d (страница %d)",
                            response.getTotal(), contentId, page)
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Ошибка при получении рецензий: " + e.getMessage());
        }
    }

    @GetMapping("/user/{userId}/content/{contentId}")
    public ResponseEntity<String> getUserReview(
            @PathVariable Long userId,
            @PathVariable Long contentId) {

        GetUserReviewRequest request = GetUserReviewRequest.newBuilder()
                .setUserId(userId)
                .setContentId(contentId)
                .build();

        try {
            ReviewResponse response = ratingStub.getUserReview(request);
            return ResponseEntity.ok(
                    String.format("Рецензия пользователя %d на контент %d: %s - %s",
                            userId, contentId, response.getTitle(), response.getText())
            );
        } catch (Exception e) {
            return ResponseEntity.status(404)
                    .body("Рецензия не найдена");
        }
    }
}
