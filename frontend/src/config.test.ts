import { describe, expect, it } from 'vitest';
import { DEFAULT_CHANNEL_PROVIDERS, parseChannelProviders, resolveConfig } from './config';

describe('resolveConfig', () => {
    it('uses defaults without settings', () => {
        const config = resolveConfig({}, {});
        expect(config.apiUrl).toBe('');
        expect(config.authMode).toBe('dev-token');
        expect(config.defaultLocale).toBe('ru');
        expect(config.pollIntervalMs).toBe(5000);
        expect(config.channelProviders).toEqual(DEFAULT_CHANNEL_PROVIDERS);
    });

    it('prefers runtime values over VITE_ build values', () => {
        const config = resolveConfig(
            { API_URL: 'https://notifications.company.kz/', AUTH_MODE: 'bearer' },
            { VITE_API_URL: 'http://localhost:8080', VITE_DEFAULT_LOCALE: 'en', VITE_APP_TITLE: 'Admin' },
        );
        expect(config.apiUrl).toBe('https://notifications.company.kz');
        expect(config.authMode).toBe('bearer');
        expect(config.defaultLocale).toBe('en');
        expect(config.title).toBe('Admin');
    });

    it('treats empty runtime values as unset', () => {
        expect(resolveConfig({ APP_TITLE: '' }, { VITE_APP_TITLE: 'Build title' }).title).toBe('Build title');
    });

    it('merges channel providers and extra channels', () => {
        const config = resolveConfig({ CHANNEL_PROVIDERS: 'sms=twilio, PUSH=FCM', EXTRA_CHANNELS: 'viber, slack' }, {});
        expect(config.channelProviders.SMS).toBe('twilio');
        expect(config.channelProviders.PUSH).toBe('fcm');
        expect(config.channelProviders.EMAIL).toBe('smtp');
        expect(config.extraChannels).toEqual(['VIBER', 'SLACK']);
    });

    it('ignores invalid numbers and pairs', () => {
        expect(resolveConfig({ POLL_INTERVAL_MS: 'abc' }, {}).pollIntervalMs).toBe(5000);
        expect(resolveConfig({ POLL_INTERVAL_MS: '0' }, {}).pollIntervalMs).toBe(0);
        expect(parseChannelProviders('SMS, =x, PUSH=')).toEqual({});
    });
});
