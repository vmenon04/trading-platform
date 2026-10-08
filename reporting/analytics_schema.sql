CREATE TABLE IF NOT EXISTS clients (
    client_id  BIGINT PRIMARY KEY,
    first_name TEXT NOT NULL,
    last_name  TEXT NOT NULL,
    email      TEXT NOT NULL,
    username   TEXT
);

CREATE TABLE IF NOT EXISTS accounts (
    account_id BIGINT PRIMARY KEY,
    balance    NUMERIC(14, 4) NOT NULL
);

ALTER TABLE accounts DROP COLUMN IF EXISTS account_type;

CREATE TABLE IF NOT EXISTS client_accounts (
    client_id  BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    PRIMARY KEY (client_id, account_id)
);

CREATE TABLE IF NOT EXISTS instruments (
    instrument_id BIGINT PRIMARY KEY,
    name          TEXT NOT NULL,
    ticker        TEXT NOT NULL,
    asset_class   TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS account_holdings (
    account_id    BIGINT NOT NULL,
    instrument_id BIGINT NOT NULL,
    as_of_date    TIMESTAMP NOT NULL,
    quantity      NUMERIC(18, 8) NOT NULL,
    status        TEXT NOT NULL,
    PRIMARY KEY (account_id, instrument_id, as_of_date)
);

CREATE TABLE IF NOT EXISTS trades (
    trade_id      BIGINT PRIMARY KEY,
    account_id    BIGINT NOT NULL,
    instrument_id BIGINT NOT NULL,
    trade_type    TEXT NOT NULL,
    quantity      NUMERIC(18, 8) NOT NULL,
    price         NUMERIC(18, 8),
    notional      NUMERIC(36, 16),
    trade_time    TIMESTAMPTZ NOT NULL,
    status        TEXT NOT NULL,
    status_time   TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_trades_account_id ON trades(account_id);
CREATE INDEX IF NOT EXISTS idx_trades_instrument_id ON trades(instrument_id);
CREATE INDEX IF NOT EXISTS idx_trades_trade_time ON trades(trade_time);

CREATE TABLE IF NOT EXISTS trade_status_history (
    trade_id    BIGINT NOT NULL,
    status      TEXT NOT NULL,
    status_time TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (trade_id, status)
);

CREATE TABLE IF NOT EXISTS sync_runs (
    run_id        SERIAL PRIMARY KEY,
    started_at    TIMESTAMPTZ NOT NULL,
    finished_at   TIMESTAMPTZ NOT NULL,
    trades_copied INT NOT NULL,
    watermark     TIMESTAMPTZ
);

-- The trading database switched its ids from INT to BIGINT. CREATE TABLE IF NOT EXISTS
-- leaves existing tables alone, so widen the id columns here (does nothing if already BIGINT).
ALTER TABLE clients              ALTER COLUMN client_id     TYPE BIGINT;
ALTER TABLE accounts             ALTER COLUMN account_id    TYPE BIGINT;
ALTER TABLE client_accounts      ALTER COLUMN client_id     TYPE BIGINT, ALTER COLUMN account_id TYPE BIGINT;
ALTER TABLE instruments          ALTER COLUMN instrument_id TYPE BIGINT;
ALTER TABLE account_holdings     ALTER COLUMN account_id    TYPE BIGINT, ALTER COLUMN instrument_id TYPE BIGINT;
ALTER TABLE trades               ALTER COLUMN trade_id      TYPE BIGINT, ALTER COLUMN account_id TYPE BIGINT,
                                 ALTER COLUMN instrument_id TYPE BIGINT;
ALTER TABLE trade_status_history ALTER COLUMN trade_id      TYPE BIGINT;
