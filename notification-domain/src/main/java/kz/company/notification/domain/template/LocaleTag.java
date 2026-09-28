package kz.company.notification.domain.template;

import kz.company.notification.domain.common.DomainValidationException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Языковой тег BCP 47, например {@code ru-KZ}, {@code kk}, {@code en}. */
public record LocaleTag(String value) {

    public LocaleTag {
        if (value == null || value.isBlank()) {
            throw DomainValidationException.invalidValue("locale", "must not be blank");
        }
        Locale locale = Locale.forLanguageTag(value.strip());
        if (locale.getLanguage().isEmpty()) {
            throw DomainValidationException.invalidValue("locale", "is not a valid BCP 47 language tag");
        }
        value = locale.toLanguageTag();
    }

    public static LocaleTag of(String value) {
        return new LocaleTag(value);
    }

    /** Тег только с языком: {@code ru-KZ} → {@code ru}. */
    public LocaleTag languageOnly() {
        return new LocaleTag(Locale.forLanguageTag(value).getLanguage());
    }

    public Locale toLocale() {
        return Locale.forLanguageTag(value);
    }

    /**
     * Цепочка поиска шаблона: {@code ru-KZ → ru → локаль по умолчанию} (без повторов).
     */
    public List<LocaleTag> fallbackChain(LocaleTag defaultLocale) {
        Set<LocaleTag> chain = new LinkedHashSet<>();
        chain.add(this);
        chain.add(languageOnly());
        chain.add(defaultLocale);
        chain.add(defaultLocale.languageOnly());
        return new ArrayList<>(chain);
    }

    @Override
    public String toString() {
        return value;
    }
}
