package kz.company.notification.application.exception;

import kz.company.notification.domain.common.DomainErrorKind;
import kz.company.notification.domain.common.DomainException;

/** Готовое содержимое без шаблона разрешено только доверенным отправителям. */
public class RawContentNotAllowedException extends DomainException {

    public RawContentNotAllowedException() {
        super("RAW_CONTENT_NOT_ALLOWED", DomainErrorKind.FORBIDDEN,
                "Raw content requires scope notifications:send-raw or a trusted message source");
    }
}
