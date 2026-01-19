package com.example.kinopoiskapicontract.dto.content;

// ContentRequest.java - для создания/обновления контента

import com.example.kinopoiskapicontract.dto.enums.ContentType;
import com.example.kinopoiskapicontract.dto.enums.Genre;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Set;

public record ContentRequest(
        @NotBlank(message = "Название не может быть пустым")
        String title,
        String description,
        @NotNull(message = "Тип контента обязателен")
        ContentType contentType,
        LocalDate releaseDate,
        Set<Genre> genres
) {}
