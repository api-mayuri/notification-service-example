package kz.company.notification.bootstrap.dev;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * ТОЛЬКО ДЛЯ ЛОКАЛЬНОЙ РАЗРАБОТКИ И ТЕСТОВ: сервис сам выпускает и проверяет JWT, ключ создаётся
 * при старте. Включается {@code notification.security.dev-tokens.enabled=true} (профиль local).
 * В prod выключено: токены выдаёт корпоративный IdP, issuer задаётся в настройках.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "notification.security.dev-tokens", name = "enabled", havingValue = "true")
public class DevTokenConfiguration {

    @Bean
    KeyPair devTokenKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    @Bean
    JwtDecoder devJwtDecoder(KeyPair devTokenKeyPair) {
        return NimbusJwtDecoder.withPublicKey((RSAPublicKey) devTokenKeyPair.getPublic()).build();
    }

    @Bean
    JwtEncoder devJwtEncoder(KeyPair devTokenKeyPair) {
        RSAKey key = new RSAKey.Builder((RSAPublicKey) devTokenKeyPair.getPublic())
                .privateKey((RSAPrivateKey) devTokenKeyPair.getPrivate())
                .keyID("dev")
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }

    @Bean
    DevTokenIssuer devTokenIssuer(JwtEncoder devJwtEncoder, java.time.Clock clock) {
        return new DevTokenIssuer(devJwtEncoder, clock);
    }
}
