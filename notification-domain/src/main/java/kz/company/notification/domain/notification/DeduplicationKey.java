package kz.company.notification.domain.notification;

import kz.company.notification.domain.common.Guard;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.template.TemplateKey;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Ключ дедупликации: SHA-256 от получателя, шаблона, переменных и каналов. Одинаковые
 * уведомления одному получателю в окне дедупликации шаблона схлопываются в одно.
 */
public record DeduplicationKey(String value) {

    public DeduplicationKey {
        Guard.notBlank(value, "deduplicationKey");
    }

    public static DeduplicationKey of(RecipientId recipientId, TemplateKey templateKey,
                                      Map<String, Object> variables, Set<ChannelType> channels) {
        StringBuilder canonical = new StringBuilder()
                .append(recipientId.value()).append('|')
                .append(templateKey.value()).append('|');
        new TreeMap<>(variables).forEach((name, value) -> canonical.append(name).append('=').append(value).append(';'));
        canonical.append('|');
        new TreeSet<>(channels.stream().map(ChannelType::code).toList()).forEach(code -> canonical.append(code).append(','));
        return new DeduplicationKey(sha256(canonical.toString()));
    }

    static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
