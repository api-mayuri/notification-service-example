package kz.company.notification.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Точка входа. Сканирует все модули ({@code kz.company.notification}); сценарии приложения
 * не помечены стереотипами и собираются в {@code config.UseCaseConfiguration}.
 */
@SpringBootApplication(scanBasePackages = "kz.company.notification")
@ConfigurationPropertiesScan("kz.company.notification")
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
