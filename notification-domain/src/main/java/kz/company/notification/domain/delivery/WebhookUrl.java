package kz.company.notification.domain.delivery;

import kz.company.notification.domain.common.DomainValidationException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/**
 * Адрес вебхука: абсолютный URL со схемой http или https, без учётных данных в URL.
 * Требование https для prod задаётся настройкой адаптера, а не доменом.
 */
public record WebhookUrl(String value) implements ChannelAddress {

    private static final int MAX_LENGTH = 2048;
    private static final Set<String> SCHEMES = Set.of("http", "https");

    public WebhookUrl {
        if (value == null || value.isBlank() || value.length() > MAX_LENGTH) {
            throw DomainValidationException.invalidValue("webhookUrl", "must be a non-empty URL up to " + MAX_LENGTH + " characters");
        }
        value = value.strip();
        try {
            URI uri = new URI(value);
            if (uri.getScheme() == null || !SCHEMES.contains(uri.getScheme().toLowerCase(Locale.ROOT))
                    || uri.getHost() == null || uri.getUserInfo() != null) {
                throw DomainValidationException.invalidValue("webhookUrl", "must be an absolute http(s) URL without credentials");
            }
        } catch (URISyntaxException exception) {
            throw DomainValidationException.invalidValue("webhookUrl", "is not a valid URI");
        }
    }

    public URI toUri() {
        return URI.create(value);
    }

    public boolean isHttps() {
        return value.regionMatches(true, 0, "https:", 0, 6);
    }

    @Override
    public String masked() {
        URI uri = toUri();
        return uri.getScheme() + "://" + uri.getHost() + "/***";
    }
}
