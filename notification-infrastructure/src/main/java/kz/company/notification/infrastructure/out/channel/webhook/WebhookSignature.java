package kz.company.notification.infrastructure.out.channel.webhook;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

/**
 * Подпись тела вебхука: {@code HMAC-SHA256(secret, timestamp + "." + body)} в hex.
 * Заголовок: {@code X-Notification-Signature: t=<unix seconds>,v1=<hex>}. Получатель пересчитывает
 * подпись и отклоняет запросы со старой меткой времени (защита от повторов).
 */
public final class WebhookSignature {

    public static final String HEADER = "X-Notification-Signature";

    private WebhookSignature() {
    }

    public static String header(String secret, long timestampSeconds, String body) {
        return "t=" + timestampSeconds + ",v1=" + hmac(secret, timestampSeconds + "." + body);
    }

    static String hmac(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HmacSHA256 is not available", exception);
        }
    }
}
