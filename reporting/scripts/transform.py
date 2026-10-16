"""Runs the queries in reporting/queries.sql and turns each result into a pandas DataFrame.

The SQL lives in queries.sql; this file only picks a query, fills in its values,
and tidies the result (adding empty periods, working out rates and percentages).
Used by the dashboard (api.py) and the reporting pack (main.py).

How the queries stay safe from SQL injection
--------------------------------------------
Anything a user types (a client id, a date, a search word) is NEVER pasted into the SQL
text. The SQL only holds placeholders such as %(client_id)s, and the real values are passed
separately in a `params` dictionary. The database driver fills them in safely, so a value
like "'; DROP TABLE trades; --" is treated as plain text, not as SQL.

The only things pasted into SQL text are the {blanks} in queries.sql, and those are always
strings written in this file (the WHERE clause built by build_where, or a column name
picked from ORDER_SORTS below).
"""
import re
from datetime import timedelta

import pandas as pd

from database_connection import REPORTING_DIR

QUERIES_FILE = REPORTING_DIR / "queries.sql"

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
# Loading queries.sql
# ---------------------------------------------------------------------------

def load_queries(path):
    """Read queries.sql into a dictionary of {name: sql}.

    Each query starts with a "-- name: something" line and runs until the next one.
    Comment lines are dropped, so a stray % or {} in a comment can't confuse the driver.
    """
    queries = {}
    name = None
    for line in path.read_text().splitlines():
        header = re.match(r"--\s*name:\s*(\w+)", line.strip())
        if header:
            name = header.group(1)
            queries[name] = []
        elif name and line.strip() and not line.strip().startswith("--"):
            queries[name].append(line)
    return {name: "\n".join(lines).rstrip().rstrip(";") for name, lines in queries.items()}


QUERIES = load_queries(QUERIES_FILE)


def query(name, **blanks):
    """The SQL for `name` from queries.sql, with its {blanks} filled in.

    Only pass blanks written in this file (never user input). Every other value goes in `params`.
    """
    return QUERIES[name].format(**blanks)


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


def by_period(engine, name, filters, grain):
    """Run one of the "one row per period" queries with the filters and period size."""
    where, params = build_where(filters)
    params["grain"] = grain
    return pd.read_sql(query(name, where=where), engine, params=params, parse_dates=["period"])


# ---------------------------------------------------------------------------
# Date range
# ---------------------------------------------------------------------------

def reporting_window(engine, months_back=12):
    """The default (start, end) dates: the last `months_back` months of trades.

    start is the 1st of a month (but never before the first trade).
    end is the day after the last trade, because end is never included.
    months_back=None means every trade on record.
    """
    row = pd.read_sql(query("trade_date_range"), engine).iloc[0]
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
    df = by_period(engine, "trade_volume_by_period", filters, grain)
    return fill_missing_periods(df, filters["start"], filters["end"], grain)


def volume_by_trade_type(engine, filters, grain):
    """Buy value and sell value in each period, plus net = buy - sell."""
    df = by_period(engine, "volume_by_trade_type", filters, grain)
    df = fill_missing_periods(df, filters["start"], filters["end"], grain)
    df["net"] = df["BUY"] - df["SELL"]
    return df


def price_by_period(engine, filters, grain):
    """Average, lowest and highest traded price in each period (meant for one instrument)."""
    df = by_period(engine, "price_by_period", filters, grain)
    # No trades means no price, so empty periods get NaN (a gap in the chart), not 0.
    return fill_missing_periods(df, filters["start"], filters["end"], grain, fill_value=float("nan"))


# ---------------------------------------------------------------------------
# Breakdowns and rankings
# ---------------------------------------------------------------------------

def volume_by_asset_class(engine, filters):
    """Traded value per asset class (STOCK, ETF...), biggest first, with its % of the total."""
    where, params = build_where(filters)
    df = pd.read_sql(query("volume_by_asset_class", where=where), engine, params=params)
    df["pct_of_notional"] = (df["notional"] / df["notional"].sum() * 100).round(2)
    return df


def top_clients(engine, filters, top_n=10):
    """The `top_n` clients with the highest traded value."""
    where, params = build_where(filters)
    params["top_n"] = top_n
    return pd.read_sql(query("top_clients", where=where), engine, params=params, parse_dates=["last_trade"])


def top_instruments(engine, filters, top_n=10):
    """The `top_n` instruments with the highest traded value."""
    where, params = build_where(filters)
    params["top_n"] = top_n
    return pd.read_sql(query("top_instruments", where=where), engine, params=params)


# ---------------------------------------------------------------------------
# Portfolio value: what each client holds right now (no date range)
# ---------------------------------------------------------------------------

def client_portfolios(engine, filters, page=1, page_size=50):
    """One page of clients, highest portfolio value first. Returns (summary, rows).

    `filters` can hold client_id, min_value, max_value (dollars) and quartile (1-4);
    missing ones are ignored. Each row has cash, invested, portfolio_value and
    quartile (4 = top 25% of clients). `summary` is {"total": clients matching,
    "total_value": their combined portfolio value}.
    """
    params = {name: filters.get(name) for name in ["client_id", "min_value", "max_value", "quartile"]}
    params["page_size"] = page_size
    params["offset"] = (page - 1) * page_size
    df = pd.read_sql(query("client_portfolios", portfolios=QUERIES["portfolios"]), engine, params=params)

    # The query repeats the match count and combined value on every row; pull them out once.
    summary = {
        "total": int(df["matching"].iloc[0]) if not df.empty else 0,
        "total_value": float(df["matching_value"].iloc[0]) if not df.empty else 0.0,
    }
    return summary, df.drop(columns=["matching", "matching_value"], errors="ignore")


def portfolio_tiers(engine):
    """How many clients fall in each quartile, and the value range of each, top quartile first."""
    return pd.read_sql(query("portfolio_tiers", portfolios=QUERIES["portfolios"]), engine)


# ---------------------------------------------------------------------------
# Lists of individual trades
# ---------------------------------------------------------------------------

def recent_trades(engine, filters, limit):
    """The newest `limit` trades in any status."""
    where, params = build_where(filters)
    params["limit"] = limit
    return pd.read_sql(query("recent_trades", where=where), engine, params=params, parse_dates=["trade_time"])


def order_history(engine, filters, sort="time", descending=True, page=1, page_size=50):
    """One page of the admin order table. Returns (total number of matching orders, rows).

    `sort` must be a key of ORDER_SORTS. Page 1 is the first page.
    """
    where, params = build_where(filters)

    # First count every matching order, so the page can show "page 2 of 40".
    total = int(pd.read_sql(query("order_count", where=where), engine, params=params)["total"][0])

    # Then fetch just the rows for this page. Page 3 with 50 rows per page skips the first 100.
    sql = query(
        "order_history",
        where=where,
        sort_column=ORDER_SORTS[sort],
        direction="DESC" if descending else "ASC",
    )
    params["page_size"] = page_size
    params["offset"] = (page - 1) * page_size
    df = pd.read_sql(sql, engine, params=params, parse_dates=["trade_time", "status_time"])
    return total, df


def order_status_history(engine, trade_id):
    """Every status one order went through, oldest first (e.g. PENDING -> ACCEPTED -> FULFILLED)."""
    return pd.read_sql(
        query("order_status_history"), engine, params={"trade_id": trade_id}, parse_dates=["status_time"]
    )


# ---------------------------------------------------------------------------
# Search boxes and small lookups
# ---------------------------------------------------------------------------

def search_clients(engine, search, limit):
    """Clients whose name, email or username contains `search`, or whose id is exactly `search`."""
    # % means "anything here", so %smith% finds "Smithers".
    params = {"pattern": f"%{search}%", "search": search, "limit": limit}
    return pd.read_sql(query("search_clients"), engine, params=params)


def search_instruments(engine, search, limit, asset_class=None):
    """Instruments whose ticker or name contains `search`, optionally only in one asset class."""
    params = {"pattern": f"%{search}%", "search": search, "asset_class": asset_class or None, "limit": limit}
    return pd.read_sql(query("search_instruments"), engine, params=params)


def asset_classes(engine):
    """Every asset class name, e.g. ["CRYPTO", "ETF", "FOREX", "STOCK"]."""
    return pd.read_sql(query("asset_classes"), engine)["asset_class"].tolist()


def last_sync(engine):
    """The most recent run of sync.py, as a one-row table (empty if it has never run)."""
    return pd.read_sql(query("last_sync"), engine, parse_dates=["finished_at", "watermark"])
