package com.example.kinopoiskservice.graphql;

import com.example.kinopoiskapicontract.dto.content.ContentParticipant;
import com.example.kinopoiskapicontract.dto.content.ContentRequest;
import com.example.kinopoiskapicontract.dto.content.ContentResponse;
import com.example.kinopoiskapicontract.dto.enums.ContentType;
import com.example.kinopoiskapicontract.dto.enums.Genre;
import com.example.kinopoiskapicontract.dto.person.PersonResponse;
import com.example.kinopoiskapicontract.dto.share.PagedResponse;
import com.example.kinopoiskservice.services.ContentService;
import com.example.kinopoiskservice.services.PersonService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@DgsComponent
public class ContentDataFetcher {

    private final ContentService contentService;
    private final PersonService personService;

    @Autowired
    public ContentDataFetcher(ContentService contentService, PersonService personService) {
        this.contentService = contentService;
        this.personService = personService;
    }

    @DgsQuery
    public PagedResponse<ContentResponse> contents(
            @InputArgument Genre genre,
            @InputArgument ContentType contentType,
            @InputArgument Integer year,
            @InputArgument String search,
            @InputArgument Integer page,
            @InputArgument Integer size) {
        return contentService.findAll(genre, contentType, year, search, page, size);
    }

    @DgsQuery
    public ContentResponse contentById(@InputArgument Long id) {
        return contentService.findById(id);
    }

    @DgsQuery
    public List<ContentResponse> contentByPerson(@InputArgument Long personId) {
        return contentService.findContentByPerson(personId);
    }

    @DgsMutation
    public ContentResponse createContent(@InputArgument("input") Map<String, Object> input) {
        ContentRequest request = new ContentRequest(
                (String) input.get("title"),
                (String) input.get("description"),
                ContentType.valueOf((String) input.get("contentType")),
                input.get("releaseDate") != null ? LocalDate.parse((String) input.get("releaseDate")) : null,
                safeConvertToGenres(input.get("genres"))
        );
        return contentService.create(request);
    }

    @DgsMutation
    public ContentResponse updateContent(@InputArgument Long id, @InputArgument("input") Map<String, Object> input) {
        ContentRequest request = new ContentRequest(
                (String) input.get("title"),
                (String) input.get("description"),
                ContentType.valueOf((String) input.get("contentType")),
                input.get("releaseDate") != null ? LocalDate.parse((String) input.get("releaseDate")) : null,
                safeConvertToGenres(input.get("genres"))
        );
        return contentService.update(id, request);
    }

    @DgsMutation
    public Long deleteContent(@InputArgument Long id) {
        contentService.delete(id);
        return id;
    }

    @DgsMutation
    public ContentResponse addParticipant(@InputArgument Long contentId,
                                          @InputArgument("input") Map<String, Object> input) {
        PersonResponse person = personService.findById(Long.parseLong(input.get("personId").toString()));

        ContentParticipant participantRequest = new ContentParticipant(
                person.getId(),
                person.getPrimaryName(),
                com.example.kinopoiskapicontract.dto.enums.Role.valueOf((String) input.get("role")),
                (String) input.get("characterName")
        );
        return contentService.addParticipant(contentId, participantRequest);
    }

    @DgsMutation
    public ContentResponse removeParticipant(@InputArgument Long contentId, @InputArgument Long personId) {
        return contentService.removeParticipant(contentId, personId);
    }

    private Set<Genre> safeConvertToGenres(Object genresObj) {
        if (genresObj instanceof List) {
            try {
                return ((List<?>) genresObj).stream()
                        .filter(item -> item instanceof String)
                        .map(item -> Genre.valueOf((String) item))
                        .collect(Collectors.toSet());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid genre value: " + e.getMessage());
            }
        }
        return null;
    }
}
