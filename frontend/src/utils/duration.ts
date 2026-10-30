/**
 * Durations for template deduplication windows. The backend takes ISO 8601 (PT10M); the form
 * also accepts shorthands like "10m", "2h", "1d", "90s".
 */
const ISO = /^P(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?)?$/i;
const SHORT = /^(\d+)\s*(s|m|h|d)$/i;

export function durationToSeconds(value: string | number | null | undefined): number | null {
    if (value === null || value === undefined || value === '') return null;
    if (typeof value === 'number') return value;
    const trimmed = value.trim();
    const iso = ISO.exec(trimmed);
    if (iso && trimmed.toUpperCase() !== 'P' && !trimmed.toUpperCase().endsWith('T')) {
        const [, d, h, m, s] = iso;
        return Number(d ?? 0) * 86400 + Number(h ?? 0) * 3600 + Number(m ?? 0) * 60 + Number(s ?? 0);
    }
    const short = SHORT.exec(trimmed);
    if (short) {
        const factor = { s: 1, m: 60, h: 3600, d: 86400 }[short[2].toLowerCase() as 's' | 'm' | 'h' | 'd'];
        return Number(short[1]) * factor;
    }
    if (/^\d+(\.\d+)?$/.test(trimmed)) return Number(trimmed);
    return null;
}

export function secondsToIso(seconds: number): string {
    if (seconds <= 0) return 'PT0S';
    const days = Math.floor(seconds / 86400);
    let rest = seconds - days * 86400;
    const hours = Math.floor(rest / 3600);
    rest -= hours * 3600;
    const minutes = Math.floor(rest / 60);
    const secs = rest - minutes * 60;
    const time = `${hours ? `${hours}H` : ''}${minutes ? `${minutes}M` : ''}${secs ? `${secs}S` : ''}`;
    return `P${days ? `${days}D` : ''}${time ? `T${time}` : ''}`;
}

/** Form value → ISO duration or null (empty — deduplication off). Throws on garbage. */
export function normalizeDuration(value: string | null | undefined): string | null {
    if (value === null || value === undefined || value.trim() === '') return null;
    const seconds = durationToSeconds(value);
    if (seconds === null) throw new Error('app.validation.duration');
    return secondsToIso(seconds);
}

/** Backend limit for deduplication windows (Template.MAX_DEDUPLICATION_WINDOW). */
export const MAX_DEDUPLICATION_SECONDS = 86400;

export const validateDuration = (value: string | null | undefined) => {
    if (value === null || value === undefined || value.trim() === '') return undefined;
    const seconds = durationToSeconds(value);
    if (seconds === null) return 'app.validation.duration';
    return seconds <= 0 || seconds > MAX_DEDUPLICATION_SECONDS ? 'app.validation.duration_range' : undefined;
};

export function humanizeDuration(value: string | number | null | undefined): string {
    const seconds = durationToSeconds(value ?? null);
    if (seconds === null) return '—';
    const parts: string[] = [];
    let rest = seconds;
    for (const [unit, size] of [
        ['d', 86400],
        ['h', 3600],
        ['m', 60],
        ['s', 1],
    ] as const) {
        const amount = Math.floor(rest / size);
        if (amount) parts.push(`${amount}${unit}`);
        rest -= amount * size;
    }
    return parts.join(' ') || '0s';
}
