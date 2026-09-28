package kz.company.notification.infrastructure.out.channel.webhook;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.Map;

/**
 * HTTP-клиент вебхуков на {@link RestClient}. Короткие повторы при сетевых ошибках внутри одной
 * попытки — {@code @Retryable} из Spring Framework 7; длинные повторы делает очередь доставок.
 * Редиректы не выполняются: адрес вебхука должен быть точным.
 */
@Component
@ConditionalOnProperty(prefix = "notification.channels.webhook", name = "provider", havingValue = "http", matchIfMissing = true)
public class WebhookHttpClient {

    private final RestClient restClient;

    public WebhookHttpClient(WebhookChannelProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    /** POST JSON. Ответ с любым HTTP-кодом возвращается как есть; сетевая ошибка — исключение. */
    @Retryable(includes = ResourceAccessException.class,
            maxRetriesString = "${notification.channels.webhook.network-retries:2}",
            delayString = "${notification.channels.webhook.network-retry-delay:200}",
            multiplier = 2)
    public WebhookResponse post(URI uri, Map<String, String> headers, String jsonBody) {
        return restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                .body(jsonBody)
                .exchange((request, response) -> new WebhookResponse(response.getStatusCode().value(),
                        response.getHeaders().getFirst("X-Request-Id")));
    }

    /** Код ответа и идентификатор запроса у получателя (если он его вернул). */
    public record WebhookResponse(int statusCode, String requestId) {
    }
}
