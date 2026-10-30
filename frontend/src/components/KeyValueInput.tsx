import { ArrayInput, SimpleFormIterator, TextInput, type Validator } from 'react-admin';

/**
 * Editable map as rows {key, value}; convert with entriesToMap() before sending.
 * Used for contacts, template variables and metadata.
 */
export const KeyValueInput = ({
    source,
    label,
    keyLabel = 'app.field.key',
    valueLabel = 'app.field.value',
    helperText,
    validate,
    multilineValue = false,
}: {
    source: string;
    label?: string;
    keyLabel?: string;
    valueLabel?: string;
    helperText?: string;
    validate?: Validator | Validator[];
    multilineValue?: boolean;
}) => (
    <ArrayInput source={source} label={label} helperText={helperText} validate={validate}>
        <SimpleFormIterator inline disableReordering>
            <TextInput source="key" label={keyLabel} helperText={false} />
            <TextInput source="value" label={valueLabel} helperText={false} multiline={multilineValue} sx={{ minWidth: 280 }} />
        </SimpleFormIterator>
    </ArrayInput>
);

export interface Entry {
    key?: string;
    value?: unknown;
}

export function entriesToMap<T = unknown>(entries: Entry[] | undefined, parse?: (value: unknown) => T): Record<string, T> | undefined {
    const result: Record<string, T> = {};
    for (const entry of entries ?? []) {
        const key = entry?.key?.trim();
        if (key) {
            result[key] = parse ? parse(entry.value) : (entry.value as T);
        }
    }
    return Object.keys(result).length ? result : undefined;
}

export function mapToEntries(map: Record<string, unknown> | null | undefined): Entry[] {
    return Object.entries(map ?? {}).map(([key, value]) => ({
        key,
        value: typeof value === 'string' ? value : JSON.stringify(value),
    }));
}

/** Template variables: plain text stays a string; JSON literals (numbers, booleans, objects, arrays) are parsed. */
export function parseVariableValue(value: unknown): unknown {
    if (typeof value !== 'string') return value ?? '';
    const trimmed = value.trim();
    if (/^(-?\d+(\.\d+)?|true|false|null|\{.*\}|\[.*\])$/s.test(trimmed)) {
        try {
            return JSON.parse(trimmed);
        } catch {
            return value;
        }
    }
    return value;
}
