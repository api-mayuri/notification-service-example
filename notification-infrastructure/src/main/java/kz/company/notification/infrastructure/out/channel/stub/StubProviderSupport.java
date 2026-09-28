package kz.company.notification.infrastructure.out.channel.stub;

import kz.company.notification.domain.delivery.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.function.DoubleSupplier;

/**
 * Общая логика логирующих заглушек (композиция, а не базовый класс): пишет в лог, что было бы
 * отправлено, и возвращает успех с флагом {@code simulated}; по настройке — случайные сбои.
 */
public final class StubProviderSupport {

    private static final Logger log = LoggerFactory.getLogger(StubProviderSupport.class);
    private static final int PREVIEW_LENGTH = 120;

    private final DoubleSupplier random;

    public StubProviderSupport(DoubleSupplier random) {
        this.random = random;
    }

    public static StubProviderSupport withSystemRandom() {
        return new StubProviderSupport(Math::random);
    }

    public SendResult simulate(String channel, UUID deliveryId, String maskedAddress, String text, StubBehaviour behaviour) {
        double roll = random.getAsDouble();
        if (roll < behaviour.permanentFailureProbability()) {
            log.warn("Stub {} provider simulated a permanent failure deliveryId={} to={}", channel, deliveryId, maskedAddress);
            return new SendResult.PermanentFailure("Simulated permanent failure of stub " + channel + " provider");
        }
        if (roll < behaviour.permanentFailureProbability() + behaviour.failureProbability()) {
            log.warn("Stub {} provider simulated a temporary failure deliveryId={} to={}", channel, deliveryId, maskedAddress);
            return new SendResult.RetryableFailure("Simulated temporary failure of stub " + channel + " provider");
        }
        String preview = text == null ? "" : text.length() <= PREVIEW_LENGTH ? text : text.substring(0, PREVIEW_LENGTH) + "…";
        log.info("Stub {} provider would send deliveryId={} to={} length={} preview=\"{}\"", channel, deliveryId,
                maskedAddress, text == null ? 0 : text.length(), preview.replace('\n', ' '));
        return SendResult.Success.simulated("stub-" + channel.toLowerCase(java.util.Locale.ROOT) + "-" + UUID.randomUUID());
    }
}
