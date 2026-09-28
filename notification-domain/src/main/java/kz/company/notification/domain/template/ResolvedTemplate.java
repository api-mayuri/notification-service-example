package kz.company.notification.domain.template;

/** Шаблон и версия, выбранные для рендера. */
public record ResolvedTemplate(Template template, TemplateRevision revision) {
}
