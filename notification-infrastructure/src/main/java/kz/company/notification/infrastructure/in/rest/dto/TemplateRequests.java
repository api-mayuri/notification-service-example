package kz.company.notification.infrastructure.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

/** Запросы API шаблонов. */
public final class TemplateRequests {

    private TemplateRequests() {
    }

    @Schema(name = "CreateTemplateRequest")
    public record Create(
            @NotBlank @Size(max = 100) String key,
            @NotBlank String channel,
            @NotBlank @Size(max = 35) String locale,
            @Size(max = 998) String subject,
            @NotBlank @Size(max = 200_000) String body,
            @Size(max = 200_000) String textBody,
            Set<@NotBlank String> requiredVariables,
            @Schema(description = "Окно дедупликации, ISO 8601, например PT10M") Duration deduplicationWindow,
            boolean activate) {
    }

    @Schema(name = "AddTemplateVersionRequest")
    public record AddVersion(
            @NotBlank String channel,
            @NotBlank @Size(max = 35) String locale,
            @Size(max = 998) String subject,
            @NotBlank @Size(max = 200_000) String body,
            @Size(max = 200_000) String textBody,
            Set<@NotBlank String> requiredVariables,
            boolean activate) {
    }

    @Schema(name = "ActivateTemplateVersionRequest")
    public record Activate(@NotBlank String channel, @NotBlank String locale, @Positive int version) {
    }

    @Schema(name = "UpdateTemplateSettingsRequest")
    public record UpdateSettings(@NotBlank String channel, @NotBlank String locale,
                                 @Schema(description = "null — выключить дедупликацию") Duration deduplicationWindow) {
    }

    @Schema(name = "PreviewTemplateRequest")
    public record Preview(@NotBlank String channel, @NotBlank String locale,
                          @Schema(description = "Версия; по умолчанию активная") Integer version,
                          Map<String, Object> variables) {
    }
}
