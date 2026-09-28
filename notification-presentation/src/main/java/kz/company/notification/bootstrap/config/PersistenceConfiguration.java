package kz.company.notification.bootstrap.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA-сущности и репозитории лежат в модуле infrastructure, а не в пакете приложения. Вынесено
 * из главного класса, чтобы не мешать тестовым срезам ({@code @WebMvcTest}).
 */
@Configuration(proxyBeanMethods = false)
@EntityScan(basePackages = "kz.company.notification.infrastructure.out.persistence.entity")
@EnableJpaRepositories(basePackages = "kz.company.notification.infrastructure.out.persistence.repository")
public class PersistenceConfiguration {
}
