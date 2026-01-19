package com.example.kinopoiskeventscontract.events;

import java.io.Serializable;

public record PersonCreatedEvent(
        Long personId,
        String primaryName
) implements Serializable {}
