package kz.company.notification.infrastructure.in.rest;

import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.notification.Category;
import kz.company.notification.domain.template.LocaleTag;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Разбор значений из параметров и тел запросов с доменными ошибками валидации (422). */
final class RequestValues {

    private RequestValues() {
    }

    static <E extends Enum<E>> E enumValue(Class<E> type, String value, String field) {
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw DomainValidationException.invalidValue(field, "unknown value '" + value + "'");
        }
    }

    static ChannelType channel(String value) {
        return value == null ? null : ChannelType.of(value);
    }

    static LocaleTag locale(String value) {
        return value == null || value.isBlank() ? null : LocaleTag.of(value);
    }

    static ZoneId zone(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ZoneId.of(value.strip());
        } catch (DateTimeException exception) {
            throw DomainValidationException.invalidValue("timeZone", "unknown IANA time zone '" + value + "'");
        }
    }

    static Set<ChannelType> channels(Set<String> values) {
        if (values == null) {
            return null;
        }
        Set<ChannelType> channels = new LinkedHashSet<>();
        values.forEach(value -> channels.add(ChannelType.of(value)));
        return channels;
    }

    static Set<Category> categories(Set<String> values) {
        if (values == null) {
            return null;
        }
        Set<Category> categories = new LinkedHashSet<>();
        values.forEach(value -> categories.add(enumValue(Category.class, value, "unsubscribedCategories")));
        return categories;
    }
}
