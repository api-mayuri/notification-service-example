/**
 * Frontend settings. Priority: runtime `window.__APP_CONFIG__` (Docker: generated from env at
 * container start) → build-time `VITE_*` variables (.env) → defaults.
 */
export type AuthMode = 'dev-token' | 'bearer';

export interface AppConfig {
    title: string;
    /** Backend base URL; empty — same origin (nginx / Vite proxy). */
    apiUrl: string;
    /** dev-token: login via POST /dev/token (profile local); bearer: paste a token from your IdP. */
    authMode: AuthMode;
    devTokenClientId: string;
    defaultLocale: 'ru' | 'en';
    /** Refresh interval for notification details while delivery is in progress; 0 disables. */
    pollIntervalMs: number;
    swaggerUrl: string;
    mailpitUrl: string;
    kafkaUiUrl: string;
    /** Channels added on the backend besides the built-in ones, e.g. ["VIBER"]. */
    extraChannels: string[];
    /** Provider per channel as configured on the backend (notification.channels.<channel>.provider). */
    channelProviders: Record<string, string>;
}

export type RawConfig = Record<string, string | undefined>;

declare global {
    interface Window {
        __APP_CONFIG__?: RawConfig;
    }
}

export const DEFAULT_CHANNEL_PROVIDERS: Record<string, string> = {
    EMAIL: 'smtp',
    SMS: 'logging',
    TELEGRAM: 'logging',
    WHATSAPP: 'logging',
    PUSH: 'logging',
    WEBHOOK: 'http',
};

const KEYS = {
    title: 'APP_TITLE',
    apiUrl: 'API_URL',
    authMode: 'AUTH_MODE',
    devTokenClientId: 'DEV_TOKEN_CLIENT_ID',
    defaultLocale: 'DEFAULT_LOCALE',
    pollIntervalMs: 'POLL_INTERVAL_MS',
    swaggerUrl: 'SWAGGER_URL',
    mailpitUrl: 'MAILPIT_URL',
    kafkaUiUrl: 'KAFKA_UI_URL',
    extraChannels: 'EXTRA_CHANNELS',
    channelProviders: 'CHANNEL_PROVIDERS',
} as const;

/** "SMS=twilio, PUSH=fcm" → {SMS: "twilio", PUSH: "fcm"}. */
export function parseChannelProviders(value: string | undefined): Record<string, string> {
    const result: Record<string, string> = {};
    for (const pair of (value ?? '').split(',')) {
        const [channel, provider] = pair.split('=').map(part => part?.trim());
        if (channel && provider) {
            result[channel.toUpperCase()] = provider.toLowerCase();
        }
    }
    return result;
}

export function parseList(value: string | undefined): string[] {
    return (value ?? '')
        .split(',')
        .map(item => item.trim().toUpperCase())
        .filter(Boolean);
}

/** Pure merge of runtime and build-time values; exported for tests. */
export function resolveConfig(runtime: RawConfig, build: RawConfig): AppConfig {
    // Empty values count as unset: the Docker entrypoint writes every variable, set or not.
    const present = (value: string | undefined) => (value === undefined || value === '' ? undefined : value);
    const read = (key: string): string | undefined => present(runtime[key]) ?? present(build[`VITE_${key}`]);
    const authMode = read(KEYS.authMode) === 'bearer' ? 'bearer' : 'dev-token';
    const locale = read(KEYS.defaultLocale) === 'en' ? 'en' : 'ru';
    const poll = Number(read(KEYS.pollIntervalMs) ?? 5000);
    return {
        title: read(KEYS.title) ?? 'Notification Service',
        apiUrl: (read(KEYS.apiUrl) ?? '').replace(/\/+$/, ''),
        authMode,
        devTokenClientId: read(KEYS.devTokenClientId) ?? 'admin-ui',
        defaultLocale: locale,
        pollIntervalMs: Number.isFinite(poll) && poll >= 0 ? poll : 5000,
        swaggerUrl: read(KEYS.swaggerUrl) ?? '/swagger-ui.html',
        mailpitUrl: read(KEYS.mailpitUrl) ?? 'http://localhost:8025',
        kafkaUiUrl: read(KEYS.kafkaUiUrl) ?? 'http://localhost:8088',
        extraChannels: parseList(read(KEYS.extraChannels)),
        channelProviders: { ...DEFAULT_CHANNEL_PROVIDERS, ...parseChannelProviders(read(KEYS.channelProviders)) },
    };
}

export const config: AppConfig = resolveConfig(
    (typeof window !== 'undefined' && window.__APP_CONFIG__) || {},
    import.meta.env as unknown as RawConfig,
);
