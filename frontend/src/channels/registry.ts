import { config } from '../config';
import { CHANNELS } from '../api/constants';
import type { ChannelInfo, ProviderDescriptor, ProviderSetting } from './types';
import { BUILT_IN_PROVIDERS } from './providers/builtIn';
import { PLANNED_PROVIDERS } from './providers/planned';

/**
 * Registry of channel providers shown on the "Channels & providers" page.
 * To add a provider to the UI: describe it in providers/planned.ts (or your own file) and add it
 * to PROVIDERS; once the backend class exists, move it to builtIn.ts with availability 'built-in'.
 */
export const PROVIDERS: ProviderDescriptor[] = [...BUILT_IN_PROVIDERS, ...PLANNED_PROVIDERS];

const BASE_PACKAGE = 'kz.company.notification.infrastructure.out.channel';

export const CHANNEL_INFO: Record<string, ChannelInfo> = {
    EMAIL: info('EMAIL', 'EmailProvider', 'EmailMessage', 'email', 'email'),
    SMS: info('SMS', 'SmsProvider', 'SmsMessage', 'sms', 'phone'),
    TELEGRAM: info('TELEGRAM', 'TelegramProvider', 'TelegramMessage', 'telegram', 'telegramChatId'),
    WHATSAPP: info('WHATSAPP', 'WhatsAppProvider', 'WhatsAppMessage', 'whatsapp', 'phone'),
    PUSH: info('PUSH', 'PushProvider', 'PushMessage', 'push', 'deviceToken'),
    WEBHOOK: info('WEBHOOK', 'WebhookProvider', 'WebhookMessage', 'webhook', 'webhookUrl'),
};

function info(channel: string, providerInterface: string, messageType: string, pkg: string, contactType: string): ChannelInfo {
    return {
        channel,
        providerInterface,
        messageType,
        packageName: `${BASE_PACKAGE}.${pkg}`,
        contactType,
        description: `app.channel.${channel}`,
    };
}

/** Channel info for custom channels (EXTRA_CHANNELS): a whole ChannelSender, no provider port. */
export function channelInfo(channel: string): ChannelInfo {
    return (
        CHANNEL_INFO[channel] ?? {
            channel,
            providerInterface: 'ChannelSender',
            messageType: 'Delivery',
            packageName: `${BASE_PACKAGE}.${channel.toLowerCase()}`,
            contactType: `${channel.toLowerCase()}Id`,
            description: 'app.channel.custom',
        }
    );
}

export const providersFor = (channel: string) => PROVIDERS.filter(provider => provider.channel === channel);

export function findProvider(channel: string, id: string | undefined): ProviderDescriptor | undefined {
    return PROVIDERS.find(provider => provider.channel === channel && provider.id === id);
}

/** Provider the backend is configured with, according to CHANNEL_PROVIDERS in the frontend config. */
export function currentProviderId(channel: string): string | undefined {
    return config.channelProviders[channel];
}

export const allChannels = () => CHANNELS;

/** Env variable that selects the provider (docker-compose maps it to notification.channels.<c>.provider). */
export const providerEnv = (channel: string) => `${channel}_PROVIDER`;

/** Spring relaxed binding: notification.channels.sms.stub.failure-probability → NOTIFICATION_CHANNELS_SMS_STUB_FAILUREPROBABILITY. */
export function propertyToEnv(property: string): string {
    return property.replace(/-/g, '').replace(/\./g, '_').toUpperCase();
}

const quoteEnv = (value: string) => (/[\s#"'$]/.test(value) ? `"${value.replace(/(["\\$])/g, '\\$1')}"` : value);

export function envSnippet(provider: ProviderDescriptor, values: Record<string, string>): string {
    const lines = [`# ${provider.channel}: ${provider.name}`, `${providerEnv(provider.channel)}=${provider.id}`];
    for (const setting of provider.settings) {
        const value = values[setting.env] ?? setting.defaultValue ?? '';
        if (!value && !setting.required) continue;
        lines.push(`${setting.env}=${setting.secret && !value ? '<secret>' : quoteEnv(value)}`);
    }
    return lines.join('\n');
}

/** application.yml fragment: provider selection and properties read from the env variables above. */
export function yamlSnippet(provider: ProviderDescriptor): string {
    const channel = provider.channel.toLowerCase();
    const tree: Record<string, unknown> = { provider: `\${${providerEnv(provider.channel)}:${provider.id}}` };
    for (const setting of provider.settings) {
        const prefix = `notification.channels.${channel}.`;
        if (!setting.property.startsWith(prefix)) continue;
        const path = setting.property.slice(prefix.length).split('.');
        let node = tree;
        for (const part of path.slice(0, -1)) {
            node = (node[part] as Record<string, unknown>) ?? (node[part] = {});
        }
        node[path.at(-1)!] = `\${${setting.env}:${setting.defaultValue ?? ''}}`;
    }
    const render = (node: Record<string, unknown>, indent: number): string[] =>
        Object.entries(node).flatMap(([key, value]) =>
            typeof value === 'object' && value !== null
                ? [`${' '.repeat(indent)}${key}:`, ...render(value as Record<string, unknown>, indent + 2)]
                : [`${' '.repeat(indent)}${key}: ${value}`],
        );
    return ['notification:', '  channels:', `    ${channel}:`, ...render(tree, 6)].join('\n');
}

const pascal = (value: string) =>
    value
        .split(/[^A-Za-z0-9]+/)
        .filter(Boolean)
        .map(part => part[0].toUpperCase() + part.slice(1))
        .join('');

/** Java skeleton for a planned provider, following docs/extending.md §1. */
export function javaSkeleton(provider: ProviderDescriptor): string {
    const channel = channelInfo(provider.channel);
    const prefix = `notification.channels.${provider.channel.toLowerCase()}`;
    const propsClass = `${pascal(provider.id)}${pascal(provider.channel.toLowerCase())}Properties`;
    const fields = provider.settings
        .filter(setting => setting.property.startsWith(`${prefix}.${provider.id}.`))
        .map(setting => {
            const name = setting.property.split('.').at(-1)!.replace(/-([a-z])/g, (_, c: string) => c.toUpperCase());
            return `        String ${name}`;
        });
    return `package ${channel.packageName};

import kz.company.notification.domain.delivery.SendResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** ${provider.name}: ${provider.channel} provider. Selected by ${prefix}.provider=${provider.id}. */
@Component
@ConditionalOnProperty(prefix = "${prefix}", name = "provider", havingValue = "${provider.id}")
public class ${provider.implementation} implements ${channel.providerInterface} {

    private final ${propsClass} properties;

    public ${provider.implementation}(${propsClass} properties) {
        this.properties = properties;
    }

    @Override
    public SendResult send(${channel.messageType} message) {
        // message.deliveryId() — pass to the provider as the idempotency key if it supports one.
        try {
            // String messageId = client.send(...);
            return SendResult.Success.real("provider-message-id");
        } catch (RuntimeException temporary) {
            // network, 429, 5xx → retry by notification.retry.*
            return new SendResult.RetryableFailure(temporary.getMessage());
        }
        // invalid address, rejected content → new SendResult.PermanentFailure(reason)
    }
}

// Settings: move to its own file ${propsClass}.java in the same package.
@ConfigurationProperties(prefix = "${prefix}.${provider.id}")
record ${propsClass}(
${fields.join(',\n') || '        String apiKey'}) {
}
`;
}

export function settingLabel(setting: ProviderSetting, translate: (key: string, options?: object) => string): string {
    return translate(setting.label, { _: setting.label });
}
