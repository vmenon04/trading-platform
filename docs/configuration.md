# Configuration

## Machines and ports

Only ports **9092** and **8080–8100** on the Linux machine are reachable from the VM.

| Machine | Port | What |
|---|---|---|
| Linux (`10.18.64.177`) | 8080 | Jenkins (host service, multibranch job `ftrade`) |
| | 8081 | Trade API (`api` container → 8080 inside) |
| | 8082 | Postgres (`trading-postgres` → 5432 inside) |
| | 8085 | SonarQube (standalone container, not in compose) |
| | 9092 | Kafka (`trading-kafka`, KRaft; controller on 9093 inside only) |
| Windows VM | 8081 | The app when run locally (`server.port`; 8080 is taken by another Jenkins) |
| Local | 8050 | Reporting API (`reporting/scripts/api.py`, 127.0.0.1) |
| | 4200 | Reporting dashboard dev server (`ng serve`, proxies `/api` to 8050) |

## Docker Compose (`../docker-compose.yml`, Linux machine)

| Service | Image | Does |
|---|---|---|
| `kafka` | `apache/kafka:3.9.1` | Broker |
| `postgres` | `postgres:16-alpine` | Database; runs `schema.sql` + `sample-data.sql` once, only when its volume is empty |
| `api` | `trading-platform-app` | `POST /accounts/{accountId}/trades` → `trade.submitted` |
| `validation` | same | `trade.submitted` → `trade.validated` |
| `recording` | same | `trade.validated` → `trade.recorded` |
| `execution` | same | `trade.recorded` → `trade.finished` |

All four app services use the one image built from `Dockerfile`; each switches on only its own stage. Scale one with `docker compose up -d --scale execution=3` (at most 8 per stage get work, one per partition). Reset the database with `docker compose down -v`.

### `.env` (read by compose and the reporting scripts)

| Variable | Used for |
|---|---|
| `HOST_IP` | Linux machine's private IP: ports are published on it, Kafka advertises it |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Database name and login |
| `POSTGRES_PORT` | Host port for Postgres (`8082`) |
| `API_PORT` | Host port for the `api` service (`8081`) |
| `MARKET_DATA_BASE_URL`, `MARKET_DATA_API_KEY` | Fauxnance price API |
| `MISSION_DB_URL`, `ANALYTICS_DB_URL` | Reporting: trading database (read-only) and analytics database. Not in `.env.example` yet |

### Environment variables set on the app containers

| Variable | Value |
|---|---|
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `${HOST_IP}:9092` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/${POSTGRES_DB}` |
| `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | from `.env` |
| `SPRING_SQL_INIT_MODE` | `never` (Postgres creates the schema; `schema.sql` drops every table) |
| `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` | `5` (Postgres allows 100 connections) |
| `MARKET_DATA_BASE_URL`, `MARKET_DATA_API_KEY` | from `.env` |
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
