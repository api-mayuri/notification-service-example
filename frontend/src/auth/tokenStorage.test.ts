import { describe, expect, it } from 'vitest';
import { authFromToken, isExpired } from './tokenStorage';

const jwt = (claims: object) => `e30.${btoa(JSON.stringify(claims)).replace(/=+$/, '').replace(/\+/g, '-').replace(/\//g, '_')}.sig`;

describe('authFromToken', () => {
    it('reads client id and scopes like the backend', () => {
        const auth = authFromToken(jwt({ sub: 's', client_id: 'orders-service', scope: 'notifications:send notifications:read', exp: 2000000000 }));
        expect(auth.clientId).toBe('orders-service');
        expect(auth.scopes).toEqual(['notifications:send', 'notifications:read']);
        expect(auth.expiresAt).toBe(2000000000000);
        expect(isExpired(auth, 1)).toBe(false);
    });

    it('falls back to azp, then sub; supports scp arrays', () => {
        expect(authFromToken(jwt({ azp: 'web', sub: 's' })).clientId).toBe('web');
        expect(authFromToken(jwt({ sub: 's', scp: ['templates:manage'] })).scopes).toEqual(['templates:manage']);
    });

    it('rejects non-JWT tokens', () => {
        expect(() => authFromToken('opaque')).toThrow();
    });
});
