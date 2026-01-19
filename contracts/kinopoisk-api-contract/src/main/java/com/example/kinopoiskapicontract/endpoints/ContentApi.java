package com.example.kinopoiskapicontract.endpoints;

import com.example.kinopoiskapicontract.dto.content.ContentParticipant;
import com.example.kinopoiskapicontract.dto.content.ContentRequest;
import com.example.kinopoiskapicontract.dto.content.ContentResponse;
import com.example.kinopoiskapicontract.dto.enums.ContentType;
import com.example.kinopoiskapicontract.dto.enums.Genre;
import com.example.kinopoiskapicontract.dto.share.StatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "content", description = "API для работы с контентом (фильмы, сериалы)")
@RequestMapping("/api/content")
public interface ContentApi {

    @Operation(summary = "Получить контент по ID")
    @ApiResponse(responseCode = "200", description = "Контент найден")
    @ApiResponse(responseCode = "404", description = "Контент не найден",
            content = @Content(schema = @Schema(implementation = StatusResponse.class)))
    @GetMapping("/{id}")
    EntityModel<ContentResponse> getContentById(@PathVariable Long id);

    @Operation(summary = "Получить список контента с пагинацией и фильтрацией")
    @ApiResponse(responseCode = "200", description = "Список контента")
    @GetMapping
    PagedModel<EntityModel<ContentResponse>> getAllContent(
            @Parameter(description = "Фильтр по жанру") @RequestParam(required = false) Genre genre,
            @Parameter(description = "Фильтр по типу") @RequestParam(required = false) ContentType contentType,
            @Parameter(description = "Фильтр по году") @RequestParam(required = false) Integer year,
            @Parameter(description = "Поиск по названию") @RequestParam(required = false) String search,
            @Parameter(description = "Номер страницы (0..N)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "20") int size
    );

    @Operation(summary = "Создать новый контент")
    @ApiResponse(responseCode = "201", description = "Контент успешно создан")
    @ApiResponse(responseCode = "400", description = "Невалидный запрос",
            content = @Content(schema = @Schema(implementation = StatusResponse.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ResponseEntity<EntityModel<ContentResponse>> createContent(@Valid @RequestBody ContentRequest request);

    @Operation(summary = "Обновить контент")
    @ApiResponse(responseCode = "200", description = "Контент обновлен")
    @ApiResponse(responseCode = "404", description = "Контент не найден",
            content = @Content(schema = @Schema(implementation = StatusResponse.class)))
    @PutMapping("/{id}")
    EntityModel<ContentResponse> updateContent(@PathVariable Long id, @Valid @RequestBody ContentRequest request);

    @Operation(summary = "Удалить контент")
    @ApiResponse(responseCode = "204", description = "Контент удален")
    @ApiResponse(responseCode = "404", description = "Контент не найден")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteContent(@PathVariable Long id);

    @Operation(summary = "Добавить участника к контенту")
    @ApiResponse(responseCode = "200", description = "Участник добавлен")
    @ApiResponse(responseCode = "404", description = "Контент или персона не найдена",
            content = @Content(schema = @Schema(implementation = StatusResponse.class)))
    @ApiResponse(responseCode = "409", description = "Участник уже добавлен к этому контенту",
            content = @Content(schema = @Schema(implementation = StatusResponse.class)))
    @PostMapping("/{contentId}/participants")
    EntityModel<ContentResponse> addParticipant(
            @PathVariable Long contentId,
            @Valid @RequestBody ContentParticipant request
    );

    @Operation(summary = "Удалить участника из контента")
    @ApiResponse(responseCode = "200", description = "Участник удален")
    @ApiResponse(responseCode = "404", description = "Контент или участник не найден")
    @DeleteMapping("/{contentId}/participants/{personId}")
    EntityModel<ContentResponse> removeParticipant(
            @PathVariable Long contentId,
            @PathVariable Long personId
    );
}
