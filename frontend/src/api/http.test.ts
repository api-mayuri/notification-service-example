import { describe, expect, it } from 'vitest';
import { buildUrl, toHttpError } from './http';

describe('buildUrl', () => {
    it('skips empty query values', () => {
        expect(buildUrl('/api/v1/notifications', { status: 'FAILED', channel: '', recipientId: undefined, page: 0 }, '')).toBe(
            '/api/v1/notifications?status=FAILED&page=0',
        );
    });

    it('prefixes the base URL', () => {
        expect(buildUrl('/actuator/health', undefined, 'http://api')).toBe('http://api/actuator/health');
    });
});

describe('toHttpError', () => {
    it('builds the message from errorCode and detail', () => {
        const error = toHttpError(422, {
            errorCode: 'MISSING_TEMPLATE_VARIABLES',
            detail: 'Template needs variables',
            missingVariables: ['orderId'],
        });
        expect(error.status).toBe(422);
        expect(error.message).toBe('MISSING_TEMPLATE_VARIABLES: Template needs variables: [orderId]');
    });

    it('maps field errors for react-admin server-side validation', () => {
        const error = toHttpError(400, {
            errorCode: 'VALIDATION_FAILED',
            detail: 'Request body is invalid',
            errors: [{ field: 'recipient.id', message: 'must not be blank' }],
        });
        expect((error.body as { errors: Record<string, unknown> }).errors).toEqual({
            'recipient.id': 'must not be blank',
            root: { serverError: 'VALIDATION_FAILED: Request body is invalid' },
        });
    });

    it('adds Retry-After for 429', () => {
        const error = toHttpError(429, { errorCode: 'RATE_LIMIT_EXCEEDED', detail: 'Too many' }, new Headers({ 'Retry-After': '1' }));
        expect(error.message).toContain('Retry-After: 1s');
    });

    it('falls back to a translatable message without a body', () => {
        expect(toHttpError(403, null).message).toBe('app.error.forbidden');
    });
});
