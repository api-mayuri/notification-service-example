import { describe, expect, it } from 'vitest';
import { durationToSeconds, humanizeDuration, normalizeDuration, secondsToIso, validateDuration } from './duration';

describe('durations', () => {
    it('parses ISO 8601, shorthands and seconds', () => {
        expect(durationToSeconds('PT10M')).toBe(600);
        expect(durationToSeconds('P1DT2H')).toBe(93600);
        expect(durationToSeconds('90s')).toBe(90);
        expect(durationToSeconds('2h')).toBe(7200);
        expect(durationToSeconds(300)).toBe(300);
        expect(durationToSeconds('PT')).toBeNull();
        expect(durationToSeconds('ten minutes')).toBeNull();
    });

    it('formats ISO durations', () => {
        expect(secondsToIso(600)).toBe('PT10M');
        expect(secondsToIso(86400)).toBe('P1D');
        expect(secondsToIso(3661)).toBe('PT1H1M1S');
        expect(normalizeDuration('10m')).toBe('PT10M');
        expect(normalizeDuration('  ')).toBeNull();
        expect(() => normalizeDuration('soon')).toThrow();
    });

    it('validates the backend range (0, 1 day]', () => {
        expect(validateDuration('PT10M')).toBeUndefined();
        expect(validateDuration('')).toBeUndefined();
        expect(validateDuration('2d')).toBe('app.validation.duration_range');
        expect(validateDuration('0s')).toBe('app.validation.duration_range');
        expect(validateDuration('soon')).toBe('app.validation.duration');
    });

    it('humanizes', () => {
        expect(humanizeDuration('PT1H30M')).toBe('1h 30m');
        expect(humanizeDuration(null)).toBe('—');
    });
});
