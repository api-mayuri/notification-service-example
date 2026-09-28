package kz.company.notification.application.settings;

import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.common.Guard;
import kz.company.notification.domain.notification.Category;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;

/**
 * Лимит частоты: не больше {@code maxDeliveries} отправок получателю по каналу за {@code window}.
 * Доставка сверх лимита не отклоняется, а откладывается до освобождения окна.
 *
 * @param categories категории, к которым применяется лимит (по умолчанию только MARKETING,
 *                   чтобы не задерживать сброс пароля и другие транзакционные сообщения)
 */
public record RateLimit(int maxDeliveries, Duration window, Set<Category> categories) {

    public RateLimit {
        Guard.positive(maxDeliveries, "maxDeliveries");
        Guard.notNull(window, "window");
        if (window.isNegative() || window.isZero()) {
            throw DomainValidationException.invalidValue("window", "must be positive");
        }
        categories = categories == null || categories.isEmpty()
                ? Set.of(Category.MARKETING)
                : Set.copyOf(EnumSet.copyOf(categories));
    }

    public boolean appliesTo(Category category) {
        return categories.contains(category);
    }
}
