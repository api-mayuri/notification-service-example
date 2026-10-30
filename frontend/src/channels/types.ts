/**
 * Channel provider descriptors for the UI. The backend selects a provider per channel with
 * `notification.channels.<channel>.provider` (@ConditionalOnProperty on *Provider beans);
 * the descriptors describe what can be selected there and which settings it needs.
 */

/** stub — logging provider that only writes what would be sent (simulated=true). */
export type ProviderKind = 'stub' | 'real';

/** built-in — implemented in the backend; planned — needs a *Provider class (see docs/extending.md). */
export type ProviderAvailability = 'built-in' | 'planned';

export interface ProviderSetting {
    /** Environment variable (compose / .env). */
    env: string;
    /** Spring property bound to the variable. */
    property: string;
    /** i18n key or plain label. */
    label: string;
    secret?: boolean;
    required?: boolean;
    defaultValue?: string;
    placeholder?: string;
    /** i18n key or plain text. */
    help?: string;
    type?: 'text' | 'number' | 'boolean';
}

export interface ProviderDescriptor {
    /** Value of notification.channels.<channel>.provider. */
    id: string;
    channel: string;
    name: string;
    kind: ProviderKind;
    availability: ProviderAvailability;
    /** i18n key. */
    description: string;
    /** Java class: existing for built-in, suggested name for planned. */
    implementation: string;
    docsUrl?: string;
    settings: ProviderSetting[];
}

export interface ChannelInfo {
    channel: string;
    /** Port the provider implements (out/channel/<channel>/<Interface>.java). */
    providerInterface: string;
    /** Message record passed to the provider. */
    messageType: string;
    packageName: string;
    contactType: string;
    /** i18n key. */
    description: string;
}
