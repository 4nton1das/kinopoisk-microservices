package com.example.kinopoiskservice.controllers;

import com.example.kinopoiskapicontract.dto.person.PersonRequest;
import com.example.kinopoiskapicontract.dto.person.PersonResponse;
import com.example.kinopoiskapicontract.dto.share.PagedResponse;
import com.example.kinopoiskapicontract.endpoints.PersonApi;
import com.example.kinopoiskservice.assemblers.PersonModelAssembler;
import com.example.kinopoiskservice.services.PersonService;
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
public class PersonController implements PersonApi {

    private final PersonService personService;
    private final PersonModelAssembler personModelAssembler;
    private final PagedResourcesAssembler<PersonResponse> pagedResourcesAssembler;

    public PersonController(PersonService personService,
                            PersonModelAssembler personModelAssembler,
                            PagedResourcesAssembler<PersonResponse> pagedResourcesAssembler) {
        this.personService = personService;
        this.personModelAssembler = personModelAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Override
    public EntityModel<PersonResponse> getPersonById(Long id) {
        PersonResponse person = personService.findById(id);
        return personModelAssembler.toModel(person);
    }

    @Override
    public PagedModel<EntityModel<PersonResponse>> getAllPersons(String search, int page, int size) {
        PagedResponse<PersonResponse> pagedResponse = personService.findAll(search, page, size);

        // Конвертируем наш PagedResponse в Spring Data Page
        Page<PersonResponse> personPage = new PageImpl<>(
                pagedResponse.content(),
                PageRequest.of(pagedResponse.pageNumber(), pagedResponse.pageSize()),
                pagedResponse.totalElements()
        );

        return pagedResourcesAssembler.toModel(personPage, personModelAssembler);
    }

    @Override
    public ResponseEntity<EntityModel<PersonResponse>> createPerson(@Valid PersonRequest request) {
        PersonResponse createdPerson = personService.create(request);
        EntityModel<PersonResponse> entityModel = personModelAssembler.toModel(createdPerson);

        URI location = entityModel.getRequiredLink("self").toUri();

        return ResponseEntity
                .created(location)
                .body(entityModel);
    }

    @Override
    public EntityModel<PersonResponse> updatePerson(Long id, @Valid PersonRequest request) {
        PersonResponse updatedPerson = personService.update(id, request);
        return personModelAssembler.toModel(updatedPerson);
    }

    @Override
    public void deletePerson(Long id) {
        personService.delete(id);
    }
}
