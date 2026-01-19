package com.example.kinopoiskservice.services;

import com.example.kinopoiskapicontract.dto.content.ContentParticipant;
import com.example.kinopoiskapicontract.dto.content.ContentRequest;
import com.example.kinopoiskapicontract.dto.content.ContentResponse;
import com.example.kinopoiskapicontract.dto.enums.ContentType;
import com.example.kinopoiskapicontract.dto.enums.Genre;
import com.example.kinopoiskapicontract.dto.share.PagedResponse;
import com.example.kinopoiskapicontract.exception.ParticipantAlreadyExistsException;
import com.example.kinopoiskapicontract.exception.ResourceNotFoundException;
import com.example.kinopoiskeventscontract.events.ContentCreatedEvent;
import com.example.kinopoiskeventscontract.events.ContentDeletedEvent;
import com.example.kinopoiskservice.config.RabbitMQConfig;
import com.example.kinopoiskservice.storage.InMemoryStorage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ContentService {

    private final InMemoryStorage storage;
    private final PersonService personService;
    private final RabbitTemplate rabbitTemplate;

    public ContentService(InMemoryStorage storage, PersonService personService, RabbitTemplate rabbitTemplate) {
        this.storage = storage;
        this.personService = personService;
        this.rabbitTemplate = rabbitTemplate;
    }

    public ContentResponse findById(Long id) {
        ContentResponse content = storage.contents.get(id);
        if (content == null) {
            throw new ResourceNotFoundException("Content", id);
        }
        return enrichContentWithParticipants(content);
    }

    public PagedResponse<ContentResponse> findAll(Genre genre, ContentType contentType, Integer year, String search, int page, int size) {
        Stream<ContentResponse> contentStream = storage.contents.values().stream()
                .map(this::enrichContentWithParticipants)
                .sorted(Comparator.comparing(ContentResponse::getId));

        // Применяем фильтры
        if (genre != null) {
            contentStream = contentStream.filter(content -> content.getGenres().contains(genre));
        }

        if (contentType != null) {
            contentStream = contentStream.filter(content -> content.getContentType() == contentType);
        }

        if (year != null) {
            contentStream = contentStream.filter(content ->
                    content.getReleaseDate().getYear() == year);
        }

        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase();
            contentStream = contentStream.filter(content ->
                    content.getTitle().toLowerCase().contains(searchLower) ||
                            content.getDescription().toLowerCase().contains(searchLower));
        }

        List<ContentResponse> allContent = contentStream.collect(Collectors.toList());

        // Пагинация
        return createPagedResponse(allContent, page, size);
    }

    public ContentResponse create(ContentRequest request) {
        long id = storage.contentSequence.incrementAndGet();

        ContentResponse content = new ContentResponse(
                id,
                request.title(),
                request.description(),
                request.contentType(),
                request.releaseDate(),
                request.genres(),
                new ArrayList<>(),
                LocalDateTime.now()
        );

        storage.contents.put(id, content);

        // Публикация события после создания
        ContentCreatedEvent event = new ContentCreatedEvent(
                content.getId(),
                content.getTitle()
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_CONTENT_CREATED,
                event
        );

        return content;
    }

    public ContentResponse update(Long id, ContentRequest request) {
        ContentResponse existingContent = findById(id);

        ContentResponse updatedContent = new ContentResponse(
                id,
                request.title(),
                request.description(),
                request.contentType(),
                request.releaseDate(),
                request.genres(),
                existingContent.getParticipants(), // Сохраняем существующих участников
                existingContent.getCreatedAt()
        );

        storage.contents.put(id, updatedContent);
        return updatedContent;
    }

    public void delete(Long id) {
        ContentResponse content = findById(id); // Убедимся, что контент существует
        findById(id);

        // Удаляем связи с участниками
        storage.contentParticipants.remove(id);
        storage.participantDetails.remove(id);

        storage.contents.remove(id);

        // Публикация события после удаления
        ContentDeletedEvent event = new ContentDeletedEvent(id);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_CONTENT_DELETED,
                event
        );
    }

    public ContentResponse addParticipant(Long contentId, ContentParticipant request) {
        findById(contentId);
        personService.findById(request.personId()); // Проверяем что персона существует

        // Проверка на дублирование
        List<Long> existingParticipants = storage.contentParticipants.getOrDefault(contentId, new ArrayList<>());
        if (existingParticipants.contains(request.personId())) {
            throw new ParticipantAlreadyExistsException(contentId, request.personId());
        }

        // Добавляем связь
        storage.contentParticipants.computeIfAbsent(contentId, k -> new ArrayList<>()).add(request.personId());
        storage.participantDetails.computeIfAbsent(contentId, k -> new HashMap<>())
                .put(request.personId(),
                        new InMemoryStorage.ContentParticipantInfo(request.role(), request.characterName()));

        return findById(contentId); // Возвращаем обновленный контент
    }

    public ContentResponse removeParticipant(Long contentId, Long personId) {
        findById(contentId);

        // Удаляем связь
        List<Long> participants = storage.contentParticipants.get(contentId);
        if (participants != null) {
            participants.remove(personId);
        }

        Map<Long, InMemoryStorage.ContentParticipantInfo> details = storage.participantDetails.get(contentId);
        if (details != null) {
            details.remove(personId);
        }

        return findById(contentId); // Возвращаем обновленный контент
    }

    private ContentResponse enrichContentWithParticipants(ContentResponse content) {
        List<Long> participantIds = storage.contentParticipants.getOrDefault(content.getId(), new ArrayList<>());
        List<ContentParticipant> participants = participantIds.stream()
                .map(personId -> {
                    var person = storage.persons.get(personId);
                    var details = storage.participantDetails.get(content.getId()).get(personId);

                    return new ContentParticipant(
                            personId,
                            person.getPrimaryName(),
                            details.role(),
                            details.characterName()
                    );
                })
                .toList();

        return new ContentResponse(
                content.getId(),
                content.getTitle(),
                content.getDescription(),
                content.getContentType(),
                content.getReleaseDate(),
                content.getGenres(),
                participants,
                content.getCreatedAt()
        );
    }

    private PagedResponse<ContentResponse> createPagedResponse(List<ContentResponse> allContent, int page, int size) {
        int totalElements = allContent.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);

        List<ContentResponse> pageContent = (fromIndex >= totalElements) ?
                new ArrayList<>() : allContent.subList(fromIndex, toIndex);

        return new PagedResponse<>(
                pageContent,
                page,
                size,
                totalElements,
                totalPages,
                page >= totalPages - 1
        );
    }

    public List<ContentResponse> findContentByPerson(Long personId) {
        return storage.contents.values().stream()
                // Сначала фильтруем по участникам
                .filter(content -> {
                    List<Long> participantIds = storage.contentParticipants.getOrDefault(content.getId(), new ArrayList<>());
                    return participantIds.contains(personId);
                })
                // Потом обогащаем ТОЛЬКО отфильтрованные данные
                .map(this::enrichContentWithParticipants)
                .collect(Collectors.toList());
    }
}
