package com.example.kinopoiskeventscontract.events;

import java.io.Serializable;

public record ContentCreatedEvent(
        Long contentId,
        String title
) implements Serializable {}
