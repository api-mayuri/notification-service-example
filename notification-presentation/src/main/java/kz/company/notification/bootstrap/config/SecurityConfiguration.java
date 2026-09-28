package kz.company.notification.bootstrap.config;

import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import kz.company.notification.infrastructure.in.rest.ApiScopes;
import kz.company.notification.infrastructure.in.rest.ClientRateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

/**
 * OAuth2 Resource Server (JWT). Права — scopes токена: {@code notifications:send},
 * {@code notifications:send-raw}, {@code notifications:read}, {@code templates:manage},
 * {@code preferences:manage}. Issuer задаётся {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, RateLimiterRegistry rateLimiterRegistry,
                                               JsonMapper jsonMapper) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/dev/token").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/notifications", "/api/v1/notifications/**")
                        .hasAuthority(ApiScopes.authority(ApiScopes.SEND))
                        .requestMatchers(HttpMethod.GET, "/api/v1/notifications", "/api/v1/notifications/**",
                                "/api/v1/statistics").hasAuthority(ApiScopes.authority(ApiScopes.READ))
                        .requestMatchers("/api/v1/templates", "/api/v1/templates/**")
                        .hasAuthority(ApiScopes.authority(ApiScopes.TEMPLATES))
                        .requestMatchers("/api/v1/recipients/*/preferences")
                        .hasAuthority(ApiScopes.authority(ApiScopes.PREFERENCES))
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()))
                .addFilterAfter(new ClientRateLimitFilter(rateLimiterRegistry, jsonMapper),
                        BearerTokenAuthenticationFilter.class);
        return http.build();
    }
}
