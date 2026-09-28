package kz.company.notification.infrastructure.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Ответ на приём уведомления.
 *
 * @param outcome CREATED — принято новое; REPLAYED — повтор по ключу идемпотентности;
 *                DEDUPLICATED — схлопнуто с недавним одинаковым уведомлением
 */
@Schema(name = "NotificationAccepted")
public record NotificationAcceptedResponse(UUID id, String status,
                                           @Schema(allowableValues = {"CREATED", "REPLAYED", "DEDUPLICATED"}) String outcome) {
}
