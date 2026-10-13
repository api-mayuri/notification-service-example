package kz.company.notification.bootstrap.config;

import kz.company.notification.application.settings.DeliverySettings;
import kz.company.notification.application.settings.RateLimit;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.notification.Category;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UseCaseConfigurationTest {

    @Test
    void rateLimitWithoutCategoriesAppliesToMarketingOnly() {
        RateLimit limit = rateLimit(null);

        assertThat(limit.appliesTo(Category.MARKETING)).isTrue();
        assertThat(limit.appliesTo(Category.TRANSACTIONAL)).isFalse();
        assertThat(limit.appliesTo(Category.SYSTEM)).isFalse();
    }

    @Test
    void explicitRateLimitCategoriesAreRespected() {
        RateLimit limit = rateLimit(Set.of("transactional"));

        assertThat(limit.appliesTo(Category.TRANSACTIONAL)).isTrue();
        assertThat(limit.appliesTo(Category.MARKETING)).isFalse();
    }

    private static RateLimit rateLimit(Set<String> categories) {
        NotificationProperties properties = new NotificationProperties("ru",
                new NotificationProperties.Retry(5, Duration.ofSeconds(30), 2.0, Duration.ofMinutes(30), 0.2),
                new NotificationProperties.Delivery(Duration.ofMinutes(5), Duration.ofMinutes(10), 200, Duration.ofDays(7),
                        Map.of("sms", new NotificationProperties.RateLimitProperties(5, Duration.ofHours(1), categories))));
        DeliverySettings settings = new UseCaseConfiguration().deliverySettings(properties);
        return settings.rateLimitFor(ChannelType.SMS).orElseThrow();
    }
}
