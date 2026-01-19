package com.example.kinopoiskservice.assemblers;

import com.example.kinopoiskapicontract.dto.content.ContentResponse;
import com.example.kinopoiskapicontract.dto.content.ContentParticipant;
import com.example.kinopoiskservice.controllers.ContentController;
import com.example.kinopoiskservice.controllers.PersonController;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Component
public class ContentModelAssembler implements RepresentationModelAssembler<ContentResponse, EntityModel<ContentResponse>> {

    @Override
    @NonNull
    public EntityModel<ContentResponse> toModel(@NonNull ContentResponse content) {
        EntityModel<ContentResponse> entityModel = EntityModel.of(content,
                linkTo(methodOn(ContentController.class).getContentById(content.getId())).withSelfRel(),
                linkTo(methodOn(ContentController.class).getAllContent(null, null, null, null, 0, 20)).withRel("collection")
        );

        // Добавляем ссылки на каждого участника
        for (ContentParticipant participant : content.getParticipants()) {
            entityModel.add(linkTo(methodOn(PersonController.class).getPersonById(participant.personId())).withRel("participant"));
        }

        return entityModel;
    }
}
