package kz.company.notification.infrastructure.out.recipient;

import kz.company.notification.application.port.out.RecipientResolver;
import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.notification.ContactType;
import kz.company.notification.domain.notification.RecipientContacts;
import kz.company.notification.domain.notification.RecipientId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Контакты получателя из сервиса пользователей.
 *
 * <p>Контракт: {@code GET {baseUrl}/api/v1/recipients/{recipientId}/contacts} →
 * {@code 200 {"email": "...", "phone": "+7...", "telegramChatId": "...", "deviceToken": "...", "webhookUrl": "..."}}
 * (ключи — типы контактов, лишние игнорируются), {@code 404} — получатель неизвестен.
 * Остальные ответы и сетевые ошибки — исключение: запрос на уведомление не принимается,
 * клиент повторяет его с тем же ключом идемпотентности.
 */
@Component
@ConditionalOnExpression("!'${notification.recipient-directory.base-url:}'.isEmpty()")
public class HttpRecipientResolver implements RecipientResolver {

    private static final ParameterizedTypeReference<Map<String, String>> CONTACTS = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;

    public HttpRecipientResolver(RecipientDirectoryProperties properties) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build());
        requestFactory.setReadTimeout(properties.readTimeout());
        RestClient.Builder builder = RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(requestFactory);
        if (properties.bearerToken() != null && !properties.bearerToken().isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.bearerToken());
        }
        this.restClient = builder.build();
    }

    @Override
    public RecipientContacts resolve(RecipientId recipientId) {
        return restClient.get()
                .uri("/api/v1/recipients/{recipientId}/contacts", recipientId.value())
                .exchange((request, response) -> {
                    int status = response.getStatusCode().value();
                    if (status == 404) {
                        return RecipientContacts.NONE;
                    }
                    if (status != 200) {
                        throw new RecipientDirectoryUnavailableException("Recipient directory responded with HTTP " + status);
                    }
                    Map<String, String> body = response.bodyTo(CONTACTS);
                    return toContacts(body == null ? Map.of() : body);
                });
    }

    private static RecipientContacts toContacts(Map<String, String> body) {
        Map<ContactType, String> contacts = new LinkedHashMap<>();
        body.forEach((type, value) -> {
            try {
                contacts.put(new ContactType(type), value);
            } catch (DomainValidationException unknownType) {
                // Неизвестный тип контакта пропускается: справочник может знать больше каналов, чем сервис.
            }
        });
        return new RecipientContacts(contacts);
    }

    /** Сервис пользователей недоступен: временная ошибка. */
    public static class RecipientDirectoryUnavailableException extends RuntimeException {
        public RecipientDirectoryUnavailableException(String message) {
            super(message);
        }
    }
}
