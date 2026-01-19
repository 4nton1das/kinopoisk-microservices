package com.example.kinopoiskapicontract.dto.content;

import com.example.kinopoiskapicontract.dto.enums.ContentType;
import com.example.kinopoiskapicontract.dto.enums.Genre;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Relation(collectionRelation = "contents", itemRelation = "content")
public class ContentResponse extends RepresentationModel<ContentResponse> {
    private final Long id;
    private final String title;
    private final String description;
    private final ContentType contentType;
    private final LocalDate releaseDate;
    private final Set<Genre> genres;
    private final List<ContentParticipant> participants;
    private final LocalDateTime createdAt;

    public ContentResponse(Long id, String title, String description, ContentType contentType,
                           LocalDate releaseDate, Set<Genre> genres, List<ContentParticipant> participants,
                           LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.contentType = contentType;
        this.releaseDate = releaseDate;
        this.genres = genres;
        this.participants = participants;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public ContentType getContentType() { return contentType; }
    public LocalDate getReleaseDate() { return releaseDate; }
    public Set<Genre> getGenres() { return genres; }
    public List<ContentParticipant> getParticipants() { return participants; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
