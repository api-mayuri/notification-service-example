package kz.company.notification.application.port.in;

/** Результат обработки одного элемента пакета. */
public sealed interface BatchItemResult {

    int index();

    record Accepted(int index, SendNotificationResult result) implements BatchItemResult {
    }

    /**
     * Элемент отклонён.
     *
     * @param errorCode код доменной ошибки
     * @param kind      категория ошибки ({@code DomainErrorKind#name()}) — адаптер переводит её в код ответа
     */
    record Rejected(int index, String errorCode, String kind, String message) implements BatchItemResult {
    }
}
