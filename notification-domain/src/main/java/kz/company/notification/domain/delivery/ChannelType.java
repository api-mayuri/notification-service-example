package kz.company.notification.domain.delivery;

import kz.company.notification.domain.common.DomainValidationException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Канал доставки. Открытый набор значений: новый канал добавляется новым адаптером
 * без изменения домена. Известные каналы вынесены в константы.
 */
public record ChannelType(String code) implements Comparable<ChannelType> {

    private static final Pattern CODE = Pattern.compile("[A-Z][A-Z0-9_]{1,31}");

    public static final ChannelType EMAIL = new ChannelType("EMAIL");
    public static final ChannelType SMS = new ChannelType("SMS");
    public static final ChannelType TELEGRAM = new ChannelType("TELEGRAM");
    public static final ChannelType WHATSAPP = new ChannelType("WHATSAPP");
    public static final ChannelType PUSH = new ChannelType("PUSH");
    public static final ChannelType WEBHOOK = new ChannelType("WEBHOOK");

    public ChannelType {
        if (code == null || !CODE.matcher(code).matches()) {
            throw DomainValidationException.invalidValue("channel", "must match " + CODE.pattern());
        }
    }

    /** Разбор без учёта регистра: {@code "email"} и {@code "EMAIL"} — один канал. */
    public static ChannelType of(String code) {
        if (code == null) {
            throw DomainValidationException.invalidValue("channel", "must not be null");
        }
        return new ChannelType(code.strip().toUpperCase(Locale.ROOT));
    }

    /** Код в нижнем регистре — для имён топиков и ключей настроек. */
    public String lowerCaseCode() {
        return code.toLowerCase(Locale.ROOT);
    }

    @Override
    public int compareTo(ChannelType other) {
        return code.compareTo(other.code);
    }

    @Override
    public String toString() {
        return code;
    }
}
