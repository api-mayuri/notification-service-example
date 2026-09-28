package kz.company.notification.bootstrap.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Провайдер SMS всегда отвечает временной ошибкой: доставка уходит в повторы с backoff через
 * retry-топики, после исчерпания попыток — в parking lot (DLT) со статусом FAILED.
 */
@TestPropertySource(properties = "notification.channels.sms.stub.failure-probability=1.0")
class DeliveryFailureIT extends IntegrationTestSupport {

    @Test
    void exhaustedRetriesEndInDeadLetterTopicWithFailedStatus() throws Exception {
        String recipient = "user-failing-" + UUID.randomUUID();
        String response = mockMvc.perform(post("/api/v1/notifications")
                        .header(HttpHeaders.AUTHORIZATION, bearer("it-failing", "notifications:send"))
                        .header("Idempotency-Key", "it-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipient": {"id": "%s", "contacts": {"phone": "+77011234567"}},
                                  "template": {"key": "order-shipped", "variables": {"orderId": "ORD-9"}},
                                  "channels": ["SMS"],
                                  "category": "TRANSACTIONAL"
                                }
                                """.formatted(recipient)))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(response, "$.id");

        await().atMost(Duration.ofSeconds(60)).untilAsserted(() -> {
            String json = mockMvc.perform(get("/api/v1/notifications/" + id)
                            .header(HttpHeaders.AUTHORIZATION, bearer("it-failing", "notifications:read")))
                    .andReturn().getResponse().getContentAsString();
            assertThat((String) JsonPath.read(json, "$.status")).isEqualTo("FAILED");
            assertThat((Integer) JsonPath.read(json, "$.deliveries[0].attempts")).isEqualTo(3);
        });
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(readAll("notification.delivery.sms.v1-dlt").stream()
                        .filter(record -> recipient.equals(record.key())).count()).isEqualTo(1));
    }
}
