import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { dataProvider, filterTemplates, toInstant } from './dataProvider';
import { tokenStorage } from '../auth/tokenStorage';
import type { TemplateView } from './types';

type Call = { url: string; init: RequestInit };

function mockFetch(responses: Array<{ status?: number; body?: unknown; headers?: Record<string, string> }>) {
    const calls: Call[] = [];
    let index = 0;
    vi.stubGlobal(
        'fetch',
        vi.fn(async (url: string, init: RequestInit) => {
            calls.push({ url, init });
            const response = responses[Math.min(index++, responses.length - 1)];
            return new Response(response.body === undefined ? '' : JSON.stringify(response.body), {
                status: response.status ?? 200,
                headers: response.headers,
            });
        }),
    );
    return calls;
}

const template = (overrides: Partial<TemplateView>): TemplateView => ({
    id: 'id',
    key: 'welcome',
    channel: 'EMAIL',
    locale: 'ru',
    activeVersion: 1,
    deduplicationWindow: null,
    versions: [],
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    ...overrides,
});

describe('dataProvider', () => {
    beforeEach(() => {
        tokenStorage.set({ token: 'token-1', clientId: 'admin-ui', scopes: [], expiresAt: null });
    });
    afterEach(() => {
        vi.unstubAllGlobals();
        tokenStorage.clear();
    });

    it('maps notification list paging and filters to the backend', async () => {
        const calls = mockFetch([{ body: { items: [{ id: 'n1' }], page: 1, size: 20, totalItems: 41, totalPages: 3 } }]);
        const result = await dataProvider.getList('notifications', {
            pagination: { page: 2, perPage: 20 },
            sort: { field: 'createdAt', order: 'DESC' },
            filter: { status: 'FAILED', recipientId: 'user-42', from: '2026-10-01T00:00:00Z' },
        });
        expect(result).toEqual({ data: [{ id: 'n1' }], total: 41 });
        const url = new URL(calls[0].url, 'http://localhost');
        expect(url.pathname).toBe('/api/v1/notifications');
        expect(Object.fromEntries(url.searchParams)).toEqual({
            status: 'FAILED',
            recipientId: 'user-42',
            from: '2026-10-01T00:00:00.000Z',
            page: '1',
            size: '20',
        });
        expect((calls[0].init.headers as Record<string, string>).Authorization).toBe('Bearer token-1');
    });

    it('clamps page size to the backend maximum', async () => {
        const calls = mockFetch([{ body: { items: [], page: 0, size: 200, totalItems: 0, totalPages: 0 } }]);
        await dataProvider.getList('notifications', { pagination: { page: 1, perPage: 1000 }, sort: { field: 'id', order: 'ASC' }, filter: {} });
        expect(calls[0].url).toContain('size=200');
    });

    it('sends notifications with the Idempotency-Key header and reports replays', async () => {
        const calls = mockFetch([
            { status: 202, body: { id: 'n1', status: 'ACCEPTED', outcome: 'REPLAYED' }, headers: { 'Idempotent-Replayed': 'true' } },
            { body: { id: 'n1', status: 'IN_PROGRESS', deliveries: [] } },
        ]);
        const result = await dataProvider.create('notifications', {
            data: { idempotencyKey: 'order-1', recipient: { id: 'u' }, channels: ['EMAIL'], category: 'TRANSACTIONAL' },
        });
        // Full view for the react-admin cache plus the outcome of the POST.
        expect(result.data).toEqual({ id: 'n1', status: 'IN_PROGRESS', deliveries: [], outcome: 'REPLAYED', replayed: true });
        expect(calls[1].url).toBe('/api/v1/notifications/n1');
        const headers = calls[0].init.headers as Record<string, string>;
        expect(headers['Idempotency-Key']).toBe('order-1');
        expect(JSON.parse(String(calls[0].init.body))).not.toHaveProperty('idempotencyKey');
    });

    it('updates template settings by key, channel and locale', async () => {
        const previous = template({ id: 't1', key: 'order-shipped', channel: 'SMS', locale: 'ru' });
        const calls = mockFetch([{ body: previous }]);
        await dataProvider.update('templates', { id: 't1', data: { deduplicationWindow: 'PT10M' }, previousData: previous });
        expect(calls[0].url).toBe('/api/v1/templates/order-shipped');
        expect(calls[0].init.method).toBe('PUT');
        expect(JSON.parse(String(calls[0].init.body))).toEqual({ channel: 'SMS', locale: 'ru', deduplicationWindow: 'PT10M' });
    });

    it('replaces preferences with PUT and keeps recipientId as id', async () => {
        const calls = mockFetch([{ body: { recipientId: 'user 1', unsubscribedChannels: ['SMS'], unsubscribedCategories: [] } }]);
        const result = await dataProvider.update('preferences', {
            id: 'user 1',
            data: { unsubscribedChannels: ['SMS'], quietHoursStart: '', timeZone: 'Asia/Almaty' },
            previousData: { id: 'user 1' },
        });
        expect(calls[0].url).toBe('/api/v1/recipients/user%201/preferences');
        expect(JSON.parse(String(calls[0].init.body))).toEqual({
            unsubscribedChannels: ['SMS'],
            unsubscribedCategories: [],
            quietHoursStart: null,
            quietHoursEnd: null,
            timeZone: 'Asia/Almaty',
            locale: null,
        });
        expect(result.data.id).toBe('user 1');
    });

    it('requests metrics with repeated tag parameters and treats 404 as no data', async () => {
        const calls = mockFetch([{ status: 404, body: {} }]);
        expect(await dataProvider.getMetric('notification.channel.send', { channel: 'sms', outcome: 'simulated' })).toBeNull();
        expect(calls[0].url).toBe('/actuator/metrics/notification.channel.send?tag=channel%3Asms&tag=outcome%3Asimulated');
    });

    it('returns DOWN health instead of throwing on 503', async () => {
        mockFetch([{ status: 503, body: { status: 'DOWN' } }]);
        expect(await dataProvider.getHealth()).toEqual({ status: 'DOWN' });
    });

    it('throws Problem Details as HttpError', async () => {
        mockFetch([{ status: 409, body: { errorCode: 'NOTIFICATION_NOT_CANCELLABLE', detail: 'Already sending' } }]);
        await expect(dataProvider.cancelNotification('n1')).rejects.toMatchObject({
            status: 409,
            message: 'NOTIFICATION_NOT_CANCELLABLE: Already sending',
        });
    });
});

describe('filterTemplates', () => {
    const templates = [
        template({ id: '1', key: 'welcome', channel: 'EMAIL', locale: 'ru' }),
        template({ id: '2', key: 'welcome', channel: 'EMAIL', locale: 'en' }),
        template({ id: '3', key: 'order-shipped', channel: 'SMS', locale: 'ru', activeVersion: null }),
    ];

    it('filters, sorts and paginates in the browser', () => {
        const result = filterTemplates(templates, {
            filter: { q: 'WEL' },
            sort: { field: 'locale', order: 'ASC' },
            pagination: { page: 1, perPage: 1 },
        });
        expect(result.total).toBe(2);
        expect(result.data.map(t => t.id)).toEqual(['2']);
    });

    it('filters by channel and active version', () => {
        expect(filterTemplates(templates, { filter: { channel: 'SMS' } } as never).data.map(t => t.id)).toEqual(['3']);
        expect(filterTemplates(templates, { filter: { hasActive: false } } as never).data.map(t => t.id)).toEqual(['3']);
    });
});

describe('toInstant', () => {
    it('converts form values to ISO instants', () => {
        expect(toInstant('')).toBeUndefined();
        expect(toInstant('not a date')).toBeUndefined();
        expect(toInstant(new Date('2026-10-30T10:00:00Z'))).toBe('2026-10-30T10:00:00.000Z');
    });
});
