package kz.company.notification.application.port.in;

import java.util.List;

/** Принять пакет уведомлений; ответ — результат по каждому элементу. */
public interface SendBatchUseCase {

    List<BatchItemResult> sendBatch(SendBatchCommand command);
}
