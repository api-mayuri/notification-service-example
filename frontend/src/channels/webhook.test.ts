import { createHmac } from 'node:crypto';
import { describe, expect, it } from 'vitest';
import { parseSignatureHeader, webhookSignature } from './WebhookSignatureTool';

describe('webhook signature', () => {
    it('matches HMAC-SHA256(secret, t + "." + body)', async () => {
        const body = '{"deliveryId":"d1"}';
        const expected = createHmac('sha256', 'secret').update(`1760380000.${body}`).digest('hex');
        expect(await webhookSignature('secret', 1760380000, body)).toBe(expected);
    });

    it('parses the signature header', () => {
        expect(parseSignatureHeader('t=1760380000, v1=abc')).toEqual({ t: 1760380000, v1: 'abc' });
        expect(parseSignatureHeader('garbage')).toEqual({ t: undefined, v1: undefined });
    });
});
