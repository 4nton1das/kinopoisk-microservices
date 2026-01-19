package com.example.kinopoiskservice.graphql;

import com.example.kinopoiskapicontract.dto.person.PersonRequest;
import com.example.kinopoiskapicontract.dto.person.PersonResponse;
import com.example.kinopoiskapicontract.dto.share.PagedResponse;
import com.example.kinopoiskservice.services.PersonService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@DgsComponent
public class PersonDataFetcher {

    private final PersonService personService;

    @Autowired
    public PersonDataFetcher(PersonService personService) {
        this.personService = personService;
    }

    @DgsQuery
    public PagedResponse<PersonResponse> persons(
            @InputArgument String search,
            @InputArgument Integer page,
            @InputArgument Integer size) {
        return personService.findAll(search, page, size);
    }

    @DgsQuery
    public PersonResponse personById(@InputArgument Long id) {
        return personService.findById(id);
    }

    @DgsQuery
    public List<PersonResponse> personsByContent(@InputArgument Long contentId) {
        return personService.findPersonsByContent(contentId);
    }

    @DgsMutation
    public PersonResponse createPerson(@InputArgument("input") Map<String, Object> input) {
        PersonRequest request = new PersonRequest(
                (String) input.get("primaryName"),
                input.get("birthDate") != null ? LocalDate.parse((String) input.get("birthDate")) : null,
                (String) input.get("birthPlace")
        );
        return personService.create(request);
    }

    @DgsMutation
    public PersonResponse updatePerson(@InputArgument Long id, @InputArgument("input") Map<String, Object> input) {
        PersonRequest request = new PersonRequest(
                (String) input.get("primaryName"),
                input.get("birthDate") != null ? LocalDate.parse((String) input.get("birthDate")) : null,
                (String) input.get("birthPlace")
        );
        return personService.update(id, request);
    }

    @DgsMutation
    public Long deletePerson(@InputArgument Long id) {
        personService.delete(id);
        return id;
    }
}
