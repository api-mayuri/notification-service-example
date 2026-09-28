package kz.company.notification.infrastructure.in.rest;

import kz.company.notification.domain.notification.ClientId;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;

/**
 * Отправитель из JWT: claim {@code client_id} (RFC 9068), иначе {@code azp}, иначе {@code sub}.
 */
public final class AuthenticatedClient {

    private AuthenticatedClient() {
    }

    public static ClientId clientId(Authentication authentication) {
        return new ClientId(clientIdValue(authentication)
                .orElseThrow(() -> new IllegalStateException("Authenticated principal has no client id")));
    }

    public static Optional<String> clientIdValue(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken token) {
            Jwt jwt = token.getToken();
            return firstPresent(jwt.getClaimAsString("client_id"), jwt.getClaimAsString("azp"), jwt.getSubject());
        }
        return authentication == null ? Optional.empty() : Optional.ofNullable(authentication.getName());
    }

    public static boolean hasScope(Authentication authentication, String scope) {
        String authority = ApiScopes.authority(scope);
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }

    private static Optional<String> firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
