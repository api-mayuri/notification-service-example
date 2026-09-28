package kz.company.notification.application.port.in;

import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.common.Guard;
import kz.company.notification.domain.notification.IdempotencyKey;

import java.util.List;

/**
 * Пакет уведомлений. Каждый элемент обрабатывается отдельно (своя транзакция).
 * Ключ идемпотентности элемента: его собственный или ключ пакета + номер элемента.
 */
public record SendBatchCommand(IdempotencyKey batchKey, List<SendNotificationCommand> items) {

    public static final int MAX_ITEMS = 500;

    public SendBatchCommand {
        Guard.notEmpty(items, "items");
        if (items.size() > MAX_ITEMS) {
            throw new DomainValidationException("BATCH_TOO_LARGE", "Batch must contain at most " + MAX_ITEMS + " items");
        }
        items = List.copyOf(items);
    }
}
