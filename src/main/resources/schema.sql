DROP TABLE IF EXISTS account_subscriptions CASCADE;
DROP TABLE IF EXISTS model_portfolio_holdings CASCADE;
DROP TABLE IF EXISTS model_portfolios CASCADE;
DROP TABLE IF EXISTS account_holdings CASCADE;
DROP TABLE IF EXISTS trade_total_price CASCADE;
DROP TABLE IF EXISTS account_trade_price CASCADE;
DROP TABLE IF EXISTS account_trade_status CASCADE;
DROP TABLE IF EXISTS account_trades CASCADE;
DROP TABLE IF EXISTS client_accounts CASCADE;
DROP TABLE IF EXISTS accounts CASCADE;
DROP TABLE IF EXISTS instruments CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS clients CASCADE;
DROP TABLE IF EXISTS roles CASCADE;

-- Old client-level tables, replaced by the account-level ones above
DROP TABLE IF EXISTS client_subscriptions CASCADE;
DROP TABLE IF EXISTS client_holdings CASCADE;
DROP TABLE IF EXISTS client_trades CASCADE;

-- clients
CREATE TABLE clients (
    client_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    external_client_id uuid,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    --advisor_id INT REFERENCES advisors(advisor_id)
    birth_date DATE NOT NULL
);

-- users: login accounts. a CLIENT user is linked to exactly one client, an ADMIN user to none
CREATE TABLE users (
    user_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    external_user_id uuid,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    client_id BIGINT REFERENCES clients(client_id),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE roles (
    user_id BIGINT REFERENCES users(user_id),
    role TEXT NOT NULL,
    CHECK (role IN ('ADMIN', 'CLIENT', 'ANALYST')),
    PRIMARY KEY (user_id, role)
);

CREATE TABLE accounts (
    account_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    external_account_id uuid,
    balance NUMERIC(14, 4) NOT NULL
);

CREATE TABLE client_accounts (
    client_id BIGINT REFERENCES clients(client_id),
    account_id BIGINT REFERENCES accounts(account_id),
    PRIMARY KEY (client_id, account_id)
);

-- instruments
CREATE TABLE instruments (
    instrument_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name TEXT NOT NULL,
    ticker TEXT NOT NULL
        CHECK (UPPER(ticker) = ticker),
    asset_class TEXT NOT NULL
);

CREATE TABLE account_holdings (
    account_id BIGINT REFERENCES accounts(account_id),
    instrument_id BIGINT REFERENCES instruments(instrument_id),
    as_of_date TIMESTAMP NOT NULL,
    PRIMARY KEY (account_id, instrument_id, as_of_date),
    quantity NUMERIC(18, 8) NOT NULL    
        CHECK (quantity >= 0),
    status TEXT CHECK (status IN ('ACTIVE', 'INACTIVE')) NOT NULL
    
);

CREATE INDEX idx_account_holdings_account_id ON account_holdings(account_id);
CREATE INDEX idx_account_holdings_instrument_id ON account_holdings(instrument_id);

CREATE TABLE account_trades (
    trade_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    external_trade_id uuid,
    account_id BIGINT REFERENCES accounts(account_id) NOT NULL,
    instrument_id BIGINT REFERENCES instruments(instrument_id) NOT NULL,
    trade_side TEXT NOT NULL
        CHECK (trade_side IN ('BUY', 'SELL')),
    -- make this the precision as account_holdings.quantity
    quantity NUMERIC(18, 8) NOT NULL
        CHECK (quantity > 0)
);

CREATE TABLE account_trade_status (
    trade_id BIGINT REFERENCES account_trades(trade_id),
    status TEXT CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'FULFILLED')) NOT NULL,
    PRIMARY KEY (trade_id, status),
    trade_time TIMESTAMPTZ NOT NULL
);

CREATE TABLE account_trade_price (
    trade_id BIGINT PRIMARY KEY REFERENCES account_trades(trade_id),
    price_per_unit NUMERIC(18, 8) NOT NULL
        CHECK (price_per_unit >= 0),
    total_price NUMERIC(18, 8) NOT NULL
        CHECK (total_price >= 0)
);

CREATE INDEX idx_account_trades_account_id ON account_trades(account_id);
CREATE INDEX idx_account_trades_instrument_id ON account_trades(instrument_id);
CREATE INDEX idx_account_trade_status_status ON account_trade_status(status);

CREATE TABLE model_portfolios (
    model_portfolio_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name TEXT NOT NULL
);

-- model_portfolio_holdings
CREATE TABLE model_portfolio_holdings (
    model_portfolio_id BIGINT REFERENCES model_portfolios(model_portfolio_id),
    instrument_id BIGINT REFERENCES instruments(instrument_id),
    effective_date DATE NOT NULL,
    PRIMARY KEY (model_portfolio_id, instrument_id, effective_date),
    target_weight_pct NUMERIC(5,2) NOT NULL
        CHECK (target_weight_pct >= 0 AND target_weight_pct <= 100),
    status TEXT CHECK (status IN ('ACTIVE', 'INACTIVE')) NOT NULL
);

CREATE INDEX idx_model_portfolio_holdings_model_portfolio_id ON model_portfolio_holdings(model_portfolio_id);
CREATE INDEX idx_model_portfolio_holdings_instrument_id ON model_portfolio_holdings(instrument_id);
CREATE INDEX idx_model_portfolio_holdings_status ON model_portfolio_holdings(status);

-- account_subscriptions
CREATE TABLE account_subscriptions (
    account_id BIGINT REFERENCES accounts(account_id) NOT NULL,
    model_portfolio_id BIGINT REFERENCES model_portfolios(model_portfolio_id),
    subscription_date DATE NOT NULL,
    unsubscribe_date DATE,
    PRIMARY KEY (account_id, model_portfolio_id, subscription_date),
    status TEXT CHECK (status IN ('ACTIVE', 'INACTIVE')) NOT NULL
);

CREATE INDEX idx_account_subscriptions_account_id ON account_subscriptions(account_id);
CREATE INDEX idx_account_subscriptions_model_portfolio_id ON account_subscriptions(model_portfolio_id);
CREATE INDEX idx_account_subscriptions_status ON account_subscriptions(status);