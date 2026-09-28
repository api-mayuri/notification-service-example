package kz.company.notification.infrastructure.out.channel.webhook;

import kz.company.notification.domain.delivery.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Вызов вебхука: POST JSON с подписью HMAC-SHA256 и ключом идемпотентности (id доставки).
 */
@Component
@ConditionalOnProperty(prefix = "notification.channels.webhook", name = "provider", havingValue = "http", matchIfMissing = true)
public class HttpWebhookProvider implements WebhookProvider {

    private static final Logger log = LoggerFactory.getLogger(HttpWebhookProvider.class);
    private static final Set<Integer> RETRYABLE_CLIENT_ERRORS = Set.of(408, 425, 429);

    private final WebhookHttpClient httpClient;
    private final WebhookChannelProperties properties;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    public HttpWebhookProvider(WebhookHttpClient httpClient, WebhookChannelProperties properties,
                               JsonMapper jsonMapper, Clock clock) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    @Override
    public SendResult send(WebhookMessage message) {
        if (properties.requireHttps() && !message.url().isHttps()) {
            return new SendResult.PermanentFailure("Webhook URL must use https");
        }
        long timestamp = clock.instant().getEpochSecond();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("deliveryId", message.deliveryId());
        payload.put("notificationId", message.notificationId());
        payload.put("recipientId", message.recipientId());
        payload.put("subject", message.subject());
        payload.put("text", message.text());
        payload.put("html", message.html());
        payload.put("timestamp", timestamp);
        String body = jsonMapper.writeValueAsString(payload);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Idempotency-Key", message.deliveryId().toString());
        headers.put("X-Notification-Delivery-Id", message.deliveryId().toString());
        if (properties.signingSecret() != null && !properties.signingSecret().isBlank()) {
            headers.put(WebhookSignature.HEADER, WebhookSignature.header(properties.signingSecret(), timestamp, body));
        } else {
            log.warn("Webhook signing secret is not configured; sending unsigned request deliveryId={}", message.deliveryId());
        }
        try {
            WebhookHttpClient.WebhookResponse response = httpClient.post(message.url().toUri(), headers, body);
            return classify(response, message);
        } catch (ResourceAccessException networkError) {
            return new SendResult.RetryableFailure("Webhook network error: " + networkError.getMessage());
        }
    }

    private static SendResult classify(WebhookHttpClient.WebhookResponse response, WebhookMessage message) {
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return SendResult.Success.real(response.requestId() != null ? response.requestId() : message.deliveryId().toString());
        }
        if (status >= 500 || RETRYABLE_CLIENT_ERRORS.contains(status)) {
            return new SendResult.RetryableFailure("Webhook responded with HTTP " + status);
        }
        return new SendResult.PermanentFailure("Webhook rejected the request with HTTP " + status);
    }
}
