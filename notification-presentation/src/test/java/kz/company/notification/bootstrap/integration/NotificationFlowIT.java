package kz.company.notification.bootstrap.integration;

import com.jayway.jsonpath.JsonPath;
import jakarta.mail.internet.MimeMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Полный путь: REST или Kafka → БД + outbox → топик канала → воркер → провайдер → событие статуса.
 */
class NotificationFlowIT extends IntegrationTestSupport {

    private static final String CLIENT = "it-orders";

    private String welcomeEmail(String recipientId, String email) {
        return """
                {
                  "recipient": {"id": "%s", "contacts": {"email": "%s"}},
                  "template": {"key": "welcome", "locale": "ru-KZ", "variables": {"name": "Анна"}},
                  "channels": ["EMAIL"],
                  "category": "TRANSACTIONAL"
                }
                """.formatted(recipientId, email);
    }

    private String send(String idempotencyKey, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/notifications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(CLIENT, "notifications:send"))
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private String status(String clientId, String notificationId) throws Exception {
        String json = mockMvc.perform(get("/api/v1/notifications/" + notificationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientId, "notifications:read")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.status");
    }

    @Test
    void emailSentThroughRestIsDeliveredAndStatusEventsArePublished() throws Exception {
        String email = "anna-" + UUID.randomUUID() + "@example.com";
        String id = send("it-" + UUID.randomUUID(), welcomeEmail("user-anna", email));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(status(CLIENT, id)).isEqualTo("COMPLETED"));

        MimeMessage delivered = Arrays.stream(GREEN_MAIL.getReceivedMessages())
                .filter(message -> {
                    try {
                        return message.getAllRecipients()[0].toString().equals(email);
                    } catch (Exception exception) {
                        return false;
                    }
                })
                .findFirst().orElseThrow();
        assertThat(delivered.getSubject()).isEqualTo("Добро пожаловать, Анна!");

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            List<ConsumerRecord<String, String>> events = readAll("notification.events.v1").stream()
                    .filter(record -> id.equals(record.key()))
                    .toList();
            assertThat(events.stream().map(record -> header(record, "ce_type")).toList())
                    .contains("kz.company.notification.delivery.sent.v1",
                            "kz.company.notification.notification.completed.v1");
            assertThat(header(events.getFirst(), "ce_specversion")).isEqualTo("1.0");
        });
    }

    @Test
    void repeatedIdempotencyKeyDoesNotCreateDuplicate() throws Exception {
        String key = "it-" + UUID.randomUUID();
        String body = welcomeEmail("user-idem", "idem@example.com");
        String first = send(key, body);

        MvcResult replay = mockMvc.perform(post("/api/v1/notifications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(CLIENT, "notifications:send"))
                        .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andReturn();
        assertThat((String) JsonPath.read(replay.getResponse().getContentAsString(), "$.id")).isEqualTo(first);

        mockMvc.perform(post("/api/v1/notifications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(CLIENT, "notifications:send"))
                        .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON)
                        .content(welcomeEmail("user-other", "other@example.com")))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.errorCode").value("IDEMPOTENCY_KEY_REUSED"));
    }

    @Test
    void kafkaRequestFollowsTheSamePath() throws Exception {
        String source = "urn:it:billing";
        String recipient = "user-kafka-" + UUID.randomUUID();
        publish("notification.send.requests.v1", recipient, Map.of(
                "ce_id", UUID.randomUUID().toString(),
                "ce_source", source,
                "ce_type", "kz.company.notification.send.v1",
                "ce_specversion", "1.0",
                "content-type", "application/json"), """
                {
                  "recipient": {"id": "%s", "contacts": {"phone": "+77011234567"}},
                  "template": {"key": "order-shipped", "variables": {"orderId": "ORD-7"}},
                  "channels": ["SMS"],
                  "category": "TRANSACTIONAL"
                }
                """.formatted(recipient));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            String json = mockMvc.perform(get("/api/v1/notifications").param("recipientId", recipient)
                            .header(HttpHeaders.AUTHORIZATION, bearer(source, "notifications:read")))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            List<String> statuses = JsonPath.read(json, "$.items[*].status");
            assertThat(statuses).containsExactly("COMPLETED");
        });
    }

    @Test
    void invalidKafkaMessageGoesToDeadLetterTopic() {
        String marker = "broken-" + UUID.randomUUID();
        publish("notification.send.requests.v1", marker, Map.of(
                "ce_id", marker, "ce_source", "urn:it:broken", "ce_type", "kz.company.notification.unknown.v1",
                "ce_specversion", "1.0"), "{}");

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(readAll("notification.send.requests.v1-dlt").stream()
                        .filter(record -> marker.equals(record.key())).count()).isEqualTo(1));
    }

    @Test
    void scheduledNotificationCanBeCancelledButSentCannot() throws Exception {
        String scheduled = """
                {
                  "recipient": {"id": "user-cancel", "contacts": {"email": "cancel@example.com"}},
                  "template": {"key": "welcome", "variables": {"name": "Иван"}},
                  "channels": ["EMAIL"],
                  "category": "TRANSACTIONAL",
                  "sendAt": "%s"
                }
                """.formatted(OffsetDateTime.now().plusHours(2));
        String id = send("it-" + UUID.randomUUID(), scheduled);

        mockMvc.perform(post("/api/v1/notifications/" + id + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, bearer(CLIENT, "notifications:send")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.deliveries[0].status").value("CANCELLED"));

        String sent = send("it-" + UUID.randomUUID(), welcomeEmail("user-sent", "sent@example.com"));
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(status(CLIENT, sent)).isEqualTo("COMPLETED"));
        mockMvc.perform(post("/api/v1/notifications/" + sent + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, bearer(CLIENT, "notifications:send")))
                .andExpect(status().isConflict());
    }

    @Test
    void validationAndSecurityErrors() throws Exception {
        mockMvc.perform(post("/api/v1/notifications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(CLIENT, "notifications:send"))
                        .header("Idempotency-Key", "it-" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipient": {"id": "user-x", "contacts": {"email": "x@example.com"}},
                                  "template": {"key": "welcome", "variables": {}},
                                  "channels": ["EMAIL"],
                                  "category": "TRANSACTIONAL"
                                }
                                """))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.errorCode").value("MISSING_TEMPLATE_VARIABLES"))
                .andExpect(jsonPath("$.missingVariables[0]").value("name"));

        mockMvc.perform(get("/api/v1/notifications/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer(CLIENT, "notifications:send")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/notifications/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void templatePreviewAndPreferences() throws Exception {
        mockMvc.perform(post("/api/v1/templates/order-shipped/preview")
                        .header(HttpHeaders.AUTHORIZATION, bearer(CLIENT, "templates:manage"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\": \"EMAIL\", \"locale\": \"ru\", \"variables\": {\"orderId\": \"<b>1</b>\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("Заказ <b>1</b> отправлен"))
                .andExpect(jsonPath("$.htmlBody").value(org.hamcrest.Matchers.containsString("&lt;b&gt;1&lt;/b&gt;")));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/recipients/user-pref/preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearer(CLIENT, "preferences:manage"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unsubscribedCategories\": [\"TRANSACTIONAL\"]}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_NOT_UNSUBSCRIBABLE"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/v1/recipients/user-pref/preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearer(CLIENT, "preferences:manage"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unsubscribedChannels\": [\"sms\"], \"timeZone\": \"Asia/Almaty\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unsubscribedChannels[0]").value("SMS"))
                .andExpect(jsonPath("$.timeZone").value("Asia/Almaty"));
    }
}
