package kz.company.notification.infrastructure.out.channel.webhook;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import kz.company.notification.domain.delivery.SendResult;
import kz.company.notification.domain.delivery.WebhookUrl;
import kz.company.notification.infrastructure.out.channel.stub.StubBehaviour;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

class HttpWebhookProviderTest {

    private static final Instant NOW = Instant.parse("2026-09-25T10:00:00Z");

    private WireMockServer server;
    private HttpWebhookProvider provider;

    @BeforeEach
    void start() {
        server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        server.start();
        WebhookChannelProperties properties = new WebhookChannelProperties(true, "http", 5, "secret", false,
                Duration.ofSeconds(2), Duration.ofSeconds(2), 0, 10, StubBehaviour.RELIABLE);
        provider = new HttpWebhookProvider(new WebhookHttpClient(properties), properties, JsonMapper.builder().build(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @AfterEach
    void stop() {
        server.stop();
    }

    private WebhookMessage message(UUID deliveryId) {
        return new WebhookMessage(deliveryId, UUID.randomUUID(), "user-1",
                new WebhookUrl("http://localhost:" + server.port() + "/hook"), "subject", "text", null);
    }

    @Test
    void successfulCallIsSignedAndIdempotent() {
        server.stubFor(post(urlEqualTo("/hook")).willReturn(aResponse().withStatus(204).withHeader("X-Request-Id", "req-1")));
        UUID deliveryId = UUID.randomUUID();

        SendResult result = provider.send(message(deliveryId));

        assertThat(result).isEqualTo(SendResult.Success.real("req-1"));
        server.verify(postRequestedFor(urlEqualTo("/hook"))
                .withHeader("Idempotency-Key", equalTo(deliveryId.toString()))
                .withHeader(WebhookSignature.HEADER, matching("t=" + NOW.getEpochSecond() + ",v1=[0-9a-f]{64}"))
                .withHeader("Content-Type", matching("application/json.*")));
    }

    @Test
    void serverErrorsAndThrottlingAreRetryable() {
        server.stubFor(post(urlEqualTo("/hook")).willReturn(aResponse().withStatus(503)));
        assertThat(provider.send(message(UUID.randomUUID()))).isInstanceOf(SendResult.RetryableFailure.class);

        server.resetAll();
        server.stubFor(post(urlEqualTo("/hook")).willReturn(aResponse().withStatus(429)));
        assertThat(provider.send(message(UUID.randomUUID()))).isInstanceOf(SendResult.RetryableFailure.class);
    }

    @Test
    void clientErrorsArePermanent() {
        server.stubFor(post(urlEqualTo("/hook")).willReturn(aResponse().withStatus(410)));

        assertThat(provider.send(message(UUID.randomUUID()))).isInstanceOf(SendResult.PermanentFailure.class);
    }

    @Test
    void httpsCanBeRequired() {
        WebhookChannelProperties strict = new WebhookChannelProperties(true, "http", 5, "secret", true,
                Duration.ofSeconds(1), Duration.ofSeconds(1), 0, 10, StubBehaviour.RELIABLE);
        HttpWebhookProvider strictProvider = new HttpWebhookProvider(new WebhookHttpClient(strict), strict,
                JsonMapper.builder().build(), Clock.systemUTC());

        assertThat(strictProvider.send(message(UUID.randomUUID()))).isInstanceOf(SendResult.PermanentFailure.class);
    }

    @Test
    void signatureIsHmacOfTimestampAndBody() {
        assertThat(WebhookSignature.hmac("key", "The quick brown fox jumps over the lazy dog"))
                .isEqualTo("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8");
        assertThat(WebhookSignature.header("key", 10, "{}")).startsWith("t=10,v1=");
    }
}
