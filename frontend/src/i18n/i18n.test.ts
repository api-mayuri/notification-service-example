import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { en } from './en';
import { ru } from './ru';
import { messages } from './index';

type Tree = { [key: string]: string | Tree };

function keys(tree: Tree, prefix = ''): string[] {
    return Object.entries(tree).flatMap(([key, value]) =>
        typeof value === 'string' ? [`${prefix}${key}`] : keys(value, `${prefix}${key}.`),
    );
}

function sources(dir: string): string[] {
    return readdirSync(dir).flatMap(name => {
        const path = join(dir, name);
        if (statSync(path).isDirectory()) return sources(path);
        return /\.tsx?$/.test(name) && !name.endsWith('.test.ts') && !path.includes('i18n') ? [path] : [];
    });
}

describe('i18n', () => {
    it('ru and en have the same keys', () => {
        expect(keys(en as unknown as Tree).sort()).toEqual(keys(ru as unknown as Tree).sort());
    });

    it('every static app.* key used in the code exists', () => {
        const known = new Set(keys(ru as unknown as Tree));
        const used = new Set<string>();
        for (const file of sources(join(__dirname, '..'))) {
            for (const match of readFileSync(file, 'utf8').matchAll(/['"`](app\.[a-z_]+(?:\.[A-Za-z_-]+)+)['"`]/g)) {
                used.add(match[1]);
            }
        }
        const missing = [...used].filter(key => !known.has(key));
        expect(missing).toEqual([]);
    });

    it('falls back to English for react-admin keys missing in Russian', () => {
        const ra = (messages.ru as unknown as Tree).ra as Tree;
        expect(typeof (ra.action as Tree).save).toBe('string');
        expect(((messages.ru as unknown as Tree).app as Tree).menu).toBeDefined();
    });
});
