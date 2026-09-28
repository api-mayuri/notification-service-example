package kz.company.notification.infrastructure.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.company.notification.application.port.in.ManagePreferencesUseCase;
import kz.company.notification.application.port.in.PreferencesView;
import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.notification.RecipientId;
import kz.company.notification.domain.preferences.PreferencesChange;
import kz.company.notification.domain.preferences.QuietHours;
import kz.company.notification.infrastructure.in.rest.dto.PreferencesRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST API предпочтений получателя. Scope {@code preferences:manage}. */
@RestController
@RequestMapping("/api/v1/recipients/{recipientId}/preferences")
@Tag(name = "Preferences", description = "Отписки, тихие часы, часовой пояс и язык получателя")
@SecurityRequirement(name = "bearer-jwt")
public class PreferencesController {

    private final ManagePreferencesUseCase managePreferences;

    public PreferencesController(ManagePreferencesUseCase managePreferences) {
        this.managePreferences = managePreferences;
    }

    @GetMapping
    @Operation(summary = "Предпочтения (значения по умолчанию, если получатель ничего не настраивал)")
    public PreferencesView get(@PathVariable String recipientId) {
        return managePreferences.get(new RecipientId(recipientId));
    }

    @PutMapping
    @Operation(summary = "Заменить предпочтения целиком")
    public PreferencesView replace(@PathVariable String recipientId, @RequestBody PreferencesRequest request) {
        return managePreferences.change(new RecipientId(recipientId), PreferencesChange.replaceWith(
                RequestValues.channels(request.unsubscribedChannels()),
                RequestValues.categories(request.unsubscribedCategories()),
                quietHours(request),
                RequestValues.zone(request.timeZone()),
                RequestValues.locale(request.locale())));
    }

    @PatchMapping
    @Operation(summary = "Изменить только переданные поля")
    public PreferencesView update(@PathVariable String recipientId, @RequestBody PreferencesRequest request) {
        return managePreferences.change(new RecipientId(recipientId), new PreferencesChange(
                RequestValues.channels(request.unsubscribedChannels()),
                RequestValues.categories(request.unsubscribedCategories()),
                quietHours(request),
                Boolean.TRUE.equals(request.clearQuietHours()),
                RequestValues.zone(request.timeZone()),
                RequestValues.locale(request.locale()),
                Boolean.TRUE.equals(request.clearLocale())));
    }

    private static QuietHours quietHours(PreferencesRequest request) {
        if (request.quietHoursStart() == null && request.quietHoursEnd() == null) {
            return null;
        }
        if (request.quietHoursStart() == null || request.quietHoursEnd() == null) {
            throw DomainValidationException.invalidValue("quietHours", "both start and end are required");
        }
        return new QuietHours(request.quietHoursStart(), request.quietHoursEnd());
    }
}
