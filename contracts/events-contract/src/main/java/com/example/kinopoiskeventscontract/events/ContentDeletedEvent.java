package com.example.kinopoiskeventscontract.events;

import java.io.Serializable;

public record ContentDeletedEvent(
        Long contentId
) implements Serializable {}
