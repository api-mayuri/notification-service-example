package kz.company.notification.bootstrap.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The prod profile must reject tokens that the IdP issued for other services. */
class ProdJwtAudienceTest {

    private HttpServer idp;
    private RSAKey key;
    private String issuer;

    @BeforeEach
    void startIdp() throws Exception {
        key = new RSAKeyGenerator(2048).keyID("test").generate();
        idp = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        issuer = "http://localhost:" + idp.getAddress().getPort();
        respond("/.well-known/openid-configuration",
                "{\"issuer\":\"" + issuer + "\",\"jwks_uri\":\"" + issuer + "/jwks\"}");
        respond("/jwks", new JWKSet(key.toPublicJWK()).toString());
        idp.start();
    }

    @AfterEach
    void stopIdp() {
        idp.stop(0);
    }

    @Test
    void acceptsOnlyTokensForThisService() {
        runner().run(context -> {
            JwtDecoder decoder = context.getBean(JwtDecoder.class);

            assertThat(decoder.decode(token(List.of("notification-service"))).getSubject()).isEqualTo("orders-service");
            assertThatThrownBy(() -> decoder.decode(token(List.of("billing-service"))))
                    .isInstanceOf(JwtValidationException.class);
            assertThatThrownBy(() -> decoder.decode(token(List.of())))
                    .isInstanceOf(JwtValidationException.class);
        });
    }

    @Test
    void audienceIsConfigurable() {
        runner().withPropertyValues("JWT_AUDIENCE=api://notifications").run(context -> {
            JwtDecoder decoder = context.getBean(JwtDecoder.class);

            assertThat(decoder.decode(token(List.of("api://notifications"))).getSubject()).isEqualTo("orders-service");
            assertThatThrownBy(() -> decoder.decode(token(List.of("notification-service"))))
                    .isInstanceOf(JwtValidationException.class);
        });
    }

    private WebApplicationContextRunner runner() {
        return new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(OAuth2ResourceServerAutoConfiguration.class))
                .withInitializer(context -> prodProperties().forEach(context.getEnvironment().getPropertySources()::addLast))
                .withPropertyValues("JWT_ISSUER_URI=" + issuer);
    }

    private static List<PropertySource<?>> prodProperties() {
        try {
            return new YamlPropertySourceLoader().load("prod", new ClassPathResource("application-prod.yml"));
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String token(List<String> audience) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("orders-service")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300));
        if (!audience.isEmpty()) {
            claims.audience(audience);
        }
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)))
                .encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims.build()))
                .getTokenValue();
    }

    private void respond(String path, String json) {
        idp.createContext(path, exchange -> {
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
    }
}
