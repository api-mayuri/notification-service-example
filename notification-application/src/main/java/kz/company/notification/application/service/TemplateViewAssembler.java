package kz.company.notification.application.service;

import kz.company.notification.application.port.in.TemplateView;
import kz.company.notification.domain.template.Template;
import kz.company.notification.domain.template.TemplateRevision;

final class TemplateViewAssembler {

    private TemplateViewAssembler() {
    }

    static TemplateView toView(Template template) {
        return new TemplateView(
                template.id().value(),
                template.key().value(),
                template.channel().code(),
                template.locale().value(),
                template.activeRevision().map(revision -> revision.version().value()).orElse(null),
                template.deduplicationWindow().orElse(null),
                template.revisions().stream().map(TemplateViewAssembler::toView).toList(),
                template.createdAt(),
                template.updatedAt());
    }

    private static TemplateView.Version toView(TemplateRevision revision) {
        return new TemplateView.Version(revision.version().value(), revision.status().name(),
                revision.content().subject(), revision.content().body(), revision.content().textBody(),
                revision.content().requiredVariables(), revision.createdAt());
    }
}
