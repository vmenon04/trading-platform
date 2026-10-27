-- The analytics database the reporting scripts copy the trading data into (ANALYTICS_DB_URL), on the same Postgres.
-- Postgres runs this once, when its volume is new (see docker-compose.yml); sync.py and consumer.py create its tables.
CREATE DATABASE analytics;
