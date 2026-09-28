package kz.company.notification.bootstrap.dev;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/** {@code POST /dev/token} — тестовый токен для локального запуска (см. README). */
@Hidden
@RestController
@ConditionalOnProperty(prefix = "notification.security.dev-tokens", name = "enabled", havingValue = "true")
public class DevTokenController {

    private static final List<String> ALL_SCOPES = List.of("notifications:send", "notifications:send-raw",
            "notifications:read", "templates:manage", "preferences:manage");

    private final DevTokenIssuer issuer;

    public DevTokenController(DevTokenIssuer issuer) {
        this.issuer = issuer;
    }

    @PostMapping("/dev/token")
    public Map<String, Object> token(@RequestBody(required = false) TokenRequest request) {
        String clientId = request == null || request.clientId() == null ? "local-client" : request.clientId();
        List<String> scopes = request == null || request.scopes() == null ? ALL_SCOPES : request.scopes();
        Duration ttl = Duration.ofHours(8);
        return Map.of("access_token", issuer.issue(clientId, scopes, ttl), "token_type", "Bearer",
                "expires_in", ttl.toSeconds(), "scope", String.join(" ", scopes));
    }

    public record TokenRequest(String clientId, List<String> scopes) {
    }
}
