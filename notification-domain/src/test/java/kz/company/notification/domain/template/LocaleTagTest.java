package kz.company.notification.domain.template;

import kz.company.notification.domain.common.DomainValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocaleTagTest {

    @Test
    void normalisesTag() {
        assertThat(LocaleTag.of("ru-kz").value()).isEqualTo("ru-KZ");
        assertThat(LocaleTag.of("ru-KZ").languageOnly()).isEqualTo(LocaleTag.of("ru"));
    }

    @Test
    void fallbackChainGoesFromRegionToLanguageToDefault() {
        assertThat(LocaleTag.of("ru-KZ").fallbackChain(LocaleTag.of("en")))
                .containsExactly(LocaleTag.of("ru-KZ"), LocaleTag.of("ru"), LocaleTag.of("en"));
        assertThat(LocaleTag.of("ru").fallbackChain(LocaleTag.of("ru")))
                .containsExactly(LocaleTag.of("ru"));
    }

    @Test
    void rejectsInvalidTag() {
        assertThatThrownBy(() -> LocaleTag.of(" ")).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> LocaleTag.of("123")).isInstanceOf(DomainValidationException.class);
    }
}
