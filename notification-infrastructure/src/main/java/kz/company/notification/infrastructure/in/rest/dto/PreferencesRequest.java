package kz.company.notification.infrastructure.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;
import java.util.Set;

/**
 * Предпочтения получателя. PUT заменяет всё (отсутствующее поле очищается), PATCH меняет только
 * переданные поля; в PATCH для очистки используйте {@code clearQuietHours} и {@code clearLocale}.
 */
@Schema(name = "RecipientPreferencesRequest")
public record PreferencesRequest(
        @Schema(example = "[\"SMS\"]") Set<String> unsubscribedChannels,
        @Schema(example = "[\"MARKETING\"]", description = "Только необязательные категории (MARKETING)") Set<String> unsubscribedCategories,
        @Schema(example = "22:00") LocalTime quietHoursStart,
        @Schema(example = "08:00") LocalTime quietHoursEnd,
        @Schema(example = "Asia/Almaty") String timeZone,
        @Schema(example = "ru-KZ") String locale,
        Boolean clearQuietHours,
        Boolean clearLocale) {
}
