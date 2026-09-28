package kz.company.notification.infrastructure.in.messaging;

/** Доставка окончательно не удалась (статус FAILED): сообщение сразу уходит в DLT (parking lot). */
public class DeliveryFailedException extends RuntimeException {

    public DeliveryFailedException(String message) {
        super(message, null, false, false);
    }
}
