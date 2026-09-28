package kz.company.notification.domain.notification;

import kz.company.notification.domain.common.DomainValidationException;

import java.util.regex.Pattern;

/**
 * Тип контакта получателя. Открытый набор: новый канал может ввести свой тип контакта
 * без изменения домена. Известные типы вынесены в константы.
 */
public record ContactType(String code) {

    private static final Pattern CODE = Pattern.compile("[a-z][A-Za-z0-9]{1,39}");

    public static final ContactType EMAIL = new ContactType("email");
    public static final ContactType PHONE = new ContactType("phone");
    public static final ContactType TELEGRAM_CHAT_ID = new ContactType("telegramChatId");
    public static final ContactType DEVICE_TOKEN = new ContactType("deviceToken");
    public static final ContactType WEBHOOK_URL = new ContactType("webhookUrl");

    public ContactType {
        if (code == null || !CODE.matcher(code).matches()) {
            throw DomainValidationException.invalidValue("contactType", "must be a camelCase identifier");
        }
    }

    @Override
    public String toString() {
        return code;
    }
}
