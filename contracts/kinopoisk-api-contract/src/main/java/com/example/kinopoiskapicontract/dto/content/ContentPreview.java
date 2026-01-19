package com.example.kinopoiskapicontract.dto.content;

// ContentPreviewResponse.java - упрощенная версия для списков

import com.example.kinopoiskapicontract.dto.enums.ContentType;

import java.time.LocalDate;

public record ContentPreview(
        Long id,
        String title,
        ContentType contentType,
        LocalDate releaseDate
) {}
