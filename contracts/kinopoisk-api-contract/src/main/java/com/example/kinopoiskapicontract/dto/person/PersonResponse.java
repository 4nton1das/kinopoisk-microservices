package com.example.kinopoiskapicontract.dto.person;

import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDate;
import java.util.List;

@Relation(collectionRelation = "persons", itemRelation = "person")
public class PersonResponse extends RepresentationModel<PersonResponse> {
    private final Long id;
    private final String primaryName;
    private final LocalDate birthDate;
    private final String birthPlace;
    private final List<PersonFilmography> filmography;

    public PersonResponse(Long id, String primaryName, LocalDate birthDate,
                          String birthPlace, List<PersonFilmography> filmography) {
        this.id = id;
        this.primaryName = primaryName;
        this.birthDate = birthDate;
        this.birthPlace = birthPlace;
        this.filmography = filmography;
    }

    public Long getId() { return id; }
    public String getPrimaryName() { return primaryName; }
    public LocalDate getBirthDate() { return birthDate; }
    public String getBirthPlace() { return birthPlace; }
    public List<PersonFilmography> getFilmography() { return filmography; }
}
