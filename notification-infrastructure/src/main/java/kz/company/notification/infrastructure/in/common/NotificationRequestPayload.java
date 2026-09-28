package kz.company.notification.infrastructure.in.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Тело запроса на уведомление. Один формат для REST ({@code POST /api/v1/notifications}) и Kafka
 * ({@code notification.send.requests.v1}). Ровно одно из полей {@code template} и {@code content}.
 *
 * @param idempotencyKey ключ идемпотентности для Kafka; в REST используется заголовок Idempotency-Key
 */
@Schema(name = "NotificationRequest", description = "Запрос на уведомление")
public record NotificationRequestPayload(
        @NotNull @Valid RecipientPayload recipient,
        @Valid TemplatePayload template,
        @Valid ContentPayload content,
        @NotEmpty @Size(max = 16) List<@NotBlank String> channels,
        @NotBlank @Schema(allowableValues = {"TRANSACTIONAL", "MARKETING", "SYSTEM"}) String category,
        @Schema(allowableValues = {"LOW", "NORMAL", "HIGH"}, defaultValue = "NORMAL") String priority,
        @Schema(description = "Отложенная отправка (ISO 8601 со смещением)") OffsetDateTime sendAt,
        @Size(max = 20) Map<@Size(max = 64) String, @Size(max = 512) String> metadata,
        @Size(max = 255) @Schema(description = "Только для Kafka; в REST — заголовок Idempotency-Key") String idempotencyKey) {

    /**
     * @param contacts контакты по типам: email, phone (E.164), telegramChatId, deviceToken, webhookUrl.
     *                 Если нужного контакта нет, он запрашивается у сервиса пользователей.
     */
    public record RecipientPayload(
            @NotBlank @Size(max = 100) String id,
            @Size(max = 10) Map<@NotBlank String, @Size(max = 4096) String> contacts) {
    }

    /** Шаблон: ключ, локаль (BCP 47, необязательно) и переменные. */
    public record TemplatePayload(
            @NotBlank @Size(max = 100) String key,
            @Size(max = 35) String locale,
            @Size(max = 100) Map<String, Object> variables) {
    }

    /** Готовое содержимое без шаблона (scope notifications:send-raw). */
    public record ContentPayload(
            @Size(max = 998) String subject,
            @Size(max = 1_000_000) String text,
            @Size(max = 1_000_000) String html) {
    }
}
