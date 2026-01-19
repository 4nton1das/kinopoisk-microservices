package com.example.kinopoiskeventscontract.events;

import java.io.Serializable;

public record ReviewCreatedEvent(
        Long contentId,
        Long userId,
        String title,
        Integer rating,
        boolean isNewReview,
        Long reviewId
) implements Serializable {}
