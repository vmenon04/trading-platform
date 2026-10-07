CREATE TABLE IF NOT EXISTS clients (
    client_id  INT PRIMARY KEY,
    first_name TEXT NOT NULL,
    last_name  TEXT NOT NULL,
    email      TEXT NOT NULL,
    username   TEXT
);

CREATE TABLE IF NOT EXISTS accounts (
    account_id INT PRIMARY KEY,
    balance    NUMERIC(14, 4) NOT NULL
);

ALTER TABLE accounts DROP COLUMN IF EXISTS account_type;

CREATE TABLE IF NOT EXISTS client_accounts (
    client_id  INT NOT NULL,
    account_id INT NOT NULL,
    PRIMARY KEY (client_id, account_id)
);

CREATE TABLE IF NOT EXISTS instruments (
    instrument_id INT PRIMARY KEY,
    name          TEXT NOT NULL,
    ticker        TEXT NOT NULL,
    asset_class   TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS account_holdings (
    account_id    INT NOT NULL,
    instrument_id INT NOT NULL,
    as_of_date    TIMESTAMP NOT NULL,
    quantity      NUMERIC(18, 8) NOT NULL,
    status        TEXT NOT NULL,
    PRIMARY KEY (account_id, instrument_id, as_of_date)
);

CREATE TABLE IF NOT EXISTS trades (
    trade_id      INT PRIMARY KEY,
    account_id    INT NOT NULL,
    instrument_id INT NOT NULL,
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
    trade_id    INT NOT NULL,
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
