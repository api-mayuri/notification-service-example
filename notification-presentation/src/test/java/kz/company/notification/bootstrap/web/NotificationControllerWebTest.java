package kz.company.notification.bootstrap.web;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import kz.company.notification.application.port.in.BatchItemResult;
import kz.company.notification.application.port.in.CancelNotificationUseCase;
import kz.company.notification.application.port.in.GetNotificationQuery;
import kz.company.notification.application.port.in.SearchNotificationsQuery;
import kz.company.notification.application.port.in.SendBatchUseCase;
import kz.company.notification.application.port.in.SendNotificationResult;
import kz.company.notification.application.port.in.SendNotificationUseCase;
import kz.company.notification.application.exception.IdempotencyKeyReusedException;
import kz.company.notification.bootstrap.config.SecurityConfiguration;
import kz.company.notification.domain.notification.IdempotencyKey;
import kz.company.notification.domain.notification.NotificationId;
import kz.company.notification.domain.notification.NotificationNotCancellableException;
import kz.company.notification.domain.notification.NotificationStatus;
import kz.company.notification.infrastructure.in.common.NotificationCommandMapper;
import kz.company.notification.infrastructure.in.rest.NotificationController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Контракт REST API уведомлений: коды ответов, заголовки, Problem Details, scopes. */
@WebMvcTest(NotificationController.class)
@Import({SecurityConfiguration.class, NotificationCommandMapper.class, NotificationControllerWebTest.TestBeans.class})
class NotificationControllerWebTest {

    private static final String BODY = """
            {
              "recipient": {"id": "user-1", "contacts": {"email": "user@example.com"}},
              "template": {"key": "order-shipped", "locale": "ru-KZ", "variables": {"orderId": "ORD-42"}},
              "channels": ["EMAIL"],
              "category": "TRANSACTIONAL"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SendNotificationUseCase sendNotificationUseCase;
    @MockitoBean
    private SendBatchUseCase sendBatchUseCase;
    @MockitoBean
    private CancelNotificationUseCase cancelNotificationUseCase;
    @MockitoBean
    private GetNotificationQuery getNotificationQuery;
    @MockitoBean
    private SearchNotificationsQuery searchNotificationsQuery;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @TestConfiguration
    static class TestBeans {
        @Bean
        RateLimiterRegistry rateLimiterRegistry() {
            return RateLimiterRegistry.of(java.util.Map.of("clients", RateLimiterConfig.custom()
                    .limitForPeriod(1000).limitRefreshPeriod(Duration.ofSeconds(1)).timeoutDuration(Duration.ZERO).build()));
        }
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor client(String... scopes) {
        return jwt().jwt(token -> token.claim("client_id", "orders-service"))
                .authorities(java.util.Arrays.stream(scopes)
                        .map(scope -> new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_" + scope))
                        .toList());
    }

    @Test
    void acceptsNotificationWithLocation() throws Exception {
        UUID id = UUID.randomUUID();
        when(sendNotificationUseCase.send(any())).thenReturn(new SendNotificationResult(new NotificationId(id),
                NotificationStatus.ACCEPTED, SendNotificationResult.Outcome.CREATED));

        mockMvc.perform(post("/api/v1/notifications").with(client("notifications:send"))
                        .header("Idempotency-Key", "key-1").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/notifications/" + id)))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.outcome").value("CREATED"));
    }

    @Test
    void replayIsMarkedWithHeader() throws Exception {
        when(sendNotificationUseCase.send(any())).thenReturn(new SendNotificationResult(new NotificationId(UUID.randomUUID()),
                NotificationStatus.COMPLETED, SendNotificationResult.Outcome.REPLAYED));

        mockMvc.perform(post("/api/v1/notifications").with(client("notifications:send"))
                        .header("Idempotency-Key", "key-1").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Idempotent-Replayed", "true"));
    }

    @Test
    void missingIdempotencyKeyIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/notifications").with(client("notifications:send"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("REQUEST_INVALID"));
    }

    @Test
    void invalidBodyIsBadRequestWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/notifications").with(client("notifications:send"))
                        .header("Idempotency-Key", "key-1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channels\": [], \"category\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void domainErrorsBecomeProblemDetails() throws Exception {
        when(sendNotificationUseCase.send(any())).thenThrow(new IdempotencyKeyReusedException(new IdempotencyKey("key-1")));

        mockMvc.perform(post("/api/v1/notifications").with(client("notifications:send"))
                        .header("Idempotency-Key", "key-1").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().is(422))
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/problem+json")))
                .andExpect(jsonPath("$.errorCode").value("IDEMPOTENCY_KEY_REUSED"))
                .andExpect(jsonPath("$.type").value("urn:problem-type:notification:idempotency-key-reused"));
    }

    @Test
    void cancelConflictIs409() throws Exception {
        UUID id = UUID.randomUUID();
        when(cancelNotificationUseCase.cancel(any(), any()))
                .thenThrow(new NotificationNotCancellableException(new NotificationId(id), NotificationStatus.COMPLETED));

        mockMvc.perform(post("/api/v1/notifications/" + id + "/cancel").with(client("notifications:send")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NOTIFICATION_NOT_CANCELLABLE"));
    }

    @Test
    void batchReturnsMultiStatus() throws Exception {
        when(sendBatchUseCase.sendBatch(any())).thenReturn(List.of(
                new BatchItemResult.Accepted(0, new SendNotificationResult(new NotificationId(UUID.randomUUID()),
                        NotificationStatus.ACCEPTED, SendNotificationResult.Outcome.CREATED)),
                new BatchItemResult.Rejected(1, "TEMPLATE_NOT_AVAILABLE", "VALIDATION", "no template")));

        mockMvc.perform(post("/api/v1/notifications/batch").with(client("notifications:send"))
                        .header("Idempotency-Key", "batch-1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\": [" + BODY + "," + BODY + "]}"))
                .andExpect(status().is(207))
                .andExpect(jsonPath("$.accepted").value(1))
                .andExpect(jsonPath("$.items[0].status").value(202))
                .andExpect(jsonPath("$.items[1].status").value(422))
                .andExpect(jsonPath("$.items[1].errorCode").value("TEMPLATE_NOT_AVAILABLE"));
    }

    @Test
    void scopesAreEnforced() throws Exception {
        mockMvc.perform(post("/api/v1/notifications").with(client("notifications:read"))
                        .header("Idempotency-Key", "key-1").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/notifications/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}
