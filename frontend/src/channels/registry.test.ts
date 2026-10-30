import { describe, expect, it } from 'vitest';
import { BUILT_IN_CHANNELS } from '../api/constants';
import { DEFAULT_CHANNEL_PROVIDERS } from '../config';
import { PROVIDERS, envSnippet, findProvider, javaSkeleton, propertyToEnv, providersFor, yamlSnippet } from './registry';

describe('provider registry', () => {
    it('describes the provider each built-in channel uses by default', () => {
        for (const channel of BUILT_IN_CHANNELS) {
            const provider = findProvider(channel, DEFAULT_CHANNEL_PROVIDERS[channel]);
            expect(provider, channel).toBeDefined();
            expect(provider!.availability).toBe('built-in');
        }
    });

    it('has a logging stub for every built-in channel', () => {
        for (const channel of BUILT_IN_CHANNELS) {
            expect(providersFor(channel).some(p => p.id === 'logging' && p.kind === 'stub'), channel).toBe(true);
        }
    });

    it('has unique ids per channel', () => {
        const ids = PROVIDERS.map(p => `${p.channel}/${p.id}`);
        expect(new Set(ids).size).toBe(ids.length);
    });

    it('maps properties to env variables like Spring relaxed binding', () => {
        expect(propertyToEnv('notification.channels.sms.stub.failure-probability')).toBe('NOTIFICATION_CHANNELS_SMS_STUB_FAILUREPROBABILITY');
        const stub = findProvider('SMS', 'logging')!;
        for (const setting of stub.settings) {
            expect(setting.env).toBe(propertyToEnv(setting.property));
        }
    });

    it('generates env, yaml and java snippets for a planned provider', () => {
        const twilio = findProvider('SMS', 'twilio')!;
        const env = envSnippet(twilio, { TWILIO_ACCOUNT_SID: 'AC1', TWILIO_FROM: '+1 555' });
        expect(env).toContain('SMS_PROVIDER=twilio');
        expect(env).toContain('TWILIO_ACCOUNT_SID=AC1');
        expect(env).toContain('TWILIO_AUTH_TOKEN=<secret>');
        expect(env).toContain('TWILIO_FROM="+1 555"');

        const yaml = yamlSnippet(twilio);
        expect(yaml).toContain('    sms:\n      provider: ${SMS_PROVIDER:twilio}\n      twilio:\n        account-sid: ${TWILIO_ACCOUNT_SID:}');

        const java = javaSkeleton(twilio);
        expect(java).toContain('package kz.company.notification.infrastructure.out.channel.sms;');
        expect(java).toContain('public class TwilioSmsProvider implements SmsProvider');
        expect(java).toContain('havingValue = "twilio"');
        expect(java).toContain('public SendResult send(SmsMessage message)');
        expect(java).toContain('record TwilioSmsProperties(\n        String accountSid,\n        String authToken,\n        String from)');
    });
});
