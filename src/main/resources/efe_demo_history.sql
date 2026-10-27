-- Demo history for efe (the test login from auth_test_data.sql): a year of past, fulfilled trades, so the
-- dashboard's performance chart has something to show. Made up for the demo: the trades never happened, but each
-- one is priced at that day's real Fauxnance close, so the chart's value and invested lines start together on
-- each trade day.
--
-- Runs once on a new database, after auth_test_data.sql. To add it to an existing one:
--   docker compose exec -T postgres sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
--     < src/main/resources/efe_demo_history.sql
-- It adds to whatever efe already has (cash, holdings) and does nothing if it has run before, so running it again
-- is safe. Everything happens in one transaction.

DO $$
DECLARE
    efe_account BIGINT;
BEGIN
    SELECT account_id INTO efe_account
    FROM accounts
    WHERE external_account_id = 'cfe3fcd1-7520-45a5-9a50-d870d47e3b62';
    IF efe_account IS NULL THEN
        RAISE NOTICE 'efe''s account is missing (run auth_test_data.sql first); no demo history added';
        RETURN;
    END IF;
    IF EXISTS (SELECT 1 FROM account_trades WHERE external_trade_id = 'de000000-0000-4000-8000-000000000001') THEN
        RAISE NOTICE 'efe''s demo history is already there';
        RETURN;
    END IF;

    -- n orders the trades; the price is the unit close that day, total = price * quantity
    CREATE TEMP TABLE demo_trades (
        n INT, external_trade_id UUID, ticker TEXT, side TEXT, quantity NUMERIC, price NUMERIC, total NUMERIC,
        filled_at TIMESTAMPTZ
    ) ON COMMIT DROP;
    INSERT INTO demo_trades VALUES
        ( 1, 'de000000-0000-4000-8000-000000000001', 'AAPL', 'BUY', 100, 269.0500, 26905.0000, TIMESTAMPTZ '2025-11-03 14:30:00+00'),
        ( 2, 'de000000-0000-4000-8000-000000000002', 'VOO', 'BUY', 80, 626.4700, 50117.6000, TIMESTAMPTZ '2025-11-10 14:30:00+00'),
        ( 3, 'de000000-0000-4000-8000-000000000003', 'MSFT', 'BUY', 60, 507.4900, 30449.4000, TIMESTAMPTZ '2025-11-17 14:30:00+00'),
        ( 4, 'de000000-0000-4000-8000-000000000004', 'NVDA', 'BUY', 300, 179.9200, 53976.0000, TIMESTAMPTZ '2025-12-01 14:30:00+00'),
        ( 5, 'de000000-0000-4000-8000-000000000005', 'X:BTC-USD', 'BUY', 0.5, 86419.7813, 43209.8907, TIMESTAMPTZ '2025-12-15 14:30:00+00'),
        ( 6, 'de000000-0000-4000-8000-000000000006', 'FX:EURUSD', 'BUY', 20000, 1.1624, 23248.0000, TIMESTAMPTZ '2026-01-12 14:30:00+00'),
        ( 7, 'de000000-0000-4000-8000-000000000007', 'AAPL', 'BUY', 50, 270.0100, 13500.5000, TIMESTAMPTZ '2026-02-02 14:30:00+00'),
        ( 8, 'de000000-0000-4000-8000-000000000008', 'VOO', 'BUY', 40, 627.9900, 25119.6000, TIMESTAMPTZ '2026-02-17 14:30:00+00'),
        ( 9, 'de000000-0000-4000-8000-000000000009', 'NVDA', 'SELL', 100, 182.6500, 18265.0000, TIMESTAMPTZ '2026-03-09 14:30:00+00'),
        (10, 'de000000-0000-4000-8000-000000000010', 'MSFT', 'BUY', 40, 383.0000, 15320.0000, TIMESTAMPTZ '2026-03-23 14:30:00+00'),
        (11, 'de000000-0000-4000-8000-000000000011', 'X:BTC-USD', 'BUY', 0.25, 75872.5234, 18968.1309, TIMESTAMPTZ '2026-04-20 14:30:00+00'),
        (12, 'de000000-0000-4000-8000-000000000012', 'FX:EURUSD', 'SELL', 10000, 1.1769, 11769.0000, TIMESTAMPTZ '2026-05-11 14:30:00+00'),
        (13, 'de000000-0000-4000-8000-000000000013', 'NVDA', 'BUY', 150, 208.6400, 31296.0000, TIMESTAMPTZ '2026-06-08 14:30:00+00'),
        (14, 'de000000-0000-4000-8000-000000000014', 'VOO', 'BUY', 30, 690.6200, 20718.6000, TIMESTAMPTZ '2026-07-06 14:30:00+00'),
        (15, 'de000000-0000-4000-8000-000000000015', 'AAPL', 'SELL', 40, 303.4200, 12136.8000, TIMESTAMPTZ '2026-08-03 14:30:00+00'),
        (16, 'de000000-0000-4000-8000-000000000016', 'AAPL', 'BUY', 25, 333.0800, 8327.0000, TIMESTAMPTZ '2026-09-14 14:30:00+00'),
        (17, 'de000000-0000-4000-8000-000000000017', 'X:BTC-USD', 'SELL', 0.25, 85786.5938, 21446.6485, TIMESTAMPTZ '2026-10-05 14:30:00+00');

    INSERT INTO account_trades (external_trade_id, account_id, instrument_id, trade_side, quantity)
    SELECT d.external_trade_id, efe_account, i.instrument_id, d.side, d.quantity
    FROM demo_trades d JOIN instruments i ON i.ticker = d.ticker
    ORDER BY d.n;

    -- each trade goes through the pipeline in a couple of seconds, like a real one
    INSERT INTO account_trade_status (trade_id, status, trade_time)
    SELECT t.trade_id, s.status, d.filled_at - s.before
    FROM demo_trades d
    JOIN account_trades t ON t.external_trade_id = d.external_trade_id
    CROSS JOIN (VALUES ('PENDING', INTERVAL '2 seconds'), ('ACCEPTED', INTERVAL '1 second'),
                       ('FULFILLED', INTERVAL '0 seconds')) AS s (status, before);

    INSERT INTO account_trade_price (trade_id, price_per_unit, total_price)
    SELECT t.trade_id, d.price, d.total
    FROM demo_trades d JOIN account_trades t ON t.external_trade_id = d.external_trade_id;

    -- the cash the buys took and the sells brought in
    UPDATE accounts
    SET balance = balance + (SELECT SUM(CASE WHEN side = 'SELL' THEN total ELSE -total END) FROM demo_trades)
    WHERE account_id = efe_account;

    -- a holdings snapshot after each trade, as the app records them (superseded, so INACTIVE) ...
    INSERT INTO account_holdings (account_id, instrument_id, as_of_date, quantity, status)
    SELECT efe_account, i.instrument_id, d.filled_at,
           SUM(CASE WHEN d.side = 'BUY' THEN d.quantity ELSE -d.quantity END)
               OVER (PARTITION BY d.ticker ORDER BY d.n),
           'INACTIVE'
    FROM demo_trades d JOIN instruments i ON i.ticker = d.ticker;

    -- ... then each instrument's current holding: what efe held already plus the demo trades
    CREATE TEMP TABLE demo_now ON COMMIT DROP AS
    SELECT i.instrument_id,
           COALESCE((SELECT h.quantity FROM account_holdings h
                     WHERE h.account_id = efe_account AND h.instrument_id = i.instrument_id
                       AND h.status = 'ACTIVE'), 0)
               + SUM(CASE WHEN d.side = 'BUY' THEN d.quantity ELSE -d.quantity END) AS quantity
    FROM demo_trades d JOIN instruments i ON i.ticker = d.ticker
    GROUP BY i.instrument_id;

    UPDATE account_holdings h
    SET status = 'INACTIVE'
    FROM demo_now n
    WHERE h.account_id = efe_account AND h.instrument_id = n.instrument_id AND h.status = 'ACTIVE';

    INSERT INTO account_holdings (account_id, instrument_id, as_of_date, quantity, status)
    SELECT efe_account, instrument_id, NOW(), quantity, CASE WHEN quantity > 0 THEN 'ACTIVE' ELSE 'INACTIVE' END
    FROM demo_now;
END
$$;
