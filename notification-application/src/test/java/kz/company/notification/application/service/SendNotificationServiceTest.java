package kz.company.notification.application.service;

import kz.company.notification.application.ApplicationTestContext;
import kz.company.notification.application.exception.ChannelNotAvailableException;
import kz.company.notification.application.exception.IdempotencyKeyReusedException;
import kz.company.notification.application.exception.RawContentNotAllowedException;
import kz.company.notification.application.port.in.SendNotificationCommand;
import kz.company.notification.application.port.in.SendNotificationResult;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.Delivery;
import kz.company.notification.domain.delivery.DeliveryStatus;
import kz.company.notification.domain.delivery.SkipReason;
import kz.company.notification.domain.delivery.event.DeliveryScheduled;
import kz.company.notification.domain.delivery.event.DeliverySkipped;
import kz.company.notification.domain.notification.Category;
import kz.company.notification.domain.notification.ContactType;
import kz.company.notification.domain.notification.NotificationStatus;
import kz.company.notification.domain.notification.RawContent;
import kz.company.notification.domain.notification.RecipientContacts;
import kz.company.notification.domain.notification.TemplateContent;
import kz.company.notification.domain.notification.event.NotificationAccepted;
import kz.company.notification.domain.notification.event.NotificationCompleted;
import kz.company.notification.domain.policy.NoDeliverableChannelException;
import kz.company.notification.domain.preferences.PreferencesChange;
import kz.company.notification.domain.preferences.QuietHours;
import kz.company.notification.domain.template.ActiveTemplateNotFoundException;
import kz.company.notification.domain.template.LocaleTag;
import kz.company.notification.domain.template.MissingTemplateVariablesException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;

import static kz.company.notification.application.ApplicationTestContext.ORDER_SHIPPED;
import static kz.company.notification.application.ApplicationTestContext.RECIPIENT;
import static kz.company.notification.application.ApplicationTestContext.command;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SendNotificationServiceTest {

    private final ApplicationTestContext context = new ApplicationTestContext();

    @Test
    void acceptsTemplateNotificationAndDispatchesImmediately() {
        context.givenActiveTemplate(ChannelType.EMAIL, "ru", null);
        context.givenActiveTemplate(ChannelType.SMS, "ru", null);

        SendNotificationResult result = context.sendService.send(
                command("key-1", Category.TRANSACTIONAL, ChannelType.EMAIL, ChannelType.SMS));

        assertThat(result.outcome()).isEqualTo(SendNotificationResult.Outcome.CREATED);
        assertThat(result.status()).isEqualTo(NotificationStatus.ACCEPTED);
        assertThat(context.deliveries.all()).hasSize(2);
        assertThat(context.dispatcher.dispatched()).hasSize(2);
        Delivery email = context.deliveries.all().stream()
                .filter(delivery -> delivery.channel().equals(ChannelType.EMAIL)).findFirst().orElseThrow();
        assertThat(email.message().orElseThrow().htmlBody()).isEqualTo("Заказ ORD-42 отправлен");
        assertThat(email.message().orElseThrow().templateVersion()).isEqualTo(1);
        assertThat(email.dispatchedAt()).isPresent();
        assertThat(context.events.eventsOf(NotificationAccepted.class)).hasSize(1);
        assertThat(context.events.eventsOf(DeliveryScheduled.class)).hasSize(2);
        assertThat(context.recipientResolver.calls()).isEqualTo(0);
    }

    @Test
    void repeatedKeyWithSameBodyReturnsOriginal() {
        context.givenActiveTemplate(ChannelType.EMAIL, "ru", null);
        SendNotificationResult first = context.sendService.send(command("key-1", Category.TRANSACTIONAL, ChannelType.EMAIL));

        SendNotificationResult second = context.sendService.send(command("key-1", Category.TRANSACTIONAL, ChannelType.EMAIL));

        assertThat(second.outcome()).isEqualTo(SendNotificationResult.Outcome.REPLAYED);
        assertThat(second.notificationId()).isEqualTo(first.notificationId());
        assertThat(context.notifications.all()).hasSize(1);
        assertThat(context.deliveries.all()).hasSize(1);
    }

    @Test
    void repeatedKeyWithDifferentBodyIsRejected() {
        context.givenActiveTemplate(ChannelType.EMAIL, "ru", null);
        context.sendService.send(command("key-1", Category.TRANSACTIONAL, ChannelType.EMAIL));

        assertThatThrownBy(() -> context.sendService.send(command("key-1", Category.MARKETING, ChannelType.EMAIL)))
                .isInstanceOf(IdempotencyKeyReusedException.class);
    }

    @Test
    void rawContentRequiresPermission() {
        SendNotificationCommand raw = command("key-raw", Category.SYSTEM, new RawContent("s", "text", null),
                ApplicationTestContext.contacts(), ChannelType.EMAIL);

        assertThatThrownBy(() -> context.sendService.send(raw)).isInstanceOf(RawContentNotAllowedException.class);

        SendNotificationCommand allowed = new SendNotificationCommand(raw.clientId(), raw.idempotencyKey(),
                raw.recipient(), raw.content(), raw.channels(), raw.category(), raw.priority(), raw.sendAt(),
                raw.metadata(), true);
        assertThat(context.sendService.send(allowed).outcome()).isEqualTo(SendNotificationResult.Outcome.CREATED);
        assertThat(context.deliveries.all().getFirst().message().orElseThrow().textBody()).isEqualTo("text");
    }

    @Test
    void disabledChannelIsRejected() {
        context.registry.unregister(ChannelType.SMS);

        assertThatThrownBy(() -> context.sendService.send(command("k", Category.TRANSACTIONAL, ChannelType.SMS)))
                .isInstanceOf(ChannelNotAvailableException.class)
                .hasMessageContaining("SMS");
    }

    @Test
    void missingVariablesAreRejectedAtAcceptance() {
        context.givenActiveTemplate(ChannelType.EMAIL, "ru", null);
        SendNotificationCommand withoutVariables = command("k", Category.TRANSACTIONAL,
                new TemplateContent(ORDER_SHIPPED, null, Map.of()), ApplicationTestContext.contacts(), ChannelType.EMAIL);

        assertThatThrownBy(() -> context.sendService.send(withoutVariables))
                .isInstanceOf(MissingTemplateVariablesException.class);
        assertThat(context.notifications.all()).isEmpty();
    }

    @Test
    void missingTemplateIsRejected() {
        assertThatThrownBy(() -> context.sendService.send(command("k", Category.TRANSACTIONAL, ChannelType.EMAIL)))
                .isInstanceOf(ActiveTemplateNotFoundException.class);
    }

    @Test
    void localeFallsBackFromRegionToLanguage() {
        context.givenActiveTemplate(ChannelType.EMAIL, "ru", null);
        SendNotificationCommand kazakhRussian = command("k", Category.TRANSACTIONAL,
                new TemplateContent(ORDER_SHIPPED, LocaleTag.of("ru-KZ"), Map.of("orderId", "1")),
                ApplicationTestContext.contacts(), ChannelType.EMAIL);

        context.sendService.send(kazakhRussian);

        assertThat(context.deliveries.all().getFirst().message().orElseThrow().templateLocale()).isEqualTo("ru");
    }

    @Test
    void preferenceLocaleIsUsedWhenRequestHasNone() {
        context.givenActiveTemplate(ChannelType.EMAIL, "ru", null);
        context.givenActiveTemplate(ChannelType.EMAIL, "kk", null);
        context.preferencesService.change(RECIPIENT, new PreferencesChange(null, null, null, false, null,
                LocaleTag.of("kk"), false));

        context.sendService.send(command("k", Category.TRANSACTIONAL, ChannelType.EMAIL));

        assertThat(context.deliveries.all().getFirst().message().orElseThrow().templateLocale()).isEqualTo("kk");
    }

    @Test
    void futureSendAtWaitsForScheduler() {
        context.givenActiveTemplate(ChannelType.EMAIL, "ru", null);
        SendNotificationCommand base = command("k", Category.TRANSACTIONAL, ChannelType.EMAIL);
        Instant later = context.clock.instant().plus(Duration.ofHours(2));
        SendNotificationCommand scheduled = new SendNotificationCommand(base.clientId(), base.idempotencyKey(),
                base.recipient(), base.content(), base.channels(), base.category(), base.priority(), later,
                base.metadata(), false);

        context.sendService.send(scheduled);

        assertThat(context.dispatcher.dispatched()).isEmpty();
        assertThat(context.deliveries.all().getFirst().nextAttemptAt()).contains(later);
    }

    @Test
    void marketingDuringQuietHoursIsPostponed() {
        context.givenActiveTemplate(ChannelType.EMAIL, "ru", null);
        context.preferencesService.change(RECIPIENT, new PreferencesChange(null, null,
                new QuietHours(LocalTime.of(10, 0), LocalTime.of(12, 0)), false, ZoneId.of("Asia/Almaty"), null, false));

        context.sendService.send(command("k", Category.MARKETING, ChannelType.EMAIL)); // 11:00 в Алматы

        assertThat(context.dispatcher.dispatched()).isEmpty();
        assertThat(context.deliveries.all().getFirst().nextAttemptAt())
                .contains(Instant.parse("2026-09-25T07:00:00Z"));
    }

    @Test
    void unsubscribedMarketingIsSkippedAndNotificationCompleted() {
        context.preferencesService.change(RECIPIENT, new PreferencesChange(null, Set.of(Category.MARKETING), null,
                false, null, null, false));

        SendNotificationResult result = context.sendService.send(command("k", Category.MARKETING, ChannelType.EMAIL));

        assertThat(result.status()).isEqualTo(NotificationStatus.COMPLETED);
        Delivery delivery = context.deliveries.all().getFirst();
        assertThat(delivery.status()).isEqualTo(DeliveryStatus.SKIPPED);
        assertThat(delivery.skipReason()).contains(SkipReason.UNSUBSCRIBED_CATEGORY);
        assertThat(context.events.eventsOf(DeliverySkipped.class)).hasSize(1);
        assertThat(context.events.eventsOf(NotificationCompleted.class)).hasSize(1);
    }

    @Test
    void missingContactsAreResolvedFromDirectory() {
        context.givenActiveTemplate(ChannelType.SMS, "ru", null);
        context.recipientResolver.put(RECIPIENT, new RecipientContacts(Map.of(ContactType.PHONE, "+77010000000")));

        context.sendService.send(command("k", Category.TRANSACTIONAL,
                new TemplateContent(ORDER_SHIPPED, null, Map.of("orderId", "1")),
                new RecipientContacts(Map.of(ContactType.EMAIL, "user@example.com")), ChannelType.SMS));

        assertThat(context.recipientResolver.calls()).isEqualTo(1);
        assertThat(context.deliveries.all().getFirst().address().orElseThrow().value()).isEqualTo("+77010000000");
    }

    @Test
    void noContactsAtAllIsRejected() {
        assertThatThrownBy(() -> context.sendService.send(command("k", Category.TRANSACTIONAL,
                new TemplateContent(ORDER_SHIPPED, null, Map.of("orderId", "1")), RecipientContacts.NONE,
                ChannelType.SMS)))
                .isInstanceOf(NoDeliverableChannelException.class);
    }

    @Test
    void identicalNotificationsInsideWindowAreDeduplicated() {
        context.givenActiveTemplate(ChannelType.EMAIL, "ru", Duration.ofMinutes(10));
        SendNotificationResult first = context.sendService.send(command("key-1", Category.MARKETING, ChannelType.EMAIL));
        context.clock.advance(Duration.ofMinutes(5));

        SendNotificationResult second = context.sendService.send(command("key-2", Category.MARKETING, ChannelType.EMAIL));
        context.clock.advance(Duration.ofMinutes(6));
        SendNotificationResult third = context.sendService.send(command("key-3", Category.MARKETING, ChannelType.EMAIL));

        assertThat(second.outcome()).isEqualTo(SendNotificationResult.Outcome.DEDUPLICATED);
        assertThat(second.notificationId()).isEqualTo(first.notificationId());
        assertThat(third.outcome()).isEqualTo(SendNotificationResult.Outcome.CREATED);
    }
}
