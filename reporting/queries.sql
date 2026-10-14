-- Every query the dashboard (api.py) and the reporting pack (main.py) run against the analytics database.
--
-- transform.py loads this file and finds each query by the "-- name:" line above it.
--
-- Two kinds of blanks:
--   %(client_id)s  a value from the user. The database driver fills it in safely, never pasted as text.
--   {where}        a piece of SQL written by transform.py itself (the WHERE clause from build_where,
--                  a sort column from ORDER_SORTS, the portfolios query). Never user input.
--
-- Shorthand used below:
--   t / i / c                      trades / instruments / clients (FROM trades t)
--   ::float                        Postgres NUMERIC to a plain number that pandas and JSON understand
--   date_trunc('month', time)      rounds a time down to the start of its month (or day, week...)
--   COUNT(*) FILTER (WHERE ...)    counts only the rows that match
--   t.status IN ('ACCEPTED', 'FULFILLED')   trades only count towards traded value once accepted or fulfilled


-- ===========================================================================
-- Date range
-- ===========================================================================

-- name: trade_date_range
SELECT MIN(trade_time)::date AS first_day,
       MAX(trade_time)::date AS last_day
FROM trades;


-- ===========================================================================
-- Charts over time: one row per period
-- ===========================================================================

-- name: trade_volume_by_period
SELECT date_trunc(%(grain)s, t.trade_time)::date AS period,
       COUNT(*)                     AS trade_count,
       SUM(t.notional)::float       AS notional,
       SUM(t.quantity)::float       AS units,
       COUNT(DISTINCT t.account_id) AS active_accounts
FROM trades t
{where} AND t.status IN ('ACCEPTED', 'FULFILLED')
GROUP BY period
ORDER BY period;

-- name: volume_by_trade_type
SELECT date_trunc(%(grain)s, t.trade_time)::date AS period,
       COALESCE(SUM(t.notional) FILTER (WHERE t.trade_type = 'BUY'), 0)::float  AS "BUY",
       COALESCE(SUM(t.notional) FILTER (WHERE t.trade_type = 'SELL'), 0)::float AS "SELL",
       COUNT(*) FILTER (WHERE t.trade_type = 'BUY')  AS buy_trades,
       COUNT(*) FILTER (WHERE t.trade_type = 'SELL') AS sell_trades
FROM trades t
{where} AND t.status IN ('ACCEPTED', 'FULFILLED')
GROUP BY period
ORDER BY period;

-- name: price_by_period
SELECT date_trunc(%(grain)s, t.trade_time)::date AS period,
       (SUM(t.notional) / SUM(t.quantity))::float AS avg_price,
       MIN(t.price)::float                       AS low_price,
       MAX(t.price)::float                       AS high_price
FROM trades t
{where} AND t.status IN ('ACCEPTED', 'FULFILLED')
GROUP BY period
ORDER BY period;


-- ===========================================================================
-- Breakdowns and rankings
-- ===========================================================================

-- name: volume_by_asset_class
SELECT i.asset_class,
       COUNT(*)                        AS trade_count,
       SUM(t.notional)::float          AS notional,
       SUM(t.quantity)::float          AS units,
       COUNT(DISTINCT i.instrument_id) AS instruments
FROM trades t
JOIN instruments i ON i.instrument_id = t.instrument_id
{where} AND t.status IN ('ACCEPTED', 'FULFILLED')
GROUP BY i.asset_class
ORDER BY notional DESC;

-- name: top_clients
SELECT c.client_id,
       c.first_name || ' ' || c.last_name AS client_name,
       COUNT(*)                           AS trade_count,
       SUM(t.notional)::float             AS notional,
       SUM(t.quantity)::float             AS units,
       COUNT(DISTINCT t.account_id)       AS accounts,
       COUNT(DISTINCT t.instrument_id)    AS instruments,
       MAX(t.trade_time)::timestamp       AS last_trade
FROM trades t
JOIN client_accounts ca ON ca.account_id = t.account_id
JOIN clients c          ON c.client_id = ca.client_id
{where} AND t.status IN ('ACCEPTED', 'FULFILLED')
GROUP BY c.client_id, c.first_name, c.last_name
ORDER BY notional DESC
LIMIT %(top_n)s;

-- name: top_instruments
SELECT i.instrument_id,
       i.ticker,
       i.name,
       i.asset_class,
       COUNT(*)                                    AS trade_count,
       SUM(t.notional)::float                      AS notional,
       SUM(t.quantity)::float                      AS units,
       COUNT(DISTINCT t.account_id)                AS accounts,
       (SUM(t.notional) / SUM(t.quantity))::float  AS avg_price
FROM trades t
JOIN instruments i ON i.instrument_id = t.instrument_id
{where} AND t.status IN ('ACCEPTED', 'FULFILLED')
GROUP BY i.instrument_id, i.ticker, i.name, i.asset_class
ORDER BY notional DESC
LIMIT %(top_n)s;


-- ===========================================================================
-- Portfolio value: what each client holds right now (no date range)
--
-- value = cash (account balances) + every holding x its last fulfilled price.
-- Balances and holdings come from sync.py; prices come from the trades the consumer writes.
-- ===========================================================================

-- name: portfolios
-- One row per client with at least one account, ranked into quartiles (4 = top 25%).
-- Holdings and cash are summed in separate steps: joining both at once would repeat
-- each balance once per holding and inflate the cash.
WITH prices AS (
    -- DISTINCT ON keeps only the first row per instrument: the newest fulfilled trade.
    SELECT DISTINCT ON (instrument_id) instrument_id, price
    FROM trades
    WHERE status = 'FULFILLED' AND price IS NOT NULL
    ORDER BY instrument_id, status_time DESC
),
holdings AS (
    -- account_holdings keeps a snapshot per date; take the newest one per position.
    SELECT DISTINCT ON (account_id, instrument_id) account_id, instrument_id, quantity, status
    FROM account_holdings
    ORDER BY account_id, instrument_id, as_of_date DESC
),
invested AS (
    SELECT ca.client_id,
           SUM(h.quantity * COALESCE(p.price, 0)) AS invested,
           COUNT(*) FILTER (WHERE p.price IS NULL) AS unpriced_positions
    FROM client_accounts ca
    JOIN holdings h     ON h.account_id = ca.account_id AND h.status = 'ACTIVE'
    LEFT JOIN prices p  ON p.instrument_id = h.instrument_id
    GROUP BY ca.client_id
),
cash AS (
    SELECT ca.client_id, SUM(a.balance) AS cash
    FROM client_accounts ca
    JOIN accounts a ON a.account_id = ca.account_id
    GROUP BY ca.client_id
),
portfolios AS (
    SELECT c.client_id,
           c.first_name || ' ' || c.last_name           AS client_name,
           cash.cash                                    AS cash,
           COALESCE(invested.invested, 0)               AS invested,
           cash.cash + COALESCE(invested.invested, 0)   AS portfolio_value,
           COALESCE(invested.unpriced_positions, 0)     AS unpriced_positions
    FROM clients c
    JOIN cash          ON cash.client_id = c.client_id
    LEFT JOIN invested ON invested.client_id = c.client_id
)
SELECT client_id,
       client_name,
       cash::float,
       invested::float,
       portfolio_value::float,
       unpriced_positions,
       NTILE(4) OVER (ORDER BY portfolio_value) AS quartile
FROM portfolios;

-- name: client_portfolios
-- Quartiles are worked out across every client inside the portfolios query first, so
-- filtering afterwards still compares each client to everyone else.
-- matching / matching_value are worked out before LIMIT, over every client that passes the filters.
SELECT *,
       COUNT(*) OVER ()                    AS matching,
       SUM(portfolio_value) OVER ()::float AS matching_value
FROM ({portfolios}) ranked
WHERE (%(client_id)s IS NULL OR client_id = %(client_id)s)
  AND (%(min_value)s IS NULL OR portfolio_value >= %(min_value)s)
  AND (%(max_value)s IS NULL OR portfolio_value <= %(max_value)s)
  AND (%(quartile)s IS NULL OR quartile = %(quartile)s)
ORDER BY portfolio_value DESC, client_id
LIMIT %(page_size)s OFFSET %(offset)s;

-- name: portfolio_tiers
SELECT quartile,
       COUNT(*)                      AS clients,
       SUM(portfolio_value)::float   AS total_value,
       MIN(portfolio_value)::float   AS min_value,
       MAX(portfolio_value)::float   AS max_value
FROM ({portfolios}) ranked
GROUP BY quartile
ORDER BY quartile DESC;


-- ===========================================================================
-- Lists of individual trades
-- ===========================================================================

-- name: recent_trades
SELECT t.trade_id,
       t.trade_time,
       t.account_id,
       i.ticker,
       i.asset_class,
       t.trade_type,
       t.quantity::float,
       t.price::float,
       t.notional::float,
       t.status
FROM trades t
JOIN instruments i ON i.instrument_id = t.instrument_id
{where}
ORDER BY t.trade_time DESC, t.trade_id DESC
LIMIT %(limit)s;

-- name: order_count
SELECT COUNT(*) AS total
FROM trades t
{where};

-- name: order_history
-- Page 3 with 50 rows per page skips the first 100 (OFFSET).
SELECT t.trade_id,
       t.trade_time,
       t.status_time,
       t.status,
       t.reject_reason,
       t.trade_type,
       t.account_id,
       (SELECT string_agg(c.first_name || ' ' || c.last_name, ', ' ORDER BY c.client_id)
        FROM client_accounts ca
        JOIN clients c ON c.client_id = ca.client_id
        WHERE ca.account_id = t.account_id) AS clients,
       i.ticker,
       i.name AS instrument,
       i.asset_class,
       t.quantity::float,
       t.price::float,
       t.notional::float
FROM trades t
JOIN instruments i ON i.instrument_id = t.instrument_id
{where}
ORDER BY {sort_column} {direction} NULLS LAST, t.trade_id {direction}
LIMIT %(page_size)s OFFSET %(offset)s;

-- name: order_status_history
SELECT status, status_time
FROM trade_status_history
WHERE trade_id = %(trade_id)s
ORDER BY status_time;


-- ===========================================================================
-- Search boxes and small lookups
-- ===========================================================================

-- name: search_clients
-- ILIKE is a case-insensitive match. transform.py wraps the search in wildcards, so "smith" finds "Smithers".
SELECT client_id,
       first_name || ' ' || last_name AS name,
       email,
       username
FROM clients
WHERE first_name || ' ' || last_name ILIKE %(pattern)s
   OR email ILIKE %(pattern)s
   OR username ILIKE %(pattern)s
   OR client_id::text = %(search)s
ORDER BY client_id::text = %(search)s DESC, last_name, first_name
LIMIT %(limit)s;

-- name: search_instruments
SELECT instrument_id, ticker, name, asset_class
FROM instruments
WHERE (ticker ILIKE %(pattern)s OR name ILIKE %(pattern)s)
  AND (%(asset_class)s IS NULL OR asset_class = %(asset_class)s)
ORDER BY upper(ticker) = upper(%(search)s) DESC, ticker
LIMIT %(limit)s;

-- name: asset_classes
SELECT DISTINCT asset_class
FROM instruments
ORDER BY asset_class;

-- name: last_sync
SELECT finished_at, watermark, trades_copied
FROM sync_runs
ORDER BY run_id DESC
LIMIT 1;
