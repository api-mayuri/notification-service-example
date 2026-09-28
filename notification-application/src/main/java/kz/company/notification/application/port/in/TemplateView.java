package kz.company.notification.application.port.in;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Шаблон со всеми версиями. */
public record TemplateView(
        UUID id,
        String key,
        String channel,
        String locale,
        Integer activeVersion,
        Duration deduplicationWindow,
        List<Version> versions,
        Instant createdAt,
        Instant updatedAt) {

    public record Version(int version, String status, String subject, String body, String textBody,
                          Set<String> requiredVariables, Instant createdAt) {
    }
}
