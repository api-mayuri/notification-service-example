package kz.company.notification.application.port.in;

import java.time.Instant;

/** Итог обработки сообщения о доставке воркером канала. */
public sealed interface DeliveryOutcome {

    /** Провайдер принял сообщение. */
    record Sent(String providerMessageId, boolean simulated) implements DeliveryOutcome {
    }

    /** Временная ошибка, повтор запланирован. */
    record RetryScheduled(Instant nextAttemptAt, String reason) implements DeliveryOutcome {
    }

    /** Доставка завершилась ошибкой. {@code permanent = false} — исчерпаны попытки. */
    record Failed(String reason, boolean permanent) implements DeliveryOutcome {
    }

    /** Отправка отложена (лимит частоты); доставку позже поставит планировщик. */
    record Deferred(Instant until) implements DeliveryOutcome {
    }

    /** Сообщение не требует обработки: доставка уже завершена, в работе, отменена или не найдена. */
    record Ignored(String reason) implements DeliveryOutcome {
    }
}
