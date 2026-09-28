package kz.company.notification.domain.template;

import kz.company.notification.domain.common.DomainValidationException;

import java.util.regex.Pattern;

/** Ключ шаблона в kebab-case, например {@code order-shipped}. */
public record TemplateKey(String value) {

    private static final Pattern KEY = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    public static final int MAX_LENGTH = 100;

    public TemplateKey {
        if (value == null || value.length() > MAX_LENGTH || !KEY.matcher(value).matches()) {
            throw DomainValidationException.invalidValue("templateKey", "must be kebab-case, up to " + MAX_LENGTH + " characters");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
