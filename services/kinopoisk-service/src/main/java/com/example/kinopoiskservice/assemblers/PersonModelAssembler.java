package com.example.kinopoiskservice.assemblers;

import com.example.kinopoiskapicontract.dto.person.PersonResponse;
import com.example.kinopoiskapicontract.dto.person.PersonFilmography;
import com.example.kinopoiskservice.controllers.PersonController;
import com.example.kinopoiskservice.controllers.ContentController;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Component
public class PersonModelAssembler implements RepresentationModelAssembler<PersonResponse, EntityModel<PersonResponse>> {

    @Override
    @NonNull
    public EntityModel<PersonResponse> toModel(@NonNull PersonResponse person) {
        EntityModel<PersonResponse> entityModel = EntityModel.of(person,
                linkTo(methodOn(PersonController.class).getPersonById(person.getId())).withSelfRel(),
                linkTo(methodOn(PersonController.class).getAllPersons(null, 0, 20)).withRel("collection")
        );

        // Добавляем ссылки на контент из фильмографии
        if (person.getFilmography() != null) {
            for (PersonFilmography film : person.getFilmography()) {
                entityModel.add(linkTo(methodOn(ContentController.class).getContentById(film.contentId())).withRel("filmography"));
            }
        }

        return entityModel;
    }

    @Override
    @NonNull
    public CollectionModel<EntityModel<PersonResponse>> toCollectionModel(@NonNull Iterable<? extends PersonResponse> entities) {
        CollectionModel<EntityModel<PersonResponse>> collectionModel = RepresentationModelAssembler.super.toCollectionModel(entities);

        collectionModel.add(linkTo(methodOn(PersonController.class).getAllPersons(null, 0, 20)).withSelfRel());

        return collectionModel;
    }
}
