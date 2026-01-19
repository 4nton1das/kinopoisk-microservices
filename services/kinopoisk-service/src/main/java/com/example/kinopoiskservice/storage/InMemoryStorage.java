package com.example.kinopoiskservice.storage;

import com.example.kinopoiskapicontract.dto.content.ContentResponse;
import com.example.kinopoiskapicontract.dto.enums.ContentType;
import com.example.kinopoiskapicontract.dto.enums.Genre;
import com.example.kinopoiskapicontract.dto.enums.Role;
import com.example.kinopoiskapicontract.dto.person.PersonResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryStorage {
    // Основные хранилища
    public final Map<Long, ContentResponse> contents = new ConcurrentHashMap<>();
    public final Map<Long, PersonResponse> persons = new ConcurrentHashMap<>();

    // Связи между контентом и участниками
    public final Map<Long, List<Long>> contentParticipants = new ConcurrentHashMap<>(); // contentId -> list of personIds
    public final Map<Long, Map<Long, ContentParticipantInfo>> participantDetails = new ConcurrentHashMap<>(); // contentId -> (personId -> role/character)

    // Последовательности для ID
    public final AtomicLong contentSequence = new AtomicLong(0);
    public final AtomicLong personSequence = new AtomicLong(0);

    public record ContentParticipantInfo(Role role, String characterName) {}

    @PostConstruct
    public void init() {
        // Инициализируем начальными данными
        initSampleData();
    }

    private void initSampleData() {
        // Создаем 5 персон
        PersonResponse person1 = new PersonResponse(
                personSequence.incrementAndGet(),
                "Кристофер Нолан",
                LocalDate.of(1970, 7, 30),
                "Лондон, Англия",
                new ArrayList<>()
        );

        PersonResponse person2 = new PersonResponse(
                personSequence.incrementAndGet(),
                "Мэттью Макконахи",
                LocalDate.of(1969, 11, 4),
                "Ювалде, Техас, США",
                new ArrayList<>()
        );

        PersonResponse person3 = new PersonResponse(
                personSequence.incrementAndGet(),
                "Энн Хэтэуэй",
                LocalDate.of(1982, 11, 12),
                "Бруклин, Нью-Йорк, США",
                new ArrayList<>()
        );

        PersonResponse person4 = new PersonResponse(
                personSequence.incrementAndGet(),
                "Леонардо ДиКаприо",
                LocalDate.of(1974, 11, 11),
                "Лос-Анджелес, Калифорния, США",
                new ArrayList<>()
        );

        PersonResponse person5 = new PersonResponse(
                personSequence.incrementAndGet(),
                "Кейт Уинслет",
                LocalDate.of(1975, 10, 5),
                "Рединг, Беркшир, Англия",
                new ArrayList<>()
        );

        // Сохраняем персон
        persons.put(person1.getId(), person1);
        persons.put(person2.getId(), person2);
        persons.put(person3.getId(), person3);
        persons.put(person4.getId(), person4);
        persons.put(person5.getId(), person5);

        // Создаем 3 контента
        ContentResponse content1 = new ContentResponse(
                contentSequence.incrementAndGet(),
                "Интерстеллар",
                "Фантастический эпос о путешествии через червоточину",
                ContentType.MOVIE,
                LocalDate.of(2014, 10, 26),
                Set.of(Genre.SCI_FI, Genre.DRAMA),
                new ArrayList<>(),
                LocalDateTime.now()
        );

        ContentResponse content2 = new ContentResponse(
                contentSequence.incrementAndGet(),
                "Начало",
                "Фильм о проникновении в сны",
                ContentType.MOVIE,
                LocalDate.of(2010, 7, 16),
                Set.of(Genre.SCI_FI, Genre.ACTION, Genre.THRILLER),
                new ArrayList<>(),
                LocalDateTime.now()
        );

        ContentResponse content3 = new ContentResponse(
                contentSequence.incrementAndGet(),
                "Титаник",
                "Романтическая драма на фоне крушения Титаника",
                ContentType.MOVIE,
                LocalDate.of(1997, 12, 19),
                Set.of(Genre.ROMANCE, Genre.DRAMA),
                new ArrayList<>(),
                LocalDateTime.now()
        );

        contents.put(content1.getId(), content1);
        contents.put(content2.getId(), content2);
        contents.put(content3.getId(), content3);

        // Создаем связи контент-участники

        // Интерстеллар
        addParticipantToContent(content1.getId(), person1.getId(), Role.DIRECTOR, null);
        addParticipantToContent(content1.getId(), person2.getId(), Role.ACTOR, "Купер");
        addParticipantToContent(content1.getId(), person3.getId(), Role.ACTOR, "Амелия Бранд");

        // Начало
        addParticipantToContent(content2.getId(), person1.getId(), Role.DIRECTOR, null);
        addParticipantToContent(content2.getId(), person4.getId(), Role.ACTOR, "Дом Кобб");

        // Титаник
        addParticipantToContent(content3.getId(), person4.getId(), Role.ACTOR, "Джек Доусон");
        addParticipantToContent(content3.getId(), person5.getId(), Role.ACTOR, "Роза Дьюитт Бьюкейтер");
    }

    private void addParticipantToContent(Long contentId, Long personId, Role role, String characterName) {
        contentParticipants.computeIfAbsent(contentId, k -> new ArrayList<>()).add(personId);
        participantDetails.computeIfAbsent(contentId, k -> new ConcurrentHashMap<>())
                .put(personId, new ContentParticipantInfo(role, characterName));
    }
}
