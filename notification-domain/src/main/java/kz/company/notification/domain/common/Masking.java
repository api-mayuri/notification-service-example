package kz.company.notification.domain.common;

/** Маскирование персональных данных для логов и публичных ответов. */
public final class Masking {

    private Masking() {
    }

    /** Оставляет по краям не более {@code visible} символов, середину заменяет звёздочками. */
    public static String keepEdges(String value, int visible) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.length() <= visible * 2) {
            return "*".repeat(value.length());
        }
        return value.substring(0, visible) + "***" + value.substring(value.length() - visible);
    }

    /** {@code john.smith@example.com} превращается в {@code j***h@example.com}. */
    public static String email(String value) {
        int at = value.indexOf('@');
        if (at <= 0) {
            return keepEdges(value, 1);
        }
        String local = value.substring(0, at);
        String maskedLocal = local.length() <= 2 ? "*".repeat(local.length()) : keepEdges(local, 1);
        return maskedLocal + value.substring(at);
    }
}
