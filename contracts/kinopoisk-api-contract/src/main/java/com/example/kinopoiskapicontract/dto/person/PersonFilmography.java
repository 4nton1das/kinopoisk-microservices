package com.example.kinopoiskapicontract.dto.person;

// PersonFilmographyResponse.java - связь персона-контент

import com.example.kinopoiskapicontract.dto.enums.ContentType;
import com.example.kinopoiskapicontract.dto.enums.Role;
import java.time.LocalDate;

public record PersonFilmography(
        Long contentId,
        String title,
        ContentType contentType,
        LocalDate releaseDate,
        Role role,
        String characterName  // для актеров
) {}
