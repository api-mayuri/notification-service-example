package kz.company.notification.domain.template;

import kz.company.notification.domain.common.Guard;

import java.util.UUID;

/** Идентификатор шаблона (комбинации ключ + канал + локаль). */
public record TemplateId(UUID value) {

    public TemplateId {
        Guard.notNull(value, "templateId");
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
