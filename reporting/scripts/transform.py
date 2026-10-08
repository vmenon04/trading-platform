"""Every database query used by the reports (main.py) and the dashboard (api.py).

Each function runs one SQL query and returns a pandas DataFrame (a table of rows).

How the queries stay safe from SQL injection
--------------------------------------------
Anything a user types (a client id, a date, a search word) is NEVER pasted into the SQL
text. The SQL only holds placeholders such as %(client_id)s, and the real values are passed
separately in a `params` dictionary. The database driver fills them in safely, so a value
like "'; DROP TABLE trades; --" is treated as plain text, not as SQL.

The only things pasted into SQL text are strings written in this file (like the WHERE
clause built by build_where, or a column name picked from ORDER_SORTS below).

Other things you'll see in the SQL
----------------------------------
- `t` and `i` are short names for the trades and instruments tables (FROM trades t).
- `::float` turns Postgres NUMERIC values into normal numbers that pandas and JSON understand.
- `date_trunc('month', t.trade_time)` rounds a time down to the start of its month (or day, week...).
- `COUNT(*) FILTER (WHERE ...)` counts only the rows that match the condition.
"""
from datetime import timedelta

import pandas as pd

# Trades only count towards traded value once they are accepted or fulfilled.
# Written straight into SQL, which is safe because it's our own text, not user input.
COMPLETED_ONLY = "t.status IN ('ACCEPTED', 'FULFILLED')"

# Each filter the dashboard can send, and the SQL condition it adds.
FILTER_SQL = {
    "start": "t.trade_time >= %(start)s",
    "end": "t.trade_time < %(end)s",
    "statuses": "t.status = ANY(%(statuses)s)",
    "client_id": "t.account_id IN (SELECT account_id FROM client_accounts WHERE client_id = %(client_id)s)",
    "asset_class": "t.instrument_id IN (SELECT instrument_id FROM instruments WHERE asset_class = %(asset_class)s)",
    "instrument_id": "t.instrument_id = %(instrument_id)s",
    "trade_id": "t.trade_id = %(trade_id)s",
    "side": "t.trade_type = %(side)s",
    "min_value": "t.notional >= %(min_value)s",
    "max_value": "t.notional <= %(max_value)s",
}

# The columns the order history table can be sorted by.
# `sort` is put straight into the SQL, so it MUST be one of these keys (anything else is a KeyError).
ORDER_SORTS = {
    "id": "t.trade_id",
    "time": "t.trade_time",
    "status": "t.status",
    "ticker": "i.ticker",
    "quantity": "t.quantity",
    "price": "t.price",
    "value": "t.notional",
}

# pandas' short codes for each period size, used by fill_missing_periods.
PANDAS_PERIOD_CODES = {"day": "D", "week": "W", "month": "M", "quarter": "Q", "year": "Y"}


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def build_where(filters):
    """Turn a dictionary of filters into a SQL WHERE clause and its parameters.

    Filters that are None or an empty list are skipped. For example:
        build_where({"side": "BUY", "client_id": None})
    returns:
        ("WHERE TRUE AND t.trade_type = %(side)s", {"side": "BUY"})

    The clause starts with TRUE so each condition can simply be added with AND,
    and so it still works when there are no filters at all.
    """
    conditions = ["TRUE"]
    params = {}

    for name, condition in FILTER_SQL.items():
        value = filters.get(name)
        if value is None or value == []:
            continue
        conditions.append(condition)
        params[name] = value

    return "WHERE " + " AND ".join(conditions), params


def fill_missing_periods(df, start, end, grain, fill_value=0):
    """Add a row for every period that had no trades, so charts don't skip it.

    The database only returns periods that had trades. If nobody traded in March, there is
    no March row, and a chart would jump straight from February to April.
    `end` is not included (end=April 1st means "up to the end of March").
    """
    # Every period from start up to end, e.g. Jan 1, Feb 1, Mar 1.
    last_day = pd.Timestamp(end) - pd.Timedelta(days=1)
    all_periods = pd.period_range(pd.Timestamp(start), last_day, freq=PANDAS_PERIOD_CODES[grain]).to_timestamp()

    # Line the rows up against that full list. Periods with no row get fill_value.
    df = df.set_index("period")
    df = df.reindex(all_periods, fill_value=fill_value)
    df.index.name = "period"
    return df.reset_index()


# ---------------------------------------------------------------------------
# Date range
# ---------------------------------------------------------------------------

def reporting_window(engine, months_back=12):
    """The default (start, end) dates: the last `months_back` months of trades.

    start is the 1st of a month (but never before the first trade).
    end is the day after the last trade, because end is never included.
    months_back=None means every trade on record.
    """
    sql = "SELECT MIN(trade_time)::date AS first_day, MAX(trade_time)::date AS last_day FROM trades"
    row = pd.read_sql(sql, engine).iloc[0]
    first_day = row["first_day"]
    last_day = row["last_day"]

    if pd.isna(last_day):
        raise RuntimeError("The trades table is empty.")

    end = last_day + timedelta(days=1)
    if months_back is None:
        return first_day, end

    start = (pd.Timestamp(end) - pd.DateOffset(months=months_back)).date()
    start = start.replace(day=1)
    if start < first_day:
        start = first_day
    return start, end


# ---------------------------------------------------------------------------
# Charts over time: one row per period (day, week, month, quarter or year)
#
# `filters` must contain "start" and "end"; it can also contain client_id,
# asset_class and instrument_id. `grain` is the period size, e.g. "month".
# ---------------------------------------------------------------------------

def trade_volume_by_period(engine, filters, grain):
    """Traded value, number of trades, units and active accounts in each period."""
    where_sql, params = build_where(filters)
    params["grain"] = grain
    sql = f"""
        SELECT date_trunc(%(grain)s, t.trade_time)::date AS period,
               COUNT(*)                     AS trade_count,
               SUM(t.notional)::float       AS notional,
               SUM(t.quantity)::float       AS units,
               COUNT(DISTINCT t.account_id) AS active_accounts
        FROM trades t
        {where_sql} AND {COMPLETED_ONLY}
        GROUP BY period
        ORDER BY period
    """
    df = pd.read_sql(sql, engine, params=params, parse_dates=["period"])
    return fill_missing_periods(df, filters["start"], filters["end"], grain)


def volume_by_trade_type(engine, filters, grain):
    """Buy value and sell value in each period, plus net = buy - sell."""
    where_sql, params = build_where(filters)
    params["grain"] = grain
    sql = f"""
        SELECT date_trunc(%(grain)s, t.trade_time)::date AS period,
               COALESCE(SUM(t.notional) FILTER (WHERE t.trade_type = 'BUY'), 0)::float  AS "BUY",
               COALESCE(SUM(t.notional) FILTER (WHERE t.trade_type = 'SELL'), 0)::float AS "SELL",
               COUNT(*) FILTER (WHERE t.trade_type = 'BUY')  AS buy_trades,
               COUNT(*) FILTER (WHERE t.trade_type = 'SELL') AS sell_trades
        FROM trades t
        {where_sql} AND {COMPLETED_ONLY}
        GROUP BY period
        ORDER BY period
    """
    df = pd.read_sql(sql, engine, params=params, parse_dates=["period"])
    df = fill_missing_periods(df, filters["start"], filters["end"], grain)
    df["net"] = df["BUY"] - df["SELL"]
    return df


def status_by_period(engine, filters, grain):
    """How many orders were placed in each period, split by status, plus fill and reject rates.

    Unlike the other charts this counts every status, including PENDING and REJECTED.
    """
    where_sql, params = build_where(filters)
    params["grain"] = grain
    sql = f"""
        SELECT date_trunc(%(grain)s, t.trade_time)::date AS period,
               COUNT(*) FILTER (WHERE t.status = 'PENDING')   AS "PENDING",
               COUNT(*) FILTER (WHERE t.status = 'ACCEPTED')  AS "ACCEPTED",
               COUNT(*) FILTER (WHERE t.status = 'FULFILLED') AS "FULFILLED",
               COUNT(*) FILTER (WHERE t.status = 'REJECTED')  AS "REJECTED",
               COUNT(*)                                       AS orders
        FROM trades t
        {where_sql}
        GROUP BY period
        ORDER BY period
    """
    df = pd.read_sql(sql, engine, params=params, parse_dates=["period"])
    df = fill_missing_periods(df, filters["start"], filters["end"], grain)

    # A period with 0 orders has no rate (you can't divide by 0), so we use NaN
    # ("not a number"), which shows up as null in the JSON and as a gap in the chart.
    orders = df["orders"].replace(0, float("nan"))
    df["fill_rate"] = (df["FULFILLED"] / orders * 100).round(1)
    df["reject_rate"] = (df["REJECTED"] / orders * 100).round(1)
    return df


def price_by_period(engine, filters, grain):
    """Average, lowest and highest traded price in each period (meant for one instrument)."""
    where_sql, params = build_where(filters)
    params["grain"] = grain
    sql = f"""
        SELECT date_trunc(%(grain)s, t.trade_time)::date AS period,
               (SUM(t.notional) / SUM(t.quantity))::float AS avg_price,
               MIN(t.price)::float                       AS low_price,
               MAX(t.price)::float                       AS high_price
        FROM trades t
        {where_sql} AND {COMPLETED_ONLY}
        GROUP BY period
        ORDER BY period
    """
    df = pd.read_sql(sql, engine, params=params, parse_dates=["period"])
    # No trades means no price, so empty periods get NaN (a gap in the chart), not 0.
    return fill_missing_periods(df, filters["start"], filters["end"], grain, fill_value=float("nan"))


# ---------------------------------------------------------------------------
# Breakdowns and rankings
# ---------------------------------------------------------------------------

def volume_by_asset_class(engine, filters):
    """Traded value per asset class (STOCK, ETF...), biggest first, with its % of the total."""
    where_sql, params = build_where(filters)
    sql = f"""
        SELECT i.asset_class,
               COUNT(*)                        AS trade_count,
               SUM(t.notional)::float          AS notional,
               SUM(t.quantity)::float          AS units,
               COUNT(DISTINCT i.instrument_id) AS instruments
        FROM trades t
        JOIN instruments i ON i.instrument_id = t.instrument_id
        {where_sql} AND {COMPLETED_ONLY}
        GROUP BY i.asset_class
        ORDER BY notional DESC
    """
    df = pd.read_sql(sql, engine, params=params)
    df["pct_of_notional"] = (df["notional"] / df["notional"].sum() * 100).round(2)
    return df


def top_clients(engine, filters, top_n=10):
    """The `top_n` clients with the highest traded value."""
    where_sql, params = build_where(filters)
    params["top_n"] = top_n
    sql = f"""
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
        {where_sql} AND {COMPLETED_ONLY}
        GROUP BY c.client_id, c.first_name, c.last_name
        ORDER BY notional DESC
        LIMIT %(top_n)s
    """
    return pd.read_sql(sql, engine, params=params, parse_dates=["last_trade"])


def top_instruments(engine, filters, top_n=10):
    """The `top_n` instruments with the highest traded value."""
    where_sql, params = build_where(filters)
    params["top_n"] = top_n
    sql = f"""
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
        {where_sql} AND {COMPLETED_ONLY}
        GROUP BY i.instrument_id, i.ticker, i.name, i.asset_class
        ORDER BY notional DESC
        LIMIT %(top_n)s
    """
    return pd.read_sql(sql, engine, params=params)


# ---------------------------------------------------------------------------
# Lists of individual trades
# ---------------------------------------------------------------------------

def recent_trades(engine, filters, limit):
    """The newest `limit` trades in any status."""
    where_sql, params = build_where(filters)
    params["limit"] = limit
    sql = f"""
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
        {where_sql}
        ORDER BY t.trade_time DESC, t.trade_id DESC
        LIMIT %(limit)s
    """
    return pd.read_sql(sql, engine, params=params, parse_dates=["trade_time"])


def order_history(engine, filters, sort="time", descending=True, page=1, page_size=50):
    """One page of the admin order table. Returns (total number of matching orders, rows).

    `sort` must be a key of ORDER_SORTS. Page 1 is the first page.
    """
    where_sql, params = build_where(filters)

    # First count every matching order, so the page can show "page 2 of 40".
    count_sql = f"SELECT COUNT(*) AS total FROM trades t {where_sql}"
    total = int(pd.read_sql(count_sql, engine, params=params)["total"][0])

    # Then fetch just the rows for this page. Page 3 with 50 rows per page skips the first 100.
    sort_column = ORDER_SORTS[sort]
    direction = "DESC" if descending else "ASC"
    params["page_size"] = page_size
    params["offset"] = (page - 1) * page_size
    sql = f"""
        SELECT t.trade_id,
               t.trade_time,
               t.status_time,
               t.status,
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
        {where_sql}
        ORDER BY {sort_column} {direction} NULLS LAST, t.trade_id {direction}
        LIMIT %(page_size)s OFFSET %(offset)s
    """
    df = pd.read_sql(sql, engine, params=params, parse_dates=["trade_time", "status_time"])
    return total, df


def order_status_history(engine, trade_id):
    """Every status one order went through, oldest first (e.g. PENDING -> ACCEPTED -> FULFILLED)."""
    sql = """
        SELECT status, status_time
        FROM trade_status_history
        WHERE trade_id = %(trade_id)s
        ORDER BY status_time
    """
    return pd.read_sql(sql, engine, params={"trade_id": trade_id}, parse_dates=["status_time"])


# ---------------------------------------------------------------------------
# Search boxes and small lookups
# ---------------------------------------------------------------------------

def search_clients(engine, search, limit):
    """Clients whose name, email or username contains `search`, or whose id is exactly `search`."""
    sql = """
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
        LIMIT %(limit)s
    """
    # ILIKE is a case-insensitive match; % means "anything here", so %smith% finds "Smithers".
    params = {"pattern": f"%{search}%", "search": search, "limit": limit}
    return pd.read_sql(sql, engine, params=params)


def search_instruments(engine, search, limit, asset_class=None):
    """Instruments whose ticker or name contains `search`, optionally only in one asset class."""
    sql = """
        SELECT instrument_id, ticker, name, asset_class
        FROM instruments
        WHERE (ticker ILIKE %(pattern)s OR name ILIKE %(pattern)s)
          AND (%(asset_class)s IS NULL OR asset_class = %(asset_class)s)
        ORDER BY upper(ticker) = upper(%(search)s) DESC, ticker
        LIMIT %(limit)s
    """
    params = {"pattern": f"%{search}%", "search": search, "asset_class": asset_class or None, "limit": limit}
    return pd.read_sql(sql, engine, params=params)


def asset_classes(engine):
    """Every asset class name, e.g. ["CRYPTO", "ETF", "FOREX", "STOCK"]."""
    df = pd.read_sql("SELECT DISTINCT asset_class FROM instruments ORDER BY asset_class", engine)
    return df["asset_class"].tolist()


def last_sync(engine):
    """The most recent run of sync.py, as a one-row table (empty if it has never run)."""
    sql = "SELECT finished_at, watermark, trades_copied FROM sync_runs ORDER BY run_id DESC LIMIT 1"
    return pd.read_sql(sql, engine, parse_dates=["finished_at", "watermark"])
