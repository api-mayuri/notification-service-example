package kz.company.notification.application.port.out;

import java.util.UUID;

/** Генерация идентификаторов. В тестах — детерминированная реализация. */
@FunctionalInterface
public interface IdGenerator {

    UUID newId();
}
