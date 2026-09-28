package kz.company.notification.infrastructure.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/** Ответ 207 Multi-Status: результат по каждому элементу пакета. */
@Schema(name = "NotificationBatchResponse")
public record BatchResponse(int accepted, int rejected, List<Item> items) {

    /**
     * @param status HTTP-код, который получил бы элемент при отдельной отправке (202, 403, 404, 409, 422)
     */
    public record Item(int index, int status, UUID notificationId, String notificationStatus, String outcome,
                       String errorCode, String detail) {
    }
}
