package kz.company.notification.bootstrap.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Брокер недоступен во время приёма: запрос всё равно принимается (уведомление и outbox —
 * в одной транзакции БД), а после возвращения брокера незавершённые публикации отправляются
 * повторно и уведомление доставляется.
 */
class OutboxResilienceIT extends IntegrationTestSupport {

    @Test
    void acceptedNotificationSurvivesBrokerOutage() throws Exception {
        KAFKA.getDockerClient().pauseContainerCmd(KAFKA.getContainerId()).exec();
        String id;
        try {
            String response = mockMvc.perform(post("/api/v1/notifications")
                            .header(HttpHeaders.AUTHORIZATION, bearer("it-outbox", "notifications:send"))
                            .header("Idempotency-Key", "it-" + UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "recipient": {"id": "user-outbox", "contacts": {"email": "outbox@example.com"}},
                                      "template": {"key": "welcome", "variables": {"name": "Ольга"}},
                                      "channels": ["EMAIL"],
                                      "category": "TRANSACTIONAL"
                                    }
                                    """))
                    .andExpect(status().isAccepted())
                    .andReturn().getResponse().getContentAsString();
            id = JsonPath.read(response, "$.id");
            Thread.sleep(5_000);
        } finally {
            KAFKA.getDockerClient().unpauseContainerCmd(KAFKA.getContainerId()).exec();
        }

        String notificationId = id;
        await().atMost(Duration.ofSeconds(90)).untilAsserted(() -> {
            String json = mockMvc.perform(get("/api/v1/notifications/" + notificationId)
                            .header(HttpHeaders.AUTHORIZATION, bearer("it-outbox", "notifications:read")))
                    .andReturn().getResponse().getContentAsString();
            assertThat((String) JsonPath.read(json, "$.status")).isEqualTo("COMPLETED");
        });
    }
}
