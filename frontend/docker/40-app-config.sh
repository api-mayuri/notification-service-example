#!/bin/sh
# Writes runtime settings for the SPA (window.__APP_CONFIG__) from environment variables.
set -eu

target=/usr/share/nginx/html/config.js

# JSON string escaping for backslash and double quote; newlines are dropped.
json_escape() {
    printf '%s' "$1" | tr -d '\n\r' | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g'
}

{
    printf 'window.__APP_CONFIG__ = {\n'
    for name in APP_TITLE API_URL AUTH_MODE DEV_TOKEN_CLIENT_ID DEFAULT_LOCALE POLL_INTERVAL_MS \
        SWAGGER_URL MAILPIT_URL KAFKA_UI_URL EXTRA_CHANNELS CHANNEL_PROVIDERS; do
        value=$(printenv "$name" || true)
        printf '  "%s": "%s",\n' "$name" "$(json_escape "$value")"
    done
    printf '};\n'
} > "$target"

echo "40-app-config.sh: wrote $target"
