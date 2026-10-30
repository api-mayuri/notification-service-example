import { describe, expect, it } from 'vitest';
import { buildNotificationRequest, toCurl, toKafkaCommand } from './requestBuilder';

describe('buildNotificationRequest', () => {
    it('builds a template request without empty parts', () => {
        const request = buildNotificationRequest({
            recipientId: ' user-42 ',
            contacts: { email: 'anna@example.com', phone: ' ' },
            extraContacts: [{ key: 'viberId', value: 'v-1' }, { key: '', value: 'x' }],
            mode: 'template',
            templateKey: 'order-shipped',
            templateLocale: 'ru-KZ',
            variables: [
                { key: 'orderId', value: 'ORD-42' },
                { key: 'count', value: '3' },
                { key: 'items', value: '[{"name":"Книга"}]' },
            ],
            channels: ['EMAIL'],
            category: 'TRANSACTIONAL',
            priority: 'NORMAL',
            metadata: [{ key: 'orderId', value: 'ORD-42' }],
        });
        expect(request).toEqual({
            recipient: { id: 'user-42', contacts: { email: 'anna@example.com', viberId: 'v-1' } },
            template: { key: 'order-shipped', locale: 'ru-KZ', variables: { orderId: 'ORD-42', count: 3, items: [{ name: 'Книга' }] } },
            channels: ['EMAIL'],
            category: 'TRANSACTIONAL',
            metadata: { orderId: 'ORD-42' },
        });
    });

    it('builds raw content with priority and sendAt', () => {
        const request = buildNotificationRequest({
            recipientId: 'u',
            mode: 'content',
            subject: 'Hi',
            text: 'Hello',
            html: '',
            channels: ['SMS'],
            category: 'SYSTEM',
            priority: 'HIGH',
            sendAt: '2030-01-01T04:00:00Z',
        });
        expect(request).toEqual({
            recipient: { id: 'u' },
            content: { subject: 'Hi', text: 'Hello' },
            channels: ['SMS'],
            category: 'SYSTEM',
            priority: 'HIGH',
            sendAt: '2030-01-01T04:00:00.000Z',
        });
    });
});

describe('request snippets', () => {
    const request = buildNotificationRequest({ recipientId: "o'neil", channels: ['EMAIL'], category: 'TRANSACTIONAL', templateKey: 'welcome' });

    it('quotes the curl body for the shell', () => {
        const curl = toCurl(request, 'key-1', 'http://localhost:8080');
        expect(curl).toContain('http://localhost:8080/api/v1/notifications');
        expect(curl).toContain("'Idempotency-Key: key-1'");
        expect(curl).toContain(`o'\\''neil`);
    });

    it('builds a CloudEvents command keyed by recipient', () => {
        const command = toKafkaCommand(request, 'evt-1', 'urn:local:demo-producer');
        expect(command).toContain('ce_id:evt-1,ce_source:urn:local:demo-producer,ce_type:kz.company.notification.send.v1');
        expect(command).toContain('--topic notification.send.requests.v1');
        expect(command).toContain(`|o'\\''neil;{`);
        expect(command).toContain('"idempotencyKey":"evt-1"');
    });
});
