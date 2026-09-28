package kz.company.notification.infrastructure.in.messaging;

/** Временная ошибка доставки: сообщение уходит в retry-топик с задержкой. */
public class RetryableDeliveryException extends RuntimeException {

    public RetryableDeliveryException(String message) {
        super(message, null, false, false);
    }
}
