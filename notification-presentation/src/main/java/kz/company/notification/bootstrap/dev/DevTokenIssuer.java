package kz.company.notification.bootstrap.dev;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;

/** Выпуск тестовых JWT (только dev и тесты). */
public class DevTokenIssuer {

    public static final String ISSUER = "urn:notification-service:dev";

    private final JwtEncoder encoder;
    private final Clock clock;

    public DevTokenIssuer(JwtEncoder encoder, Clock clock) {
        this.encoder = encoder;
        this.clock = clock;
    }

    public String issue(String clientId, Collection<String> scopes, Duration ttl) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(clientId)
                .claim("client_id", clientId)
                .claim("scope", String.join(" ", scopes))
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims))
                .getTokenValue();
    }
}
