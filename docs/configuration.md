# Configuration

## Machines and ports

Only ports **9092** and **8080–8100** on the Linux machine are reachable from the VM.

| Machine | Port | What |
|---|---|---|
| Linux (`10.18.64.177`) | 8080 | Jenkins (host service, multibranch job `ftrade`) |
| | 8081 | Trade API (`api` container → 8080 inside) |
| | 8082 | Postgres (`trading-postgres` → 5432 inside) |
| | 8083 | Redis price cache (`trading-redis` → 6379 inside) |
| | 8085 | SonarQube (standalone container, not in compose) |
| | 8086 | Redis Insight, web UI for Redis (`trading-redis-insight`, `tools` profile only) |
| | 8087 | Kafbat UI, web UI for Kafka (`trading-kafka-ui`, `tools` profile only) |
| | 8088 | Adminer, web UI for Postgres (`trading-adminer`, `tools` profile only) |
| | 9092 | Kafka (`trading-kafka`, KRaft; controller on 9093 inside only) |
| Windows VM | 8081 | The app when run locally (`server.port`; 8080 is taken by another Jenkins) |
| Local | 8050 | Reporting API (`reporting/scripts/api.py`, 127.0.0.1) |
| | 4200 | Reporting dashboard dev server (`ng serve`, proxies `/api` to 8050) |

## Docker Compose (`../docker-compose.yml`, Linux machine)

| Service | Image | Does |
|---|---|---|
| `kafka` | `apache/kafka:3.9.1` | Broker |
| `postgres` | `postgres:16-alpine` | Database; runs `schema.sql` + `sample-data.sql` once, only when its volume is empty |
| `redis` | `redis:7.4-alpine` | Fauxnance price cache; password-protected, nothing saved to disk |
| `api` | `trading-platform-app` | `POST /accounts/{accountId}/trades` → `trade.submitted` |
| `validation` | same | `trade.submitted` → `trade.validated` |
| `recording` | same | `trade.validated` → `trade.recorded` |
| `execution` | same | `trade.recorded` → `trade.finished` |
| `redis-insight` | `redis/redisinsight:3.8` | Web UI for Redis, already connected to `redis`. `tools` profile only |
| `kafka-ui` | `ghcr.io/kafbat/kafka-ui:v1.5.0` | Web UI for Kafka topics, messages and consumer groups (read-only). `tools` profile only |
| `adminer` | `adminer:6.1.2` | Web UI for Postgres: browse tables, run SQL. Log in with `POSTGRES_USER` / `POSTGRES_PASSWORD`. `tools` profile only |

All four app services use the one image built from `Dockerfile`; each switches on only its own stage. Scale one with `docker compose up -d --scale execution=3` (at most 8 per stage get work, one per partition). Reset the database with `docker compose down -v`.

`docker compose up -d` leaves out the three web UIs; `docker compose --profile tools up -d` starts them too. Redis Insight and Kafka UI have no login.

### `.env` (read by compose and the reporting scripts)

| Variable | Used for |
|---|---|
| `HOST_IP` | Linux machine's private IP: ports are published on it, Kafka advertises it |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Database name and login |
| `POSTGRES_PORT` | Host port for Postgres (`8082`) |
| `API_PORT` | Host port for the `api` service (`8081`) |
| `MARKET_DATA_BASE_URL`, `MARKET_DATA_API_KEY` | Fauxnance price API |
| `MARKET_DATA_CACHE_TTL` | How long a price stays cached (`5m`) |
| `REDIS_PORT`, `REDIS_PASSWORD` | Host port (`8083`) and password for Redis |
| `REDIS_INSIGHT_PORT`, `KAFKA_UI_PORT`, `ADMINER_PORT` | Host ports for the web UIs (`8086`, `8087`, `8088`) |
| `MISSION_DB_URL`, `ANALYTICS_DB_URL` | Reporting: trading database (read-only) and analytics database. Not in `.env.example` yet |

### Environment variables set on the app containers

| Variable | Value |
|---|---|
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `${HOST_IP}:9092` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/${POSTGRES_DB}` |
| `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | from `.env` |
| `SPRING_SQL_INIT_MODE` | `never` (Postgres creates the schema; `schema.sql` drops every table) |
| `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` | `5` (Postgres allows 100 connections) |
| `MARKET_DATA_BASE_URL`, `MARKET_DATA_API_KEY`, `MARKET_DATA_CACHE_TTL` | from `.env` |
| `SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PORT` | `redis`, `6379` |
| `SPRING_DATA_REDIS_PASSWORD` | `${REDIS_PASSWORD}` |
| `MANAGEMENT_HEALTH_REDIS_ENABLED` | `false` (Redis is only a cache, so it doesn't decide whether the app is UP) |
| `TRADE_STAGE_{API,VALIDATION,RECORDING,EXECUTION}_ENABLED` | `true` for the service's own stage, `false` for the rest |

## `application.properties` (running the app outside Docker)

Note that every property can also be set as an environment variable (`spring.kafka.bootstrap-servers` → `SPRING_KAFKA_BOOTSTRAP_SERVERS`).

| Property | Meaning |
|---|---|
| `server.port` | HTTP port (`8081` on the VM) |
| `spring.kafka.bootstrap-servers` | `10.18.64.177:9092` |
| `spring.datasource.url` / `.username` / `.password` | `jdbc:postgresql://10.18.64.177:8082/trading` |
| `spring.sql.init.mode` | `always` drops and reloads every table on start. Use `never` against the shared database |
| `spring.sql.init.data-locations` | `classpath:sample-data.sql` (with `always`) |
| `market.data.base-url`, `market.data.api-key` | Fauxnance |
| `market.data.cache-ttl` | How long a price stays in the Redis cache, e.g. `5m` |
| `spring.data.redis.host` / `.port` / `.password` | `10.18.64.177`, `8083`, and `REDIS_PASSWORD` from `.env` |
| `management.health.redis.enabled` | `false`: if Redis is down, prices come straight from Fauxnance |
| `trade.stage.{api,validation,recording,execution}.enabled` | Which stages run; unset counts as `true` |
| `spring.kafka.listener.concurrency` | Optional: listener threads per stage (default 1) |

## Kafka

| Topic | Partitions | Producer → consumer group |
|---|---|---|
| `trade.submitted` | 8 | API → `validation-service` |
| `trade.validated` | 8 | validation → `recording-service` |
| `trade.recorded` | 8 | recording → `execution-service` |
| `trade.finished` | 8 | execution → (none yet) |

Created by `KafkaTopicConfig` at startup (`PARTITIONS = 8`, replication 1). The first three are keyed by account ID, so each account's trades stay in order; `trade.finished` is keyed by trade ID.

## CI (Jenkins, `Jenkinsfile`)

Builds are started manually. Stages: Build Image → Smoke-Test → (Code-Coverage, Static-Analysis, Reporting-Tests in parallel) → SonarQube.

| Item | Value |
|---|---|
| Jenkins tools | Maven `Maven3`, SonarQube scanner `Sonar`, server `SonarQube` |
| Image | `fintech-five:<branch>` |
| Smoke test | Throwaway `postgres:17-alpine` + the image with `SPRING_SQL_INIT_MODE=always`, Kafka listeners off (`SPRING_KAFKA_LISTENER_AUTO_STARTUP=false`), dummy market data; waits for `/actuator/health` UP |
| Static analysis | Checkstyle (`config/checkstyle.xml`) + SpotBugs; new issues mark the build UNSTABLE |
| Coverage | JaCoCo (`target/site/jacoco/jacoco.xml`) |
| SonarQube | Project key `fintech-five`, at `http://10.18.64.177:8085` |
| Reporting tests | pytest in a `python:3.14` container |
