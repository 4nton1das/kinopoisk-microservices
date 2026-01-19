package com.example.kinopoiskservice.controllers;

import org.springframework.hateoas.RepresentationModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api")
public class RootController {

    @GetMapping
    public RepresentationModel<?> getRoot() {
        RepresentationModel<?> rootModel = new RepresentationModel<>();

        rootModel.add(
                linkTo(methodOn(ContentController.class).getAllContent(null, null, null, null, 0, 20))
                        .withRel("content"),
                linkTo(methodOn(PersonController.class).getAllPersons(null, 0, 20))
                        .withRel("persons"),
                linkTo(methodOn(RootController.class).getRoot())
                        .slash("swagger-ui.html")
                        .withRel("documentation")
        );

        return rootModel;
    }
}
