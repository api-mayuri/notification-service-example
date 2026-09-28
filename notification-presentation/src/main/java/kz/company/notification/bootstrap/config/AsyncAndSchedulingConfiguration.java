package kz.company.notification.bootstrap.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Планировщик ({@code @Scheduled}), асинхронные слушатели событий ({@code @ApplicationModuleListener}
 * работает через {@code @Async}) и {@code @Retryable}/{@code @ConcurrencyLimit} из Spring Framework 7.
 * С {@code spring.threads.virtual.enabled=true} исполнители используют виртуальные потоки.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableAsync
@EnableResilientMethods
public class AsyncAndSchedulingConfiguration {
}
