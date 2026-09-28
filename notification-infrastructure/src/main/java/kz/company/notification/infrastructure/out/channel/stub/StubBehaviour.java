package kz.company.notification.infrastructure.out.channel.stub;

import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Поведение логирующей заглушки провайдера: вероятность случайных сбоев, чтобы проверять
 * повторы и dead letter topics без реального провайдера.
 *
 * @param failureProbability          вероятность временной ошибки (0..1)
 * @param permanentFailureProbability вероятность постоянной ошибки (0..1)
 */
public record StubBehaviour(@DefaultValue("0.0") double failureProbability,
                            @DefaultValue("0.0") double permanentFailureProbability) {

    public static final StubBehaviour RELIABLE = new StubBehaviour(0.0, 0.0);

    public StubBehaviour {
        if (failureProbability < 0 || failureProbability > 1 || permanentFailureProbability < 0
                || permanentFailureProbability > 1 || failureProbability + permanentFailureProbability > 1) {
            throw new IllegalArgumentException("Stub failure probabilities must be within [0, 1] and sum to at most 1");
        }
    }
}
