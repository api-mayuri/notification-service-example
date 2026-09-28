package kz.company.notification.domain.delivery;

import kz.company.notification.domain.common.Guard;

/**
 * Результат одной попытки отправки у провайдера канала.
 */
public sealed interface SendResult {

    /**
     * Провайдер принял сообщение.
     *
     * @param providerMessageId идентификатор сообщения у провайдера
     * @param simulated          {@code true}, если отправку выполнила заглушка и реальной доставки не было
     */
    record Success(String providerMessageId, boolean simulated) implements SendResult {

        public static Success real(String providerMessageId) {
            return new Success(providerMessageId, false);
        }

        public static Success simulated(String providerMessageId) {
            return new Success(providerMessageId, true);
        }
    }

    /** Временная ошибка: сеть, таймаут, 4xx SMTP, 5xx/429 HTTP. Повторяется по {@code RetryPolicy}. */
    record RetryableFailure(String reason) implements SendResult {
        public RetryableFailure {
            Guard.notBlank(reason, "reason");
        }
    }

    /** Постоянная ошибка: неверный адрес, заблокированный бот, 5xx SMTP. Не повторяется. */
    record PermanentFailure(String reason) implements SendResult {
        public PermanentFailure {
            Guard.notBlank(reason, "reason");
        }
    }
}
