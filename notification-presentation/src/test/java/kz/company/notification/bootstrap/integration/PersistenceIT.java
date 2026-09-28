package kz.company.notification.bootstrap.integration;

import kz.company.notification.application.port.in.StatisticsCriteria;
import kz.company.notification.application.port.out.StatisticsReadModel;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.Delivery;
import kz.company.notification.domain.delivery.DeliveryFactory;
import kz.company.notification.domain.delivery.DeliveryId;
import kz.company.notification.domain.delivery.DeliveryPlan;
import kz.company.notification.domain.delivery.DeliveryRepository;
import kz.company.notification.domain.delivery.EmailAddress;
import kz.company.notification.domain.delivery.RenderedMessage;
import kz.company.notification.domain.notification.Category;
import kz.company.notification.domain.notification.ClientId;
import kz.company.notification.domain.notification.Notification;
import kz.company.notification.domain.notification.NotificationId;
import kz.company.notification.domain.notification.NotificationRepository;
import kz.company.notification.domain.notification.NotificationRequest;
import kz.company.notification.domain.notification.RawContent;
import kz.company.notification.domain.notification.Recipient;
import kz.company.notification.domain.notification.RecipientId;
import kz.company.notification.domain.template.LocaleTag;
import kz.company.notification.domain.template.TemplateKey;
import kz.company.notification.domain.template.TemplateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** JPA-адаптеры на реальном PostgreSQL со схемой из Liquibase. Планировщик выключен. */
@TestPropertySource(properties = "notification.scheduler.enabled=false")
class PersistenceIT extends IntegrationTestSupport {

    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private DeliveryRepository deliveryRepository;
    @Autowired
    private TemplateRepository templateRepository;
    @Autowired
    private StatisticsReadModel statisticsReadModel;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private Delivery persistDueDelivery(String clientId, Instant now) {
        Notification notification = Notification.accept(new NotificationId(UUID.randomUUID()),
                new NotificationRequest(new ClientId(clientId), null, null,
                        new Recipient(new RecipientId("user-" + UUID.randomUUID()), null),
                        new RawContent("s", "t", null), Set.of(ChannelType.EMAIL), Category.SYSTEM, null, null, null),
                null, now);
        notificationRepository.save(notification);
        Delivery delivery = new DeliveryFactory(() -> new DeliveryId(UUID.randomUUID())).create(notification,
                List.of(new DeliveryPlan.Deliverable(ChannelType.EMAIL, new EmailAddress("user@example.com"),
                        RenderedMessage.raw("s", "t", null), now.minusSeconds(1))), now).getFirst();
        return deliveryRepository.save(delivery);
    }

    @Test
    void seededTemplatesAreAvailable() {
        assertThat(templateRepository.find(new TemplateKey("welcome"), ChannelType.EMAIL, LocaleTag.of("ru")))
                .hasValueSatisfying(template -> assertThat(template.activeRevision()).isPresent());
        assertThat(templateRepository.findByKey(new TemplateKey("order-shipped")).size()).isGreaterThanOrEqualTo(6);
    }

    @Test
    void lockedDeliveriesAreSkippedByOtherInstances() throws Exception {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        Delivery due = transactionTemplate.execute(status -> persistDueDelivery("it-lock", now));
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        CompletableFuture<List<Delivery>> firstInstance = CompletableFuture.supplyAsync(() ->
                transactionTemplate.execute(status -> {
                    List<Delivery> rows = deliveryRepository.lockDueForDispatch(Instant.now(), 1000);
                    locked.countDown();
                    try {
                        release.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                    }
                    return rows;
                }));
        assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
        List<Delivery> secondInstance = transactionTemplate.execute(status ->
                deliveryRepository.lockDueForDispatch(Instant.now(), 1000));
        release.countDown();

        assertThat(firstInstance.get(10, TimeUnit.SECONDS)).extracting(Delivery::id).contains(due.id());
        assertThat(secondInstance).extracting(Delivery::id).doesNotContain(due.id());
    }

    @Test
    void statisticsAreGroupedByChannelAndStatus() {
        Instant now = Instant.now();
        transactionTemplate.executeWithoutResult(status -> persistDueDelivery("it-stats", now));

        var rows = statisticsReadModel.countDeliveries(new StatisticsCriteria(new ClientId("it-stats"),
                now.minus(1, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS), ChannelType.EMAIL,
                StatisticsCriteria.Granularity.HOUR));

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().channel()).isEqualTo("EMAIL");
        assertThat(rows.getFirst().status()).isEqualTo("PENDING");
        assertThat(rows.getFirst().count()).isEqualTo(1);
    }
}
