package kz.company.notification.domain.delivery;

import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.common.Masking;

import java.util.regex.Pattern;

/**
 * Адрес электронной почты. Проверяется упрощённая форма addr-spec из RFC 5322
 * (без комментариев и quoted-string), длина — не больше 254 символов (RFC 5321).
 */
public record EmailAddress(String value) implements ChannelAddress {

    private static final int MAX_LENGTH = 254;
    private static final Pattern ADDR_SPEC = Pattern.compile(
            "^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*"
                    + "@[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?(\\.[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$");

    public EmailAddress {
        if (value == null || value.length() > MAX_LENGTH || !ADDR_SPEC.matcher(value.strip()).matches()) {
            throw DomainValidationException.invalidValue("email", "is not a valid RFC 5322 address");
        }
        value = value.strip();
    }

    @Override
    public String masked() {
        return Masking.email(value);
    }
}
