package kz.company.notification.domain.template;

import kz.company.notification.domain.common.Guard;

/** Номер версии шаблона, начиная с 1. */
public record TemplateVersion(int value) implements Comparable<TemplateVersion> {

    public static final TemplateVersion FIRST = new TemplateVersion(1);

    public TemplateVersion {
        Guard.positive(value, "templateVersion");
    }

    public TemplateVersion next() {
        return new TemplateVersion(value + 1);
    }

    @Override
    public int compareTo(TemplateVersion other) {
        return Integer.compare(value, other.value);
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
