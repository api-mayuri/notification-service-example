/** Access token kept in localStorage; claims are decoded locally only for display and scopes. */
const STORAGE_KEY = 'notification-admin.auth';

export interface StoredAuth {
    token: string;
    clientId: string;
    scopes: string[];
    /** Epoch milliseconds; null if the token has no exp claim. */
    expiresAt: number | null;
}

export interface JwtClaims {
    sub?: string;
    client_id?: string;
    azp?: string;
    scope?: string | string[];
    scp?: string | string[];
    exp?: number;
    iat?: number;
    iss?: string;
    aud?: string | string[];
    [claim: string]: unknown;
}

export function decodeJwt(token: string): JwtClaims {
    const parts = token.split('.');
    if (parts.length < 2) {
        throw new Error('Token is not a JWT');
    }
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4);
    const bytes = Uint8Array.from(atob(padded), char => char.charCodeAt(0));
    return JSON.parse(new TextDecoder().decode(bytes)) as JwtClaims;
}

function toList(value: string | string[] | undefined): string[] {
    if (!value) return [];
    return Array.isArray(value) ? value : value.split(' ').filter(Boolean);
}

/** Same order as AuthenticatedClient on the backend: client_id, then azp, then sub. */
export function authFromToken(token: string): StoredAuth {
    const claims = decodeJwt(token);
    return {
        token,
        clientId: claims.client_id ?? claims.azp ?? claims.sub ?? 'unknown',
        scopes: [...toList(claims.scope), ...toList(claims.scp)],
        expiresAt: typeof claims.exp === 'number' ? claims.exp * 1000 : null,
    };
}

export function isExpired(auth: StoredAuth, now = Date.now()): boolean {
    return auth.expiresAt !== null && auth.expiresAt <= now;
}

export const tokenStorage = {
    get(): StoredAuth | null {
        try {
            const raw = localStorage.getItem(STORAGE_KEY);
            return raw ? (JSON.parse(raw) as StoredAuth) : null;
        } catch {
            return null;
        }
    },
    set(auth: StoredAuth): void {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
    },
    clear(): void {
        localStorage.removeItem(STORAGE_KEY);
    },
};
