package kz.company.notification.application.port.in;

/** Освободить ключи идемпотентности старше срока хранения (не меньше 24 часов). */
public interface ReleaseExpiredIdempotencyKeysUseCase {

    int releaseExpired();
}
