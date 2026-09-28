package kz.company.notification.infrastructure.out.persistence.mapper;

import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.template.LocaleTag;
import kz.company.notification.domain.template.Template;
import kz.company.notification.domain.template.TemplateContentDefinition;
import kz.company.notification.domain.template.TemplateId;
import kz.company.notification.domain.template.TemplateKey;
import kz.company.notification.domain.template.TemplateRevision;
import kz.company.notification.domain.template.TemplateSnapshot;
import kz.company.notification.domain.template.TemplateStatus;
import kz.company.notification.domain.template.TemplateVersion;
import kz.company.notification.infrastructure.out.persistence.entity.TemplateJpaEntity;
import kz.company.notification.infrastructure.out.persistence.entity.TemplateRevisionEmbeddable;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Маппинг {@link Template} ↔ {@link TemplateJpaEntity}. */
@Component
public class TemplateMapper {

    public TemplateJpaEntity toEntity(Template template) {
        TemplateSnapshot snapshot = template.snapshot();
        TemplateJpaEntity entity = new TemplateJpaEntity();
        entity.setId(snapshot.id().value());
        entity.setTemplateKey(snapshot.key().value());
        entity.setChannel(snapshot.channel().code());
        entity.setLocale(snapshot.locale().value());
        entity.setDeduplicationWindowSeconds(snapshot.deduplicationWindow() == null
                ? null : snapshot.deduplicationWindow().toSeconds());
        List<TemplateRevisionEmbeddable> revisions = new ArrayList<>();
        for (TemplateRevision revision : snapshot.revisions()) {
            TemplateRevisionEmbeddable row = new TemplateRevisionEmbeddable();
            row.setRevisionNumber(revision.version().value());
            row.setSubject(revision.content().subject());
            row.setBody(revision.content().body());
            row.setTextBody(revision.content().textBody());
            row.setRequiredVariables(DelimitedText.join(revision.content().requiredVariables(), Function.identity()));
            row.setStatus(revision.status().name());
            row.setCreatedAt(revision.createdAt());
            revisions.add(row);
        }
        entity.setRevisions(revisions);
        entity.setCreatedAt(snapshot.createdAt());
        entity.setUpdatedAt(snapshot.updatedAt());
        entity.setVersion(snapshot.version());
        return entity;
    }

    public Template toDomain(TemplateJpaEntity entity) {
        List<TemplateRevision> revisions = entity.getRevisions().stream()
                .map(row -> new TemplateRevision(
                        new TemplateVersion(row.getRevisionNumber()),
                        new TemplateContentDefinition(row.getSubject(), row.getBody(), row.getTextBody(),
                                DelimitedText.split(row.getRequiredVariables(), Function.identity())),
                        TemplateStatus.valueOf(row.getStatus()),
                        row.getCreatedAt()))
                .toList();
        return Template.restore(new TemplateSnapshot(
                new TemplateId(entity.getId()),
                new TemplateKey(entity.getTemplateKey()),
                ChannelType.of(entity.getChannel()),
                LocaleTag.of(entity.getLocale()),
                entity.getDeduplicationWindowSeconds() == null ? null : Duration.ofSeconds(entity.getDeduplicationWindowSeconds()),
                revisions,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()));
    }
}
