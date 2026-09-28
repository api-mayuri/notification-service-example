package kz.company.notification.infrastructure.out.template;

import kz.company.notification.infrastructure.out.persistence.entity.TemplateRevisionEmbeddable;
import kz.company.notification.infrastructure.out.persistence.repository.TemplateJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Чтение исходного текста части шаблона из БД для {@link DatabaseTemplateResolver}. */
@Component
public class TemplateSourceLoader {

    private final TemplateJpaRepository templateRepository;

    public TemplateSourceLoader(TemplateJpaRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Transactional(readOnly = true)
    public String load(TemplatePart part) {
        TemplateRevisionEmbeddable revision = templateRepository.findById(part.templateId())
                .flatMap(template -> template.getRevisions().stream()
                        .filter(candidate -> candidate.getRevisionNumber() == part.version())
                        .findFirst())
                .orElseThrow(() -> new IllegalStateException("Template source not found: " + part.templateName()));
        String source = switch (part.part()) {
            case SUBJECT -> revision.getSubject();
            case BODY -> revision.getBody();
            case TEXT_BODY -> revision.getTextBody();
        };
        return source == null ? "" : source;
    }
}
