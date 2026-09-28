package kz.company.notification.domain.delivery;

import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.common.Masking;

import java.util.regex.Pattern;

/** Токен устройства для push-уведомлений (FCM, APNs): 16–4096 печатных символов без пробелов. */
public record DeviceToken(String value) implements ChannelAddress {

    private static final Pattern TOKEN = Pattern.compile("^[\\x21-\\x7E]{16,4096}$");

    public DeviceToken {
        if (value == null || !TOKEN.matcher(value.strip()).matches()) {
            throw DomainValidationException.invalidValue("deviceToken", "must be 16-4096 printable characters");
        }
        value = value.strip();
    }

    @Override
    public String masked() {
        return Masking.keepEdges(value, 4);
    }
}
