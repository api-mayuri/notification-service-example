import { describe, expect, it } from 'vitest';
import { detectVariables } from '../resources/templates/TemplateContentInputs';
import { itemsFromRecipients } from '../resources/notifications/BatchSend';
import { toLocalTime } from '../resources/preferences/PreferencesPage';
import { summarize, toChartData } from '../pages/StatisticsPage';

describe('helpers', () => {
    it('detects Thymeleaf variables', () => {
        expect(detectVariables('Заказ [(${orderId})]', '<b th:text="${user.name}">x</b> ${orderId}', null)).toEqual(['orderId', 'user']);
    });

    it('builds batch items from recipient lines', () => {
        const items = itemsFromRecipients('# comment\nuser-1,anna@example.com,name=Анна;code=a=b\nuser-2', {
            templateKey: 'welcome',
            channels: ['EMAIL'],
            category: 'MARKETING',
        });
        expect(items).toEqual([
            {
                recipient: { id: 'user-1', contacts: { email: 'anna@example.com' } },
                template: { key: 'welcome', variables: { name: 'Анна', code: 'a=b' } },
                channels: ['EMAIL'],
                category: 'MARKETING',
            },
            { recipient: { id: 'user-2' }, template: { key: 'welcome' }, channels: ['EMAIL'], category: 'MARKETING' },
        ]);
    });

    it('normalizes LocalTime values', () => {
        expect(toLocalTime('22:00:00')).toBe('22:00');
        expect(toLocalTime('8:05')).toBe('08:05');
        expect(toLocalTime('')).toBeNull();
    });

    it('aggregates statistics', () => {
        const stats = {
            from: '',
            to: '',
            granularity: 'DAY' as const,
            totals: { EMAIL: { SENT: 8, FAILED: 2 }, SMS: { SKIPPED: 1, PENDING: 1 } },
            rows: [
                { periodStart: '2026-10-02T00:00:00Z', channel: 'EMAIL', status: 'SENT', count: 5 },
                { periodStart: '2026-10-01T00:00:00Z', channel: 'EMAIL', status: 'SENT', count: 3 },
                { periodStart: '2026-10-01T00:00:00Z', channel: 'SMS', status: 'SENT', count: 1 },
            ],
        };
        expect(summarize(stats.totals)).toEqual({ total: 12, delivered: 8, failed: 2, skipped: 1, inFlight: 1, successRate: 80 });
        expect(toChartData(stats)).toEqual([
            { period: '2026-10-01T00:00:00Z', SENT: 4 },
            { period: '2026-10-02T00:00:00Z', SENT: 5 },
        ]);
    });
});
