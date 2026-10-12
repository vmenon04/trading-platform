"""Every DataFrame the deliverable needs, built straight from the database.

One function per report. Each takes the engine, runs its query, and returns a
ready-to-chart DataFrame with float columns and real dates.

Grouping happens in SQL, so these return tens of rows no matter how large
account_trades grows.
"""
from __future__ import annotations

from datetime import date

import pandas as pd
from sqlalchemy import text
from sqlalchemy.engine import Engine

# Which trade statuses count as real volume.
# 'rejected' never happened; 'pending' has not happened yet.
COUNTED_STATUSES = ("accepted", "fulfilled")

VALID_GRAINS = ("day", "week", "month", "quarter", "year")

_WINDOW = """
    WHERE t.trade_time >= %(start)s
      AND t.trade_time <  %(end)s
      AND t.status      = ANY(%(statuses)s)
"""


# ---------------------------------------------------------------------
# helpers
# ---------------------------------------------------------------------
def reporting_window(engine: Engine, months_back: int | None = 12) -> tuple[date, date]:
    """Derive the date range from the data itself, so the report widens as the
    database grows. Returns (start, end) with end exclusive.
    months_back=None means every trade on record.
    """
    with engine.connect() as conn:
        row = conn.execute(text(
            "SELECT MIN(trade_time)::date AS first, MAX(trade_time)::date AS last "
            "FROM account_trades"
        )).one()

    if row.last is None:
        raise RuntimeError("account_trades is empty — nothing to report on.")

    end = date.fromordinal(row.last.toordinal() + 1)
    if months_back is None:
        return row.first, end

    year, month = end.year, end.month - months_back
    while month <= 0:
        year, month = year - 1, month + 12
    return max(date(year, month, 1), row.first), end


def _params(start: date, end: date, statuses=COUNTED_STATUSES, **extra) -> dict:
    return {"start": start, "end": end, "statuses": list(statuses), **extra}


def _floats(df: pd.DataFrame, *cols: str) -> pd.DataFrame:
    """psycopg2 hands back NUMERIC as Decimal; matplotlib needs floats."""
    for col in cols:
        if col in df.columns:
            df[col] = df[col].astype(float)
    return df


def _check_grain(grain: str) -> str:
    if grain not in VALID_GRAINS:
        raise ValueError(f"grain must be one of {VALID_GRAINS}, got {grain!r}")
    return grain


# ---------------------------------------------------------------------
# 1. Trade volume by period
# ---------------------------------------------------------------------
def trade_volume_by_period(engine: Engine, start: date, end: date,
                           grain: str = "month",
                           statuses=COUNTED_STATUSES) -> pd.DataFrame:
    """period | trade_count | notional | units | active_accounts"""
    sql = f"""
        SELECT date_trunc('{_check_grain(grain)}', t.trade_time)::date AS period,
               COUNT(*)                     AS trade_count,
               SUM(t.quantity * t.price)    AS notional,
               SUM(t.quantity)              AS units,
               COUNT(DISTINCT t.account_id) AS active_accounts
        FROM account_trades t
        {_WINDOW}
        GROUP BY 1
        ORDER BY 1
    """
    df = pd.read_sql(sql, engine, params=_params(start, end, statuses),
                     parse_dates=["period"])
    return _floats(df, "notional", "units")


# ---------------------------------------------------------------------
# 2. Trade value / volume per customer (top N)
# ---------------------------------------------------------------------
def top_clients(engine: Engine, start: date, end: date, top_n: int = 10,
                statuses=COUNTED_STATUSES) -> pd.DataFrame:
    """client_id | client_name | trade_count | notional | units | accounts | instruments | last_trade

    Clients reach trades through client_accounts, so trades on accounts with no
    client mapping are not counted here.
    """
    sql = """
        SELECT c.client_id,
               c.name                          AS client_name,
               COUNT(*)                        AS trade_count,
               SUM(t.quantity * t.price)       AS notional,
               SUM(t.quantity)                 AS units,
               COUNT(DISTINCT t.account_id)    AS accounts,
               COUNT(DISTINCT t.instrument_id) AS instruments,
               MAX(t.trade_time)               AS last_trade
        FROM account_trades t
        JOIN client_accounts ca ON ca.account_id = t.account_id
        JOIN clients         c  ON c.client_id   = ca.client_id
    """ + _WINDOW + """
        GROUP BY c.client_id, c.name
        ORDER BY notional DESC
        LIMIT %(top_n)s
    """
    df = pd.read_sql(sql, engine, params=_params(start, end, statuses, top_n=top_n),
                     parse_dates=["last_trade"])
    # trade_time is TIMESTAMPTZ; Excel cannot store tz-aware values
    if getattr(df["last_trade"].dtype, "tz", None) is not None:
        df["last_trade"] = df["last_trade"].dt.tz_localize(None)
    return _floats(df, "notional", "units")


# ---------------------------------------------------------------------
# 3. Trade volume per asset class
# ---------------------------------------------------------------------
def volume_by_asset_class(engine: Engine, start: date, end: date,
                          statuses=COUNTED_STATUSES) -> pd.DataFrame:
    """asset_class | trade_count | notional | units | instruments | pct_of_notional"""
    sql = """
        SELECT i.asset_class,
               COUNT(*)                        AS trade_count,
               SUM(t.quantity * t.price)       AS notional,
               SUM(t.quantity)                 AS units,
               COUNT(DISTINCT i.instrument_id) AS instruments
        FROM account_trades t
        JOIN instruments i ON i.instrument_id = t.instrument_id
    """ + _WINDOW + """
        GROUP BY i.asset_class
        ORDER BY notional DESC
    """
    df = _floats(pd.read_sql(sql, engine, params=_params(start, end, statuses)),
                 "notional", "units")
    total = df["notional"].sum()
    df["pct_of_notional"] = (df["notional"] / total * 100).round(2) if total else 0.0
    return df


# ---------------------------------------------------------------------
# 4. Trade volume by buy / sell
# ---------------------------------------------------------------------
def volume_by_trade_type(engine: Engine, start: date, end: date,
                         grain: str = "month",
                         statuses=COUNTED_STATUSES) -> pd.DataFrame:
    """period | BUY | SELL | net | buy_trades | sell_trades — one row per period,
    already pivoted for a grouped bar chart."""
    sql = f"""
        SELECT date_trunc('{_check_grain(grain)}', t.trade_time)::date AS period,
               t.trade_type,
               COUNT(*)                  AS trade_count,
               SUM(t.quantity * t.price) AS notional
        FROM account_trades t
        {_WINDOW}
        GROUP BY 1, 2
        ORDER BY 1, 2
    """
    long = _floats(pd.read_sql(sql, engine, params=_params(start, end, statuses),
                               parse_dates=["period"]), "notional")

    wide = long.pivot(index="period", columns="trade_type", values="notional").fillna(0.0)
    counts = long.pivot(index="period", columns="trade_type", values="trade_count").fillna(0)
    for side in ("BUY", "SELL"):
        if side not in wide.columns:
            wide[side], counts[side] = 0.0, 0

    out = wide[["BUY", "SELL"]].copy()
    out["net"] = out["BUY"] - out["SELL"]
    out["buy_trades"] = counts["BUY"].astype(int)
    out["sell_trades"] = counts["SELL"].astype(int)
    out.columns.name = None
    return out.sort_index().reset_index()


# ---------------------------------------------------------------------
# shared formatting helpers used by charts.py
# ---------------------------------------------------------------------
def period_labels(df: pd.DataFrame, column: str = "period",
                  fmt: str = "%b %Y") -> pd.Series:
    return pd.to_datetime(df[column]).dt.strftime(fmt)


def shorten(names: pd.Series, width: int = 22) -> pd.Series:
    trimmed = names.str.slice(0, width).str.strip()
    return trimmed + names.str.len().gt(width).map({True: "…", False: ""})