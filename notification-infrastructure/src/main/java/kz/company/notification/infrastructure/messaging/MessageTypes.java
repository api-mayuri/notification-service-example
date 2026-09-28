package kz.company.notification.infrastructure.messaging;

/** Значения {@code ce_type}: версия входит в тип, несовместимые изменения — через новую версию. */
public final class MessageTypes {

    public static final String SEND_REQUEST = "kz.company.notification.send.v1";
    public static final String DELIVERY_REQUESTED = "kz.company.notification.delivery.requested.v1";
    public static final String EVENT_PREFIX = "kz.company.notification.";
    public static final String EVENT_VERSION = ".v1";

    private MessageTypes() {
    }

    /** {@code delivery.sent} → {@code kz.company.notification.delivery.sent.v1}. */
    public static String event(String name) {
        return EVENT_PREFIX + name + EVENT_VERSION;
    }
}
