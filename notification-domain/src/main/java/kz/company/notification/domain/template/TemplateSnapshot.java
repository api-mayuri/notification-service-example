package kz.company.notification.domain.template;

import kz.company.notification.domain.delivery.ChannelType;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Полное состояние агрегата {@link Template} для сохранения и восстановления. */
public record TemplateSnapshot(
        TemplateId id,
        TemplateKey key,
        ChannelType channel,
        LocaleTag locale,
        Duration deduplicationWindow,
        List<TemplateRevision> revisions,
        Instant createdAt,
        Instant updatedAt,
        Long version) {
}
