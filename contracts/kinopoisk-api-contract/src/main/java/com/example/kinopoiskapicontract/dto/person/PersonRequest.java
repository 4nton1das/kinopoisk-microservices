package com.example.kinopoiskapicontract.dto.person;

// PersonRequest.java - для создания/обновления персоны

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record PersonRequest(
        @NotBlank(message = "Имя не может быть пустым")
        String primaryName,
        LocalDate birthDate,
        String birthPlace
) {}
