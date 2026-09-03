-- Module 09 Lab — Hardened DDL for the Mission Model
-- Start from Module 08's first-draft DDL and add NOT NULL, UNIQUE, CHECK constraints,
-- and indexes on FK columns. This DDL is run in the `mission` schema in Module 13.

-- Part A: close the gap — add a client_holdings table

DROP TABLE IF EXISTS client_subscriptions CASCADE;
DROP TABLE IF EXISTS model_portfolio_holdings CASCADE;
DROP TABLE IF EXISTS model_portfolios CASCADE;
DROP TABLE IF EXISTS client_holdings CASCADE;
DROP TABLE IF EXISTS client_trades CASCADE;
DROP TABLE IF EXISTS instruments CASCADE;
DROP TABLE IF EXISTS clients CASCADE;

-- clients
CREATE TABLE clients (
    client_id SERIAL PRIMARY KEY,
    name TEXT NOT NULL
    --advisor_id INT REFERENCES advisors(advisor_id)
);

-- instruments
CREATE TABLE instruments (
    instrument_id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    ticker TEXT NOT NULL UNIQUE
);

CREATE TABLE client_holdings (
    client_id INT REFERENCES clients(client_id),
    instrument_id INT REFERENCES instruments(instrument_id),
    as_of_date DATE NOT NULL,
    PRIMARY KEY (client_id, instrument_id, as_of_date),
    quantity INT NOT NULL    
        CHECK (quantity >= 0),
    status TEXT CHECK (status IN ('active', 'inactive')) NOT NULL
    
);

CREATE INDEX idx_client_holdings_client_id ON client_holdings(client_id);
CREATE INDEX idx_client_holdings_instrument_id ON client_holdings(instrument_id);

CREATE TABLE client_trades (
    trade_id SERIAL PRIMARY KEY,
    client_id INT REFERENCES clients(client_id),
    instrument_id INT REFERENCES instruments(instrument_id),
    trade_time DATE NOT NULL,
    trade_type TEXT NOT NULL
        CHECK (trade_type IN ('BUY', 'SELL')),
    quantity NUMERIC(14, 4) NOT NULL
        CHECK (quantity > 0),
    price NUMERIC(14, 4) NOT NULL
        CHECK (price > 0)
);

CREATE INDEX idx_client_trades_client_id ON client_trades(client_id);
CREATE INDEX idx_client_trades_instrument_id ON client_trades(instrument_id);

-- model_portfolios
CREATE TABLE model_portfolios (
    model_portfolio_id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
);

CREATE INDEX idx_model_portfolios_status ON model_portfolios(status);

-- model_portfolio_holdings
CREATE TABLE model_portfolio_holdings (
    model_portfolio_id INT REFERENCES model_portfolios(model_portfolio_id),
    instrument_id INT REFERENCES instruments(instrument_id),
    effective_date DATE NOT NULL,
    PRIMARY KEY (model_portfolio_id, instrument_id, effective_date),
    target_weight_pct NUMERIC(5,2) NOT NULL
        CHECK (target_weight_pct >= 0 AND target_weight_pct <= 100),
    status TEXT CHECK (status IN ('active', 'inactive')) NOT NULL
);

CREATE INDEX idx_model_portfolio_holdings_model_portfolio_id ON model_portfolio_holdings(model_portfolio_id);
CREATE INDEX idx_model_portfolio_holdings_instrument_id ON model_portfolio_holdings(instrument_id);

-- client_subscriptions
CREATE TABLE client_subscriptions (
    client_id INT REFERENCES clients(client_id),
    model_portfolio_id INT REFERENCES model_portfolios(model_portfolio_id),
    subscription_date DATE NOT NULL,
    PRIMARY KEY (client_id, model_portfolio_id, subscription_date),
    status TEXT CHECK (status IN ('active', 'inactive')) NOT NULL
);

CREATE INDEX idx_client_subscriptions_client_id ON client_subscriptions(client_id);
CREATE INDEX idx_client_subscriptions_model_portfolio_id ON client_subscriptions(model_portfolio_id);
CREATE INDEX idx_client_subscriptions_status ON client_subscriptions(status);

-- Part B: constraints
-- Add NOT NULL where appropriate, a UNIQUE constraint on instruments.ticker, a CHECK on
-- model_portfolio_holdings.target_weight_pct (0-100), and a CHECK on client_holdings.quantity
-- (>= 0).


-- Part C: indexes
-- Add an index on every foreign key column. Then identify one additional column worth
-- indexing, and one you would deliberately leave unindexed — with reasoning for both.


-- Part D: prove it works
-- Run this against a real Postgres database, then try to insert a row that violates one of
-- your constraints and note the actual error message.
