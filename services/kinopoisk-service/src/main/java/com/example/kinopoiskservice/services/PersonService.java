package com.example.kinopoiskservice.services;

import com.example.kinopoiskapicontract.dto.person.PersonFilmography;
import com.example.kinopoiskapicontract.dto.person.PersonRequest;
import com.example.kinopoiskapicontract.dto.person.PersonResponse;
import com.example.kinopoiskapicontract.dto.share.PagedResponse;
import com.example.kinopoiskapicontract.exception.ResourceNotFoundException;
import com.example.kinopoiskeventscontract.events.PersonCreatedEvent;
import com.example.kinopoiskeventscontract.events.PersonDeletedEvent;
import com.example.kinopoiskservice.config.RabbitMQConfig;
import com.example.kinopoiskservice.storage.InMemoryStorage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PersonService {

    private final InMemoryStorage storage;
    private final RabbitTemplate rabbitTemplate;

    public PersonService(InMemoryStorage storage, RabbitTemplate rabbitTemplate) {
        this.storage = storage;
        this.rabbitTemplate = rabbitTemplate;
    }

    public PersonResponse findById(Long id) {
        PersonResponse person = storage.persons.get(id);
        if (person == null) {
            throw new ResourceNotFoundException("Person", id);
        }
        return enrichPersonWithFilmography(person);
    }

    public PagedResponse<PersonResponse> findAll(String search, int page, int size) {
        Stream<PersonResponse> personStream = storage.persons.values().stream()
                .map(this::enrichPersonWithFilmography)
                .sorted(Comparator.comparing(PersonResponse::getId));

        // Применяем фильтры
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase();
            personStream = personStream.filter(person ->
                    person.getPrimaryName().toLowerCase().contains(searchLower));
        }

        List<PersonResponse> allPersons = personStream.collect(Collectors.toList());
        return createPagedResponse(allPersons, page, size);
    }

    public PersonResponse create(PersonRequest request) {
        long id = storage.personSequence.incrementAndGet();

        PersonResponse person = new PersonResponse(
                id,
                request.primaryName(),
                request.birthDate(),
                request.birthPlace(),
                new ArrayList<>() // Пустая фильмография при создании
        );

        storage.persons.put(id, person);

        // Публикация события после создания
        PersonCreatedEvent event = new PersonCreatedEvent(
                person.getId(),
                person.getPrimaryName()
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_PERSON_CREATED,
                event
        );

        return person;
    }

    public PersonResponse update(Long id, PersonRequest request) {
        PersonResponse existingPerson = findById(id);

        PersonResponse updatedPerson = new PersonResponse(
                id,
                request.primaryName(),
                request.birthDate(),
                request.birthPlace(),
                existingPerson.getFilmography() // Сохраняем существующую фильмографию
        );

        storage.persons.put(id, updatedPerson);
        return updatedPerson;
    }

    public void delete(Long id) {
        PersonResponse person = findById(id); // Убедимся, что личность существует
        findById(id); // Проверяем существование

        // Удаляем связи с контентом
        removePersonFromAllContent(id);

        storage.persons.remove(id);

        // Публикация события после удаления
        PersonDeletedEvent event = new PersonDeletedEvent(id);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_PERSON_DELETED,
                event
        );
    }

    private PersonResponse enrichPersonWithFilmography(PersonResponse person) {
        List<PersonFilmography> filmography = storage.contentParticipants.entrySet().stream()
                .filter(entry -> entry.getValue().contains(person.getId()))
                .map(entry -> {
                    Long contentId = entry.getKey();
                    var content = storage.contents.get(contentId);
                    var details = storage.participantDetails.get(contentId).get(person.getId());

                    return new PersonFilmography(
                            contentId,
                            content.getTitle(),
                            content.getContentType(),
                            content.getReleaseDate(),
                            details.role(),
                            details.characterName()
                    );
                })
                .collect(Collectors.toList());

        return new PersonResponse(
                person.getId(),
                person.getPrimaryName(),
                person.getBirthDate(),
                person.getBirthPlace(),
                filmography
        );
    }

    private void removePersonFromAllContent(Long personId) {
        // Удаляем персону из всех контентов
        for (Long contentId : storage.contentParticipants.keySet()) {
            List<Long> participants = storage.contentParticipants.get(contentId);
            if (participants != null) {
                participants.remove(personId);
            }

            Map<Long, InMemoryStorage.ContentParticipantInfo> details = storage.participantDetails.get(contentId);
            if (details != null) {
                details.remove(personId);
            }
        }
    }

    private PagedResponse<PersonResponse> createPagedResponse(List<PersonResponse> allPersons, int page, int size) {
        int totalElements = allPersons.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);

        List<PersonResponse> pageContent = (fromIndex >= totalElements) ?
                new ArrayList<>() : allPersons.subList(fromIndex, toIndex);

        return new PagedResponse<>(
                pageContent,
                page,
                size,
                totalElements,
                totalPages,
                page >= totalPages - 1
        );
    }

    public List<PersonResponse> findPersonsByContent(Long contentId) {
        return storage.persons.values().stream()
                // Сначала фильтруем по фильмографии
                .filter(person -> {
                    List<Long> participantIds = storage.contentParticipants.getOrDefault(contentId, new ArrayList<>());
                    return participantIds.contains(person.getId());
                })
                // Потом обогащаем
                .map(this::enrichPersonWithFilmography)
                .collect(Collectors.toList());
    }
}
