import type { ProviderDescriptor, ProviderSetting } from '../types';

/** notification.channels.<channel>.stub.*: random failures of a logging provider (StubBehaviour). */
export function stubSettings(channel: string): ProviderSetting[] {
    const prefix = `notification.channels.${channel.toLowerCase()}.stub`;
    const env = `NOTIFICATION_CHANNELS_${channel}_STUB`;
    return [
        {
            env: `${env}_FAILUREPROBABILITY`,
            property: `${prefix}.failure-probability`,
            label: 'app.provider.setting.failure_probability',
            defaultValue: '0.0',
            type: 'number',
            help: 'app.provider.setting.failure_probability_help',
        },
        {
            env: `${env}_PERMANENTFAILUREPROBABILITY`,
            property: `${prefix}.permanent-failure-probability`,
            label: 'app.provider.setting.permanent_failure_probability',
            defaultValue: '0.0',
            type: 'number',
            help: 'app.provider.setting.permanent_failure_probability_help',
        },
    ];
}

function logging(channel: string, implementation: string): ProviderDescriptor {
    return {
        id: 'logging',
        channel,
        name: 'Logging stub',
        kind: 'stub',
        availability: 'built-in',
        description: 'app.provider.logging',
        implementation,
        settings: stubSettings(channel),
    };
}

/** Providers implemented in notification-infrastructure/out/channel. */
export const BUILT_IN_PROVIDERS: ProviderDescriptor[] = [
    {
        id: 'smtp',
        channel: 'EMAIL',
        name: 'SMTP',
        kind: 'real',
        availability: 'built-in',
        description: 'app.provider.smtp',
        implementation: 'SmtpEmailProvider',
        settings: [
            { env: 'SMTP_HOST', property: 'spring.mail.host', label: 'Host', required: true, placeholder: 'smtp.company.kz' },
            { env: 'SMTP_PORT', property: 'spring.mail.port', label: 'Port', required: true, defaultValue: '587', type: 'number' },
            { env: 'SMTP_USERNAME', property: 'spring.mail.username', label: 'Username' },
            { env: 'SMTP_PASSWORD', property: 'spring.mail.password', label: 'Password', secret: true },
            { env: 'SMTP_AUTH', property: 'mail.smtp.auth', label: 'SMTP AUTH', defaultValue: 'true', type: 'boolean' },
            { env: 'SMTP_STARTTLS', property: 'mail.smtp.starttls.enable', label: 'STARTTLS (587)', defaultValue: 'true', type: 'boolean' },
            { env: 'SMTP_SSL', property: 'mail.smtp.ssl.enable', label: 'SSL (465)', defaultValue: 'false', type: 'boolean' },
            { env: 'MAIL_FROM', property: 'notification.channels.email.from', label: 'From', required: true, placeholder: 'notifications@company.kz' },
            { env: 'MAIL_FROM_NAME', property: 'notification.channels.email.from-name', label: 'From name', placeholder: 'Компания' },
            { env: 'MAIL_REPLY_TO', property: 'notification.channels.email.reply-to', label: 'Reply-To' },
            { env: 'MAIL_MESSAGE_ID_DOMAIN', property: 'notification.channels.email.message-id-domain', label: 'Message-ID domain', placeholder: 'company.kz' },
            {
                env: 'MAIL_UNSUBSCRIBE_URL_TEMPLATE',
                property: 'notification.channels.email.unsubscribe-url-template',
                label: 'List-Unsubscribe URL',
                placeholder: 'https://company.kz/unsubscribe?recipient={recipientId}',
            },
        ],
    },
    logging('EMAIL', 'LoggingEmailProvider'),
    logging('SMS', 'LoggingSmsProvider'),
    logging('TELEGRAM', 'LoggingTelegramProvider'),
    logging('WHATSAPP', 'LoggingWhatsAppProvider'),
    logging('PUSH', 'LoggingPushProvider'),
    {
        id: 'http',
        channel: 'WEBHOOK',
        name: 'HTTP + HMAC',
        kind: 'real',
        availability: 'built-in',
        description: 'app.provider.http',
        implementation: 'HttpWebhookProvider',
        settings: [
            {
                env: 'WEBHOOK_SIGNING_SECRET',
                property: 'notification.channels.webhook.signing-secret',
                label: 'app.provider.setting.signing_secret',
                secret: true,
                required: true,
            },
            {
                env: 'NOTIFICATION_CHANNELS_WEBHOOK_REQUIREHTTPS',
                property: 'notification.channels.webhook.require-https',
                label: 'app.provider.setting.require_https',
                defaultValue: 'false',
                type: 'boolean',
            },
            {
                env: 'NOTIFICATION_CHANNELS_WEBHOOK_CONNECTTIMEOUT',
                property: 'notification.channels.webhook.connect-timeout',
                label: 'Connect timeout',
                defaultValue: '5s',
            },
            {
                env: 'NOTIFICATION_CHANNELS_WEBHOOK_READTIMEOUT',
                property: 'notification.channels.webhook.read-timeout',
                label: 'Read timeout',
                defaultValue: '10s',
            },
        ],
    },
    logging('WEBHOOK', 'LoggingWebhookProvider'),
];
