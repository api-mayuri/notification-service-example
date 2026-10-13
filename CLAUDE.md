# Notes for AI agents

Human docs live in `README.md` and `docs/`. Agent working notes live in `ai/` (session log, original plan, research). Update `ai/progress.md` after significant changes; do not put agent notes into `docs/`.

## Build and test

- **Java 25 is required.** Do not downgrade: on JDK 21 virtual threads pinned in `synchronized` Kafka client code deadlock on shutdown/rebalance (fixed in JDK 24, JEP 491).
- Locally JDK 25 is at `~/.jdks/ms-25.0.4.1`: `JAVA_HOME=$HOME/.jdks/ms-25.0.4.1 ./mvnw -o -B verify`.
- `verify` runs integration tests (`*IT`) with Testcontainers: Docker must be running. Full run takes ~3 min.
- Hung IT run → thread dump with `jcmd <pid> Thread.dump_to_file` (shows virtual threads; `jstack` does not).

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
