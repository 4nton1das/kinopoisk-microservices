package com.example.kinopoiskservice.controllers;

import com.kinopoisk.grpc.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    @GrpcClient("user-service")
    private UserRatingServiceGrpc.UserRatingServiceBlockingStub ratingStub;

    @PostMapping("/content/{contentId}")
    public ResponseEntity<String> addToFavorites(
            @PathVariable Long contentId,
            @RequestParam Long userId) {

        AddToFavoritesRequest request = AddToFavoritesRequest.newBuilder()
                .setUserId(userId)
                .setContentId(contentId)
                .build();

        try {
            FavoriteResponse response = ratingStub.addToFavorites(request);
            return ResponseEntity.ok(
                    String.format("Контент %d добавлен в избранное пользователя %d",
                            contentId, userId)
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Ошибка при добавлении в избранное: " + e.getMessage());
        }
    }

    @DeleteMapping("/content/{contentId}")
    public ResponseEntity<String> removeFromFavorites(
            @PathVariable Long contentId,
            @RequestParam Long userId) {

        RemoveFromFavoritesRequest request = RemoveFromFavoritesRequest.newBuilder()
                .setUserId(userId)
                .setContentId(contentId)
                .build();

        try {
            FavoriteResponse response = ratingStub.removeFromFavorites(request);
            return ResponseEntity.ok(
                    String.format("Контент %d удален из избранного пользователя %d",
                            contentId, userId)
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Ошибка при удалении из избранного: " + e.getMessage());
        }
    }

    @GetMapping("/check")
    public ResponseEntity<String> checkInFavorites(
            @RequestParam Long userId,
            @RequestParam Long contentId) {

        IsInFavoritesRequest request = IsInFavoritesRequest.newBuilder()
                .setUserId(userId)
                .setContentId(contentId)
                .build();

        try {
            IsInFavoritesResponse response = ratingStub.isInFavorites(request);
            return ResponseEntity.ok(
                    response.getIsInFavorites()
                            ? String.format("Контент %d В избранном у пользователя %d", contentId, userId)
                            : String.format("Контент %d НЕ в избранном у пользователя %d", contentId, userId)
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Ошибка при проверке избранного: " + e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<String> getUserFavorites(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        GetUserFavoritesRequest request = GetUserFavoritesRequest.newBuilder()
                .setUserId(userId)
                .setPage(page)
                .setSize(size)
                .build();

        try {
            UserFavoritesResponse response = ratingStub.getUserFavorites(request);
            return ResponseEntity.ok(
                    String.format("Найдено %d избранных контентов у пользователя %d (страница %d)",
                            response.getTotal(), userId, page)
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Ошибка при получении избранного: " + e.getMessage());
        }
    }
}