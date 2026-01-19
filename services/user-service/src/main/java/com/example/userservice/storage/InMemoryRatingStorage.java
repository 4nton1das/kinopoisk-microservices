package com.example.userservice.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryRatingStorage {

    private static final Logger log = LoggerFactory.getLogger(InMemoryRatingStorage.class);

    // === Хранилище оценок ===
    // contentId -> Map<userId, rating>
    private final Map<Long, Map<Long, Integer>> contentRatings = new ConcurrentHashMap<>();
    // userId -> Set<contentId> (для быстрого поиска оценок пользователя)
    private final Map<Long, Set<Long>> userRatings = new ConcurrentHashMap<>();

    // === Хранилище рецензий ===
    private final Map<String, Review> reviews = new ConcurrentHashMap<>();
    private final Map<Long, List<Review>> reviewsByContent = new ConcurrentHashMap<>();

    // === Хранилище избранного ===
    // userId -> Set<contentId>
    private final Map<Long, Set<Long>> userFavorites = new ConcurrentHashMap<>();

    // === Кэш существующих контентов ===
    private final Set<Long> existingContentIds = ConcurrentHashMap.newKeySet();

    // ID генераторы
    private final AtomicLong reviewIdSequence = new AtomicLong(1);

    // Модели
    public record Review(Long id, Long userId, Long contentId, String title,
                         String text, Integer rating, Long createdAt, Long updatedAt) {}

    // ===== Методы для управления кэшем контентов =====

    public void addContentId(Long contentId) {
        existingContentIds.add(contentId);
        log.info("Content {} added to validation cache. Total: {}",
                contentId, existingContentIds.size());
    }

    public boolean contentExists(Long contentId) {
        return existingContentIds.contains(contentId);
    }

    public Set<Long> getExistingContentIds() {
        return Set.copyOf(existingContentIds);
    }

    public void removeContentAndData(Long contentId) {
        // 1. Удаляем из кэша
        existingContentIds.remove(contentId);
        log.info("Content {} removed from validation cache", contentId);

        // 2. Удаляем оценки
        Map<Long, Integer> ratings = contentRatings.remove(contentId);
        int deletedRatings = (ratings != null) ? ratings.size() : 0;

        if (deletedRatings > 0) {
            ratings.keySet().forEach(userId -> {
                Set<Long> userContentIds = userRatings.get(userId);
                if (userContentIds != null) {
                    userContentIds.remove(contentId);
                }
            });
            log.info("Deleted {} ratings for content {}", deletedRatings, contentId);
        }

        // 3. Удаляем рецензии
        List<Review> contentReviews = reviewsByContent.remove(contentId);
        int deletedReviews = (contentReviews != null) ? contentReviews.size() : 0;

        if (deletedReviews > 0) {
            contentReviews.forEach(review -> {
                reviews.remove(review.userId() + ":" + review.contentId());
            });
            log.info("Deleted {} reviews for content {}", deletedReviews, contentId);
        }

        // 4. Удаляем из избранного
        int removedFromFavorites = 0;
        for (Set<Long> favorites : userFavorites.values()) {
            if (favorites.remove(contentId)) {
                removedFromFavorites++;
            }
        }

        if (removedFromFavorites > 0) {
            log.info("Removed content {} from {} users' favorites",
                    contentId, removedFromFavorites);
        }

        log.info("Total data deleted for content {}: {} ratings, {} reviews, {} favorites",
                contentId, deletedRatings, deletedReviews, removedFromFavorites);
    }

    // ===== Методы для оценок =====

    public void addRating(Long userId, Long contentId, Integer rating) {
        // Проверяем существование контента
        if (!contentExists(contentId)) {
            throw new IllegalArgumentException("Content " + contentId + " does not exist");
        }

        contentRatings.computeIfAbsent(contentId, k -> new ConcurrentHashMap<>())
                .put(userId, rating);

        // Добавляем в userRatings
        userRatings.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                .add(contentId);

        String key = userId + ":" + contentId;
        Review existingReview = reviews.get(key);

        if (existingReview != null) {
            Review updatedReview = new Review(
                    existingReview.id(),
                    existingReview.userId(),
                    existingReview.contentId(),
                    existingReview.title(),
                    existingReview.text(),
                    rating,
                    existingReview.createdAt(),
                    System.currentTimeMillis() / 1000
            );

            reviews.put(key, updatedReview);

            removeReviewFromContentIndex(existingReview);
            addReviewToContentIndex(updatedReview);
        }
    }

    public boolean deleteRating(Long userId, Long contentId) {
        // Проверяем существование контента
        if (!contentExists(contentId)) {
            throw new IllegalArgumentException("Content " + contentId + " does not exist");
        }

        // Удаляем оценку
        Map<Long, Integer> ratings = contentRatings.get(contentId);
        if (ratings != null) {
            Integer removed = ratings.remove(userId);
            if (removed != null) {
                // Также удаляем из userRatings
                Set<Long> userContentIds = userRatings.get(userId);
                if (userContentIds != null) {
                    userContentIds.remove(contentId);
                }
                return true;
            }
        }
        return false;
    }

    public Integer getUserRating(Long userId, Long contentId) {
        Map<Long, Integer> ratings = contentRatings.get(contentId);
        return ratings != null ? ratings.get(userId) : null;
    }

    // Получить средний рейтинг контента
    public double getAverageRating(Long contentId) {
        Map<Long, Integer> ratings = contentRatings.get(contentId);
        if (ratings == null || ratings.isEmpty()) {
            return 0.0;
        }
        return ratings.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
    }

    // Получить количество оценок для контента
    public int getTotalRatings(Long contentId) {
        Map<Long, Integer> ratings = contentRatings.get(contentId);
        return ratings != null ? ratings.size() : 0;
    }

    public List<RatingInfo> getContentRatings(Long contentId, int page, int size) {
        Map<Long, Integer> ratingsMap = contentRatings.get(contentId);
        if (ratingsMap == null) {
            return Collections.emptyList();
        }

        List<RatingInfo> allRatings = ratingsMap.entrySet().stream()
                .map(entry -> new RatingInfo(entry.getKey(), entry.getValue()))
                .toList();

        // Пагинация
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, allRatings.size());

        return fromIndex < allRatings.size()
                ? allRatings.subList(fromIndex, toIndex)
                : Collections.emptyList();
    }

    public record RatingInfo(Long userId, Integer rating) {}

    // ===== Методы для рецензий =====

    public Review addOrUpdateReview(Long userId, Long contentId, String title,
                                    String text, Integer rating) {
        // Проверяем существование контента
        if (!contentExists(contentId)) {
            throw new IllegalArgumentException("Content " + contentId + " does not exist");
        }

        String key = userId + ":" + contentId;
        Review existingReview = reviews.get(key);
        long now = System.currentTimeMillis() / 1000;

        if (rating != null) {
            addRating(userId, contentId, rating);
        }

        Review review;
        if (existingReview != null) {
            // Обновляем существующую рецензию
            review = new Review(
                    existingReview.id(),
                    userId,
                    contentId,
                    title,
                    text,
                    rating,
                    existingReview.createdAt(),
                    now
            );

            // Удаляем старую из reviewsByContent
            removeReviewFromContentIndex(existingReview);
        } else {
            // Создаем новую рецензию
            Long id = reviewIdSequence.getAndIncrement();
            review = new Review(id, userId, contentId, title, text, rating, now, now);
        }

        // Сохраняем/обновляем
        reviews.put(key, review);
        addReviewToContentIndex(review);

        return review;
    }

    public Review getUserReview(Long userId, Long contentId) {
        return reviews.get(userId + ":" + contentId);
    }

    public boolean deleteReview(Long userId, Long contentId) {
        String key = userId + ":" + contentId;
        Review review = reviews.remove(key);
        if (review != null) {
            removeReviewFromContentIndex(review);
            return true;
        }
        return false;
    }

    public boolean hasUserReview(Long userId, Long contentId) {
        return reviews.containsKey(userId + ":" + contentId);
    }

    private void addReviewToContentIndex(Review review) {
        reviewsByContent.computeIfAbsent(review.contentId(), k -> new ArrayList<>())
                .add(review);
    }

    private void removeReviewFromContentIndex(Review review) {
        List<Review> contentReviews = reviewsByContent.get(review.contentId());
        if (contentReviews != null) {
            contentReviews.removeIf(r -> r.id().equals(review.id()));
            if (contentReviews.isEmpty()) {
                reviewsByContent.remove(review.contentId());
            }
        }
    }

    public List<Review> getContentReviews(Long contentId, int page, int size) {
        List<Review> reviews = reviewsByContent.get(contentId);
        if (reviews == null) {
            return Collections.emptyList();
        }

        // Пагинация
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, reviews.size());

        return fromIndex < reviews.size()
                ? reviews.subList(fromIndex, toIndex)
                : Collections.emptyList();
    }

    public int getTotalReviews(Long contentId) {
        List<Review> reviews = reviewsByContent.get(contentId);
        return reviews != null ? reviews.size() : 0;
    }

    // ===== Методы для избранного =====

    public boolean addToFavorites(Long userId, Long contentId) {
        // Проверяем существование контента
        if (!contentExists(contentId)) {
            throw new IllegalArgumentException("Content " + contentId + " does not exist");
        }

        return userFavorites.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                .add(contentId);
    }

    public boolean removeFromFavorites(Long userId, Long contentId) {
        Set<Long> favorites = userFavorites.get(userId);
        return favorites != null && favorites.remove(contentId);
    }

    public boolean isInFavorites(Long userId, Long contentId) {
        Set<Long> favorites = userFavorites.get(userId);
        return favorites != null && favorites.contains(contentId);
    }

    public Set<Long> getUserFavorites(Long userId) {
        return userFavorites.getOrDefault(userId, Collections.emptySet());
    }

    public List<FavoriteInfo> getUserFavoritesPage(Long userId, int page, int size) {
        Set<Long> favorites = userFavorites.get(userId);
        if (favorites == null) {
            return Collections.emptyList();
        }

        List<Long> favoritesList = new ArrayList<>(favorites);

        // Пагинация
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, favoritesList.size());

        if (fromIndex >= favoritesList.size()) {
            return Collections.emptyList();
        }

        return favoritesList.subList(fromIndex, toIndex).stream()
                .map(contentId -> new FavoriteInfo(contentId, null))
                .toList();
    }

    public record FavoriteInfo(Long contentId, Long addedAt) {}

    // ===== Инициализация =====

    public void initSampleData() {
        // Инициализируем контенты 1, 2, 3 (которые есть в kinopoisk-service)
        addContentId(1L);
        addContentId(2L);
        addContentId(3L);

        // Тестовые данные
        addRating(1L, 1L, 8);
        addRating(1L, 2L, 9);
        addRating(2L, 1L, 7);

        addOrUpdateReview(1L, 1L, "Отличный фильм!",
                "Очень понравилось, рекомендую всем посмотреть.", 8);
        addOrUpdateReview(2L, 1L, "Неплохо, но могло быть лучше",
                "Хорошая картина, но есть недочеты.", 7);

        addToFavorites(1L, 1L);
        addToFavorites(1L, 3L);
        addToFavorites(2L, 1L);

        log.info("Initialized with {} existing content ids", existingContentIds.size());
    }
}