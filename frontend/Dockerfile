# syntax=docker/dockerfile:1.7
# Build: Node 22, npm cache reused between builds. Tests and typecheck run before the bundle.
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund
COPY . .
RUN npm test && npm run build

# Runtime: nginx serves the bundle and proxies the API, so the browser sees one origin.
# Settings come from environment variables at container start (docker/40-app-config.sh).
FROM nginx:1.29-alpine
ENV BACKEND_URL=http://notification-service:8080 \
    NGINX_RESOLVER=127.0.0.11
COPY docker/nginx.conf.template /etc/nginx/templates/default.conf.template
COPY docker/40-app-config.sh /docker-entrypoint.d/40-app-config.sh
COPY --from=build /app/dist /usr/share/nginx/html
RUN chmod +x /docker-entrypoint.d/40-app-config.sh
EXPOSE 80
HEALTHCHECK --interval=10s --timeout=3s --retries=5 CMD wget -qO- http://127.0.0.1/healthz >/dev/null || exit 1
