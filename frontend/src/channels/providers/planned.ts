import type { ProviderDescriptor, ProviderSetting } from '../types';

/**
 * Real providers that are not implemented on the backend yet: the UI shows their settings and
 * generates .env, application.yml and a Java skeleton. Add your provider here.
 */
function setting(channel: string, provider: string, name: string, env: string, extra: Partial<ProviderSetting> = {}): ProviderSetting {
    return {
        env,
        property: `notification.channels.${channel.toLowerCase()}.${provider}.${name}`,
        label: name,
        required: true,
        ...extra,
    };
}

export const PLANNED_PROVIDERS: ProviderDescriptor[] = [
    {
        id: 'twilio',
        channel: 'SMS',
        name: 'Twilio',
        kind: 'real',
        availability: 'planned',
        description: 'app.provider.twilio',
        implementation: 'TwilioSmsProvider',
        docsUrl: 'https://www.twilio.com/docs/messaging/api/message-resource',
        settings: [
            setting('SMS', 'twilio', 'account-sid', 'TWILIO_ACCOUNT_SID', { placeholder: 'ACxxxxxxxx' }),
            setting('SMS', 'twilio', 'auth-token', 'TWILIO_AUTH_TOKEN', { secret: true }),
            setting('SMS', 'twilio', 'from', 'TWILIO_FROM', { placeholder: '+15005550006' }),
        ],
    },
    {
        id: 'mobizon',
        channel: 'SMS',
        name: 'Mobizon',
        kind: 'real',
        availability: 'planned',
        description: 'app.provider.mobizon',
        implementation: 'MobizonSmsProvider',
        docsUrl: 'https://mobizon.kz/help/api-docs',
        settings: [
            setting('SMS', 'mobizon', 'api-url', 'MOBIZON_API_URL', { defaultValue: 'https://api.mobizon.kz' }),
            setting('SMS', 'mobizon', 'api-key', 'MOBIZON_API_KEY', { secret: true }),
            setting('SMS', 'mobizon', 'from', 'MOBIZON_FROM', { required: false }),
        ],
    },
    {
        id: 'bot-api',
        channel: 'TELEGRAM',
        name: 'Telegram Bot API',
        kind: 'real',
        availability: 'planned',
        description: 'app.provider.telegram_bot',
        implementation: 'BotApiTelegramProvider',
        docsUrl: 'https://core.telegram.org/bots/api#sendmessage',
        settings: [
            setting('TELEGRAM', 'bot-api', 'token', 'TELEGRAM_BOT_TOKEN', { secret: true, placeholder: '123456:ABC-DEF…' }),
            setting('TELEGRAM', 'bot-api', 'api-url', 'TELEGRAM_API_URL', { defaultValue: 'https://api.telegram.org' }),
            setting('TELEGRAM', 'bot-api', 'parse-mode', 'TELEGRAM_PARSE_MODE', { required: false, defaultValue: 'HTML' }),
        ],
    },
    {
        id: 'meta-cloud',
        channel: 'WHATSAPP',
        name: 'WhatsApp Cloud API (Meta)',
        kind: 'real',
        availability: 'planned',
        description: 'app.provider.whatsapp_cloud',
        implementation: 'MetaCloudWhatsAppProvider',
        docsUrl: 'https://developers.facebook.com/docs/whatsapp/cloud-api/messages',
        settings: [
            setting('WHATSAPP', 'meta-cloud', 'access-token', 'WHATSAPP_ACCESS_TOKEN', { secret: true }),
            setting('WHATSAPP', 'meta-cloud', 'phone-number-id', 'WHATSAPP_PHONE_NUMBER_ID'),
            setting('WHATSAPP', 'meta-cloud', 'api-version', 'WHATSAPP_API_VERSION', { defaultValue: 'v21.0' }),
        ],
    },
    {
        id: 'fcm',
        channel: 'PUSH',
        name: 'Firebase Cloud Messaging',
        kind: 'real',
        availability: 'planned',
        description: 'app.provider.fcm',
        implementation: 'FcmPushProvider',
        docsUrl: 'https://firebase.google.com/docs/cloud-messaging/send-message',
        settings: [
            setting('PUSH', 'fcm', 'project-id', 'FCM_PROJECT_ID'),
            setting('PUSH', 'fcm', 'credentials-file', 'FCM_CREDENTIALS_FILE', { placeholder: '/run/secrets/fcm.json' }),
        ],
    },
    {
        id: 'apns',
        channel: 'PUSH',
        name: 'Apple Push Notification service',
        kind: 'real',
        availability: 'planned',
        description: 'app.provider.apns',
        implementation: 'ApnsPushProvider',
        docsUrl: 'https://developer.apple.com/documentation/usernotifications/sending-notification-requests-to-apns',
        settings: [
            setting('PUSH', 'apns', 'team-id', 'APNS_TEAM_ID'),
            setting('PUSH', 'apns', 'key-id', 'APNS_KEY_ID'),
            setting('PUSH', 'apns', 'bundle-id', 'APNS_BUNDLE_ID'),
            setting('PUSH', 'apns', 'private-key-file', 'APNS_PRIVATE_KEY_FILE', { placeholder: '/run/secrets/apns.p8' }),
        ],
    },
    {
        id: 'sendgrid',
        channel: 'EMAIL',
        name: 'SendGrid',
        kind: 'real',
        availability: 'planned',
        description: 'app.provider.sendgrid',
        implementation: 'SendGridEmailProvider',
        docsUrl: 'https://www.twilio.com/docs/sendgrid/api-reference/mail-send/mail-send',
        settings: [setting('EMAIL', 'sendgrid', 'api-key', 'SENDGRID_API_KEY', { secret: true })],
    },
];
