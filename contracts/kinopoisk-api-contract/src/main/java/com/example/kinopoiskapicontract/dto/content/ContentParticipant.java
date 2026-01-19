package com.example.kinopoiskapicontract.dto.content;

// ContentParticipantResponse.java - связь контент-персона

import com.example.kinopoiskapicontract.dto.enums.Role;

public record ContentParticipant(
        Long personId,
        String primaryName,
        Role role,
        String characterName  // для актеров
) {}
