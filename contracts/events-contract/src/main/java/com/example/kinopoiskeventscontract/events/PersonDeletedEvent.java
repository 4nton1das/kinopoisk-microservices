package com.example.kinopoiskeventscontract.events;

import java.io.Serializable;

public record PersonDeletedEvent(
        Long personId
) implements Serializable {}
