import polyglotI18nProvider from 'ra-i18n-polyglot';
import englishMessages from 'ra-language-english';
import russianMessages from 'ra-language-russian';
import type { TranslationMessages } from 'react-admin';
import { config } from '../config';
import { en } from './en';
import { ru } from './ru';

type Tree = { [key: string]: string | Tree };

/** Deep merge: keys missing in `override` fall back to `base` (ra-language-russian lags behind). */
export function deepMerge(base: Tree, override: Tree): Tree {
    const result: Tree = { ...base };
    for (const [key, value] of Object.entries(override)) {
        const existing = result[key];
        result[key] =
            typeof value === 'object' && typeof existing === 'object' ? deepMerge(existing, value) : value;
    }
    return result;
}

export const messages: Record<string, TranslationMessages> = {
    en: deepMerge(englishMessages as unknown as Tree, en as unknown as Tree) as unknown as TranslationMessages,
    ru: deepMerge(
        deepMerge(englishMessages as unknown as Tree, russianMessages as unknown as Tree),
        deepMerge(en as unknown as Tree, ru as unknown as Tree),
    ) as unknown as TranslationMessages,
};

export const i18nProvider = polyglotI18nProvider(
    locale => messages[locale] ?? messages.ru,
    config.defaultLocale,
    [
        { locale: 'ru', name: 'Русский' },
        { locale: 'en', name: 'English' },
    ],
    { allowMissing: true },
);
