package kz.company.notification.infrastructure.in.messaging;

/** Сообщение нельзя разобрать или оно нарушает контракт. Не повторяется: сразу в DLT. */
public class InvalidMessageException extends RuntimeException {

    public InvalidMessageException(String message) {
        super(message);
    }

    public InvalidMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
