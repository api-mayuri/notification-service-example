import { HttpError } from 'react-admin';
import { config } from '../config';
import { tokenStorage } from '../auth/tokenStorage';
import type { ProblemDetail } from './types';

export interface ApiResponse<T> {
    status: number;
    headers: Headers;
    body: T;
}

export interface RequestOptions {
    method?: string;
    body?: unknown;
    headers?: Record<string, string>;
    query?: Record<string, string | number | boolean | null | undefined>;
    /** Skip the Authorization header (public endpoints like /dev/token). */
    anonymous?: boolean;
    /** Non-2xx statuses whose body is still a result (health returns 503 when DOWN). */
    okStatuses?: number[];
}

export function buildUrl(path: string, query?: RequestOptions['query'], baseUrl = config.apiUrl): string {
    const params = new URLSearchParams();
    for (const [key, value] of Object.entries(query ?? {})) {
        if (value !== undefined && value !== null && value !== '') {
            params.append(key, String(value));
        }
    }
    const search = params.toString();
    return `${baseUrl}${path}${search ? `?${search}` : ''}`;
}

/**
 * Turns a Problem Details response into react-admin's HttpError. Field errors become
 * `body.errors` so react-admin forms show them next to the inputs (server-side validation).
 */
export function toHttpError(status: number, problem: ProblemDetail | null, headers?: Headers): HttpError {
    const code = problem?.errorCode;
    const parts = [code, problem?.detail ?? problem?.title].filter(Boolean);
    if (problem?.missingVariables?.length) {
        parts.push(`[${problem.missingVariables.join(', ')}]`);
    }
    if (status === 429) {
        const retryAfter = headers?.get('Retry-After');
        if (retryAfter) parts.push(`Retry-After: ${retryAfter}s`);
    }
    const message = parts.length ? parts.join(': ') : defaultMessage(status);
    const body: Record<string, unknown> = { ...(problem ?? {}) };
    if (problem?.errors?.length) {
        const fieldErrors: Record<string, string> = {};
        for (const error of problem.errors) {
            fieldErrors[error.field] = error.message;
        }
        body.errors = { ...fieldErrors, root: { serverError: message } };
    }
    return new HttpError(message, status, body);
}

function defaultMessage(status: number): string {
    switch (status) {
        case 401:
            return 'ra.notification.logged_out';
        case 403:
            return 'app.error.forbidden';
        case 404:
            return 'ra.notification.item_doesnt_exist';
        default:
            return `HTTP ${status}`;
    }
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<ApiResponse<T>> {
    const headers: Record<string, string> = { Accept: 'application/json', ...(options.headers ?? {}) };
    if (options.body !== undefined) {
        headers['Content-Type'] = 'application/json';
    }
    const auth = tokenStorage.get();
    if (auth && !options.anonymous) {
        headers.Authorization = `Bearer ${auth.token}`;
    }
    let response: Response;
    try {
        response = await fetch(buildUrl(path, options.query), {
            method: options.method ?? 'GET',
            headers,
            body: options.body === undefined ? undefined : JSON.stringify(options.body),
        });
    } catch (networkError) {
        throw new HttpError(`app.error.network`, 0, { detail: String(networkError) });
    }
    const text = await response.text();
    let body: unknown = null;
    if (text) {
        try {
            body = JSON.parse(text);
        } catch {
            body = text;
        }
    }
    if (!response.ok && !options.okStatuses?.includes(response.status)) {
        const problem = body && typeof body === 'object' ? (body as ProblemDetail) : null;
        throw toHttpError(response.status, problem, response.headers);
    }
    return { status: response.status, headers: response.headers, body: body as T };
}

export const api = {
    get: <T>(path: string, query?: RequestOptions['query']) => apiRequest<T>(path, { query }).then(r => r.body),
    post: <T>(path: string, body?: unknown, headers?: Record<string, string>) =>
        apiRequest<T>(path, { method: 'POST', body: body ?? {}, headers }),
    put: <T>(path: string, body: unknown) => apiRequest<T>(path, { method: 'PUT', body }).then(r => r.body),
    patch: <T>(path: string, body: unknown) => apiRequest<T>(path, { method: 'PATCH', body }).then(r => r.body),
};
