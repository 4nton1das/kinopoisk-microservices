package com.example.kinopoiskeventscontract.events;

import java.io.Serializable;

public record ContentRatedEvent(
        Long contentId,
        Long userId,
        Integer rating,
        Double averageRating,
        Integer totalRatings
) implements Serializable {}
