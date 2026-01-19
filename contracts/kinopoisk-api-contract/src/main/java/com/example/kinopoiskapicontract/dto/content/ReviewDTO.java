package com.example.kinopoiskapicontract.dto.content;

public record ReviewDTO(
        Long userId,
        String userName,
        String title,
        String text,
        Integer rating
) {}
