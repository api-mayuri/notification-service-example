package kz.company.notification.bootstrap.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Описание API для springdoc (OpenAPI 3.1). Swagger UI отключается в prod. */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    @Bean
    OpenAPI notificationOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Notification Service API")
                        .version("v1")
                        .description("""
                                Приём уведомлений, шаблоны, предпочтения получателей и статистика.
                                Ошибки — RFC 9457 Problem Details с полем errorCode.
                                POST-запросы на отправку требуют заголовок Idempotency-Key.
                                Асинхронный контракт (Kafka) — docs/asyncapi.yaml."""))
                .components(new Components().addSecuritySchemes("bearer-jwt", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("""
                                OAuth2 access token (JWT). Scopes:
                                notifications:send, notifications:send-raw, notifications:read,
                                templates:manage, preferences:manage.""")));
    }
}
