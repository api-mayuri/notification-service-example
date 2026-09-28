package kz.company.notification.domain.delivery;

import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.common.Masking;

import java.util.regex.Pattern;

/** Номер телефона в формате E.164: {@code +} и от 7 до 15 цифр, первая цифра не ноль. */
public record PhoneNumber(String value) implements ChannelAddress {

    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{6,14}$");

    public PhoneNumber {
        if (value == null || !E164.matcher(value.strip()).matches()) {
            throw DomainValidationException.invalidValue("phone", "must be in E.164 format, e.g. +77011234567");
        }
        value = value.strip();
    }

    @Override
    public String masked() {
        return Masking.keepEdges(value, 3);
    }
}
