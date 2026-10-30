import type { AuthProvider } from 'react-admin';
import { apiRequest } from '../api/http';
import type { DevTokenResponse } from '../api/types';
import { authFromToken, isExpired, tokenStorage } from './tokenStorage';

/**
 * Login params:
 *  - dev token (profile local): { mode: 'dev-token', clientId, scopes } → POST /dev/token;
 *  - bearer: { mode: 'bearer', token } — a token issued by your IdP.
 * To plug in OIDC, obtain the token in a custom login page and call login({ mode: 'bearer', token }).
 */
export type LoginParams =
    | { mode: 'dev-token'; clientId: string; scopes?: string[] }
    | { mode: 'bearer'; token: string };

export const authProvider: AuthProvider = {
    async login(params: LoginParams) {
        let token: string;
        if (params.mode === 'dev-token') {
            const response = await apiRequest<DevTokenResponse>('/dev/token', {
                method: 'POST',
                anonymous: true,
                body: { clientId: params.clientId, scopes: params.scopes?.length ? params.scopes : undefined },
            });
            token = response.body.access_token;
        } else {
            token = params.token.trim().replace(/^Bearer\s+/i, '');
        }
        const auth = authFromToken(token);
        if (isExpired(auth)) {
            throw new Error('app.auth.expired');
        }
        tokenStorage.set(auth);
    },

    async logout() {
        tokenStorage.clear();
    },

    async checkAuth() {
        const auth = tokenStorage.get();
        if (!auth) {
            // Not signed in yet: go to the login page without a notification.
            throw { message: false };
        }
        if (isExpired(auth)) {
            tokenStorage.clear();
            throw { message: 'app.auth.expired' };
        }
    },

    async checkError(error: { status?: number }) {
        // 401 — token invalid or expired; 403 is a missing scope, the session stays.
        if (error?.status === 401) {
            tokenStorage.clear();
            throw new Error('ra.notification.logged_out');
        }
    },

    async getIdentity() {
        const auth = tokenStorage.get();
        if (!auth) throw new Error('app.auth.expired');
        return { id: auth.clientId, fullName: auth.clientId };
    },

    async getPermissions() {
        return tokenStorage.get()?.scopes ?? [];
    },
};

export function hasScope(permissions: unknown, scope: string): boolean {
    return Array.isArray(permissions) && permissions.includes(scope);
}
