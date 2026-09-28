package kz.company.notification.infrastructure.in.messaging;

/** Команда отклонена бизнес-правилами (ошибка валидации, нет прав). Не повторяется: сразу в DLT. */
public class RejectedMessageException extends RuntimeException {

    private final String errorCode;

    public RejectedMessageException(String errorCode, String message, Throwable cause) {
        super(errorCode + ": " + message, cause);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}
