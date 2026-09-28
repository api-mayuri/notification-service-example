package kz.company.notification.domain.delivery;

import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.common.Masking;

import java.util.regex.Pattern;

/** Идентификатор чата Telegram: число (может быть отрицательным для групп) или {@code @username} канала. */
public record TelegramChatId(String value) implements ChannelAddress {

    private static final Pattern CHAT_ID = Pattern.compile("^(-?\\d{1,20}|@[A-Za-z][A-Za-z0-9_]{4,31})$");

    public TelegramChatId {
        if (value == null || !CHAT_ID.matcher(value.strip()).matches()) {
            throw DomainValidationException.invalidValue("telegramChatId", "must be a numeric chat id or @username");
        }
        value = value.strip();
    }

    @Override
    public String masked() {
        return Masking.keepEdges(value, 2);
    }
}
