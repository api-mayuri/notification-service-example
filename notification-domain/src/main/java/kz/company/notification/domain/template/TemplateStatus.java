package kz.company.notification.domain.template;

/** Статус версии шаблона. У шаблона не больше одной активной версии. */
public enum TemplateStatus {
    DRAFT,
    ACTIVE,
    ARCHIVED
}
