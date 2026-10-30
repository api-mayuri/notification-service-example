# Notes for AI agents

Human docs live in `README.md` and `docs/`. Agent working notes live in `ai/` (session log, original plan, research). Update `ai/progress.md` after significant changes; do not put agent notes into `docs/`.

## Build and test

- **Java 25 is required.** Do not downgrade: on JDK 21 virtual threads pinned in `synchronized` Kafka client code deadlock on shutdown/rebalance (fixed in JDK 24, JEP 491).
- Locally JDK 25 is at `~/.jdks/ms-25.0.4.1`: `JAVA_HOME=$HOME/.jdks/ms-25.0.4.1 ./mvnw -o -B verify`.
- `verify` runs integration tests (`*IT`) with Testcontainers: Docker must be running. Full run takes ~3 min.
- Hung IT run → thread dump with `jcmd <pid> Thread.dump_to_file` (shows virtual threads; `jstack` does not).

## Frontend (`frontend/`)

- react-admin 5, MUI 9, Vite 8, TypeScript 7. MUI 9 has no system props (`mt`, `display`, `flexWrap`…) on Box/Typography/Stack: use `sx`. Checks: `cd frontend && npm ci && npm test && npm run build` (build includes `tsc`).
- Human docs: `docs/frontend.md`. Backend DTOs are mirrored in `src/api/types.ts`; REST changes must update it and `src/api/dataProvider.ts`.
- UI strings live in `src/i18n/{ru,en}.ts`; `i18n.test.ts` fails if a static `app.*` key used in code is missing.
- Channel providers shown in the UI: `src/channels/providers/{builtIn,planned}.ts`. New backend provider → move it to `builtIn.ts`.
- Settings: runtime `config.js` written from env by `docker/40-app-config.sh` (Docker) or `VITE_*` (dev). Adding a setting: `config.ts`, the script's variable list, compose, `.env.example`, docs.
- `useDataProvider()` returns a new proxy on every render: never put it in effect deps. Do not set `requireAuth` on `<Admin>` (loops with the custom login page); wrap custom routes in `<Authenticated>`.
- Screenshots in `docs/screenshots/` (English UI): `frontend/scripts/screenshots.cjs`; retake them after visible UI changes.
- The backend has no CORS: the UI talks to it through nginx (`docker/nginx.conf.template`) or the Vite proxy.

## Architecture rules (enforced by ArchUnit)

- `domain` depends only on the JDK; `application` only on `domain` + `spring-tx`; use cases are wired in `bootstrap/config/UseCaseConfiguration`.
- `infrastructure.in..` and `infrastructure.out..` must not depend on each other; infrastructure must not depend on bootstrap.
- Naming: `*Controller` in `in.rest`, `*JpaEntity` in `out.persistence.entity`, `*ChannelSender` in `out.channel`. Constructor injection only.

## Pitfalls

- Keep `spring.modulith.events.kafka.enable-json: false`. Otherwise Modulith registers a JSON `RecordMessageConverter` and Kafka gets the whole `ExternalMessage` wrapper instead of its payload: every delivery goes to DLT and notifications stay `ACCEPTED`.
- Liquibase changesets are immutable once applied: add a new numbered file under `db/changelog/changes/` and include it in the master changelog. Every schema change needs a rollback.
- One retry policy (`notification.retry.*`) drives both Kafka retry topics and the domain attempt counter; keep them in sync.
- Kafka listener factories: build custom ones through Boot's `ConcurrentKafkaListenerContainerFactoryConfigurer` so they inherit Boot settings.
- Delivery topic messages (`notification.delivery.*`) are internal; `notification.events.v1` and `notification.send.requests.v1` are public contracts (`docs/asyncapi.yaml`). Breaking changes → new `.v2` type.

## Conventions

- Commits: conventional style (`fix:`, `feat:`, `docs:`), no `Co-Authored-By` trailer.
- New code comments: short, in English.
- Do not commit `.claude/settings.local.json`.
