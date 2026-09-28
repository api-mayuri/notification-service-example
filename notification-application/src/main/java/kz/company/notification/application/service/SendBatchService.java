package kz.company.notification.application.service;

import kz.company.notification.application.port.in.BatchItemResult;
import kz.company.notification.application.port.in.SendBatchCommand;
import kz.company.notification.application.port.in.SendBatchUseCase;
import kz.company.notification.application.port.in.SendNotificationCommand;
import kz.company.notification.application.port.in.SendNotificationUseCase;
import kz.company.notification.domain.common.DomainException;
import kz.company.notification.domain.notification.IdempotencyKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Пакетная отправка: каждый элемент — отдельный вызов {@link SendNotificationUseCase}
 * в своей транзакции. Ошибка бизнес-правил отклоняет только свой элемент. Инфраструктурная
 * ошибка прерывает пакет; повтор с тем же ключом пакета безопасен — принятые элементы
 * вернутся как повторы.
 */
public class SendBatchService implements SendBatchUseCase {

    private final SendNotificationUseCase sendNotificationUseCase;

    public SendBatchService(SendNotificationUseCase sendNotificationUseCase) {
        this.sendNotificationUseCase = sendNotificationUseCase;
    }

    @Override
    public List<BatchItemResult> sendBatch(SendBatchCommand command) {
        List<BatchItemResult> results = new ArrayList<>(command.items().size());
        for (int index = 0; index < command.items().size(); index++) {
            SendNotificationCommand item = command.items().get(index);
            IdempotencyKey key = item.idempotencyKey() != null
                    ? item.idempotencyKey()
                    : command.batchKey() == null ? null : command.batchKey().forBatchItem(index);
            try {
                results.add(new BatchItemResult.Accepted(index, sendNotificationUseCase.send(item.withIdempotencyKey(key))));
            } catch (DomainException exception) {
                results.add(new BatchItemResult.Rejected(index, exception.errorCode(), exception.kind().name(),
                        exception.getMessage()));
            }
        }
        return results;
    }
}
