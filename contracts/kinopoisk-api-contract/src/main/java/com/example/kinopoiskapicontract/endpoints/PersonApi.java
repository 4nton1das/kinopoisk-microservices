package com.example.kinopoiskapicontract.endpoints;

import com.example.kinopoiskapicontract.dto.person.PersonRequest;
import com.example.kinopoiskapicontract.dto.person.PersonResponse;
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

@Tag(name = "persons", description = "API для работы с участниками (актеры, режиссеры и т.д.)")
@RequestMapping("/api/persons")
public interface PersonApi {

    @Operation(summary = "Получить участника по ID")
    @ApiResponse(responseCode = "200", description = "Участник найден")
    @ApiResponse(responseCode = "404", description = "Участник не найден",
            content = @Content(schema = @Schema(implementation = StatusResponse.class)))
    @GetMapping("/{id}")
    EntityModel<PersonResponse> getPersonById(@PathVariable Long id);

    @Operation(summary = "Получить список участников с пагинацией и фильтрацией")
    @ApiResponse(responseCode = "200", description = "Список участников")
    @GetMapping
    PagedModel<EntityModel<PersonResponse>> getAllPersons(
            @Parameter(description = "Поиск по имени") @RequestParam(required = false) String search,
            @Parameter(description = "Номер страницы (0..N)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "20") int size
    );

    @Operation(summary = "Создать нового участника")
    @ApiResponse(responseCode = "201", description = "Участник успешно создан")
    @ApiResponse(responseCode = "400", description = "Невалидный запрос",
            content = @Content(schema = @Schema(implementation = StatusResponse.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ResponseEntity<EntityModel<PersonResponse>> createPerson(@Valid @RequestBody PersonRequest request);

    @Operation(summary = "Обновить участника")
    @ApiResponse(responseCode = "200", description = "Участник обновлен")
    @ApiResponse(responseCode = "404", description = "Участник не найден",
            content = @Content(schema = @Schema(implementation = StatusResponse.class)))
    @PutMapping("/{id}")
    EntityModel<PersonResponse> updatePerson(@PathVariable Long id, @Valid @RequestBody PersonRequest request);

    @Operation(summary = "Удалить участника")
    @ApiResponse(responseCode = "204", description = "Участник удален")
    @ApiResponse(responseCode = "404", description = "Участник не найден")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deletePerson(@PathVariable Long id);
}
