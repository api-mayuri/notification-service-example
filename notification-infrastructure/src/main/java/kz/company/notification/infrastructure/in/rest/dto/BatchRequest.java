package kz.company.notification.infrastructure.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import kz.company.notification.infrastructure.in.common.NotificationRequestPayload;

import java.util.List;

/** Пакет до 500 уведомлений. Ключ элемента: его {@code idempotencyKey} или ключ пакета + ":" + индекс. */
@Schema(name = "NotificationBatchRequest")
public record BatchRequest(@NotEmpty @Size(max = 500) List<@Valid NotificationRequestPayload> items) {
}
