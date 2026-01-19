package com.example.kinopoiskservice.services;

import com.example.kinopoiskapicontract.dto.content.ContentDetailsDTO;
import com.example.kinopoiskapicontract.dto.content.ContentResponse;
import com.example.kinopoiskapicontract.dto.content.ReviewDTO;
import com.kinopoisk.grpc.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ContentDetailsService {

    @GrpcClient("user-service")
    private UserRatingServiceGrpc.UserRatingServiceBlockingStub ratingStub;
    // 1. Добавляем поле для сервиса, который работает с базой контента
    private final ContentService contentService;

    // 2. Внедряем через конструктор
    public ContentDetailsService(ContentService contentService) {
        this.contentService = contentService;
    }

    public ContentDetailsDTO getContentDetails(Long contentId, Long userId) {
        // Получаем базовую информацию о контенте
        ContentResponse content = getContentFromStorage(contentId);
        if (content == null) {
            return null;
        }

        // Получаем информацию о рейтингах
        GetAverageRatingRequest avgRequest = GetAverageRatingRequest.newBuilder()
                .setContentId(contentId)
                .build();
        AverageRatingResponse avgResponse = ratingStub.getAverageRating(avgRequest);

        // Получаем оценку пользователя (если userId указан)
        Integer userRating = null;
        Boolean inFavorites = null;

        if (userId != null) {
            try {
                GetUserRatingRequest userRatingRequest = GetUserRatingRequest.newBuilder()
                        .setUserId(userId)
                        .setContentId(contentId)
                        .build();
                RatingResponse ratingResponse = ratingStub.getUserRating(userRatingRequest);
                userRating = ratingResponse.getRating();
            } catch (Exception e) {
                // Пользователь не оценивал этот контент
            }

            // Проверяем, в избранном ли
            IsInFavoritesRequest favoritesRequest = IsInFavoritesRequest.newBuilder()
                    .setUserId(userId)
                    .setContentId(contentId)
                    .build();
            IsInFavoritesResponse favoritesResponse = ratingStub.isInFavorites(favoritesRequest);
            inFavorites = favoritesResponse.getIsInFavorites();
        }

        // Получаем несколько последних рецензий
        GetContentReviewsRequest reviewsRequest = GetContentReviewsRequest.newBuilder()
                .setContentId(contentId)
                .setPage(0)
                .setSize(3) // Берем 3 последние рецензии
                .build();

        ContentReviewsResponse reviewsResponse = ratingStub.getContentReviews(reviewsRequest);

        List<ReviewDTO> recentReviews = new ArrayList<>();
        for (ReviewResponse grpcReview : reviewsResponse.getReviewsList()) {
            recentReviews.add(new ReviewDTO(
                    grpcReview.getUserId(),
                    "Пользователь " + grpcReview.getUserId(),
                    grpcReview.getTitle(),
                    grpcReview.getText(),
                    grpcReview.getRating()
            ));
        }

        return new ContentDetailsDTO(
                content,
                avgResponse.getAverageRating(),
                avgResponse.getTotalRatings(),
                userRating,
                inFavorites,
                reviewsResponse.getTotal(),
                recentReviews
        );
    }

    private ContentResponse getContentFromStorage(Long contentId) {
        try {
            return contentService.findById(contentId);
        } catch (Exception e) {
            return null;
        }
    }

}
