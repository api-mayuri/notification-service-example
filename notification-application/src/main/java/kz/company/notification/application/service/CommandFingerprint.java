package kz.company.notification.application.service;

import kz.company.notification.application.port.in.SendNotificationCommand;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.notification.RawContent;
import kz.company.notification.domain.notification.TemplateContent;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Отпечаток команды для проверки идемпотентности: SHA-256 от канонического представления
 * (порядок ключей и каналов не влияет). Ключ идемпотентности и права отправителя в отпечаток
 * не входят.
 */
public final class CommandFingerprint {

    private CommandFingerprint() {
    }

    public static String of(SendNotificationCommand command) {
        StringBuilder canonical = new StringBuilder();
        canonical.append("recipient=").append(command.recipient().id().value()).append('\n');
        canonical.append("contacts=");
        appendValue(canonical, toStringKeys(command.recipient().contacts().values()));
        canonical.append('\n');
        switch (command.content()) {
            case TemplateContent template -> {
                canonical.append("template=").append(template.templateKey().value()).append('\n');
                canonical.append("locale=").append(template.locale() == null ? "" : template.locale().value()).append('\n');
                canonical.append("variables=");
                appendValue(canonical, template.variables());
            }
            case RawContent raw -> canonical.append("raw=").append(raw.subject()).append('\u0000')
                    .append(raw.text()).append('\u0000').append(raw.html());
        }
        canonical.append('\n');
        canonical.append("channels=").append(new TreeSet<>(command.channels().stream().map(ChannelType::code).toList()));
        canonical.append("\ncategory=").append(command.category());
        canonical.append("\npriority=").append(command.priority());
        canonical.append("\nsendAt=").append(command.sendAt());
        canonical.append("\nmetadata=");
        appendValue(canonical, command.metadata());
        return sha256(canonical.toString());
    }

    private static Map<String, Object> toStringKeys(Map<?, String> values) {
        Map<String, Object> result = new TreeMap<>();
        values.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    private static void appendValue(StringBuilder target, Object value) {
        switch (value) {
            case null -> target.append("null");
            case Map<?, ?> map -> {
                Map<String, Object> sorted = new TreeMap<>();
                map.forEach((key, item) -> sorted.put(String.valueOf(key), item));
                target.append('{');
                sorted.forEach((key, item) -> {
                    target.append(key).append('=');
                    appendValue(target, item);
                    target.append(';');
                });
                target.append('}');
            }
            case Collection<?> collection -> {
                target.append('[');
                collection.forEach(item -> {
                    appendValue(target, item);
                    target.append(',');
                });
                target.append(']');
            }
            default -> target.append(value.getClass().getSimpleName()).append(':').append(value);
        }
    }

    private static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
