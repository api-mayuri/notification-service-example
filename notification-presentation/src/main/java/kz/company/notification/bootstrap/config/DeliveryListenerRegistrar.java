package kz.company.notification.bootstrap.config;

import kz.company.notification.application.port.in.ProcessDeliveryUseCase;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.infrastructure.in.messaging.ChannelDeliveryListener;
import kz.company.notification.infrastructure.messaging.NotificationKafkaProperties;
import kz.company.notification.infrastructure.messaging.TopicNames;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import tools.jackson.databind.json.JsonMapper;

import java.util.Locale;
import java.util.Map;

/**
 * Регистрирует по бину {@link ChannelDeliveryListener} на каждый включённый канал и полосу
 * приоритета. Так у каждого канала свой топик, consumer group и concurrency, а новый канал
 * получает воркер автоматически — достаточно {@code notification.channels.<канал>.enabled=true}.
 */
public class DeliveryListenerRegistrar implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        Binder binder = Binder.get(environment);
        Map<String, ChannelListenerSettings> channels = binder
                .bind("notification.channels", Bindable.mapOf(String.class, ChannelListenerSettings.class))
                .orElse(Map.of());
        TopicNames topicNames = new TopicNames(
                binder.bindOrCreate("notification.kafka", NotificationKafkaProperties.class));
        ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory) registry;

        channels.forEach((code, settings) -> {
            if (!settings.enabled()) {
                return;
            }
            ChannelType channel = ChannelType.of(code);
            for (TopicNames.Lane lane : TopicNames.Lane.values()) {
                String beanName = "delivery-" + channel.lowerCaseCode() + "-" + lane.name().toLowerCase(Locale.ROOT);
                int concurrency = lane == TopicNames.Lane.HIGH ? settings.highPriorityConcurrency() : settings.concurrency();
                RootBeanDefinition definition = new RootBeanDefinition(ChannelDeliveryListener.class,
                        () -> new ChannelDeliveryListener(beanName, topicNames.deliveryLane(channel, lane),
                                topicNames.deliveryConsumerGroup(channel, lane), concurrency,
                                beanFactory.getBean(ProcessDeliveryUseCase.class),
                                beanFactory.getBean(JsonMapper.class)));
                registry.registerBeanDefinition(beanName, definition);
            }
        });
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        // Все регистрации выполнены в postProcessBeanDefinitionRegistry.
    }
}
