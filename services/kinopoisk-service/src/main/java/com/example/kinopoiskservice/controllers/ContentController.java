package com.example.kinopoiskservice.controllers;

import com.example.kinopoiskapicontract.dto.content.ContentParticipant;
import com.example.kinopoiskapicontract.dto.content.ContentRequest;
import com.example.kinopoiskapicontract.dto.content.ContentResponse;
import com.example.kinopoiskapicontract.dto.enums.ContentType;
import com.example.kinopoiskapicontract.dto.enums.Genre;
import com.example.kinopoiskapicontract.dto.share.PagedResponse;
import com.example.kinopoiskapicontract.endpoints.ContentApi;
import com.example.kinopoiskservice.assemblers.ContentModelAssembler;
import com.example.kinopoiskservice.services.ContentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class ContentController implements ContentApi {

    private final ContentService contentService;
    private final ContentModelAssembler contentModelAssembler;
    private final PagedResourcesAssembler<ContentResponse> pagedResourcesAssembler;

    public ContentController(ContentService contentService,
                             ContentModelAssembler contentModelAssembler,
                             PagedResourcesAssembler<ContentResponse> pagedResourcesAssembler) {
        this.contentService = contentService;
        this.contentModelAssembler = contentModelAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Override
    public EntityModel<ContentResponse> getContentById(Long id) {
        ContentResponse content = contentService.findById(id);
        return contentModelAssembler.toModel(content);
    }

    @Override
    public PagedModel<EntityModel<ContentResponse>> getAllContent(Genre genre, ContentType contentType,
                                                                  Integer year, String search, int page, int size) {
        PagedResponse<ContentResponse> pagedResponse = contentService.findAll(genre, contentType, year, search, page, size);

        // Конвертируем наш PagedResponse в Spring Data Page
        Page<ContentResponse> contentPage = new PageImpl<>(
                pagedResponse.content(),
                PageRequest.of(pagedResponse.pageNumber(), pagedResponse.pageSize()),
                pagedResponse.totalElements()
        );

        return pagedResourcesAssembler.toModel(contentPage, contentModelAssembler);
    }

    @Override
    public ResponseEntity<EntityModel<ContentResponse>> createContent(@Valid ContentRequest request) {
        ContentResponse createdContent = contentService.create(request);
        EntityModel<ContentResponse> entityModel = contentModelAssembler.toModel(createdContent);

        URI location = entityModel.getRequiredLink("self").toUri();

        return ResponseEntity
                .created(location)
                .body(entityModel);
    }

    @Override
    public EntityModel<ContentResponse> updateContent(Long id, @Valid ContentRequest request) {
        ContentResponse updatedContent = contentService.update(id, request);
        return contentModelAssembler.toModel(updatedContent);
    }

    @Override
    public void deleteContent(Long id) {
        contentService.delete(id);
    }

    @Override
    public EntityModel<ContentResponse> addParticipant(Long contentId, @Valid ContentParticipant request) {
        ContentResponse updatedContent = contentService.addParticipant(contentId, request);
        return contentModelAssembler.toModel(updatedContent);
    }

    @Override
    public EntityModel<ContentResponse> removeParticipant(Long contentId, Long personId) {
        ContentResponse updatedContent = contentService.removeParticipant(contentId, personId);
        return contentModelAssembler.toModel(updatedContent);
    }
}
