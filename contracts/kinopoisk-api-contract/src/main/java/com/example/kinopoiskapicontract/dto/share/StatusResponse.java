package com.example.kinopoiskapicontract.dto.share;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Стандартный ответ со статусом")
public record StatusResponse(
        @Schema(description = "Статус операции") String status,
        @Schema(description = "Сообщение об ошибке") String error
) {}
