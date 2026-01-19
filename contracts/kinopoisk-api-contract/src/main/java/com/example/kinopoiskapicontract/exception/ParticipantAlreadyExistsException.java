package com.example.kinopoiskapicontract.exception;

public class ParticipantAlreadyExistsException extends DuplicateResourceException {
    public ParticipantAlreadyExistsException(Long contentId, Long personId) {
        super("Participant with personId=" + personId + " already exists in content with id=" + contentId);
    }
}
