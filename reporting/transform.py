from datetime import timedelta

import pandas as pd
from sqlalchemy import text

COUNTED_STATUSES = ["ACCEPTED", "FULFILLED"]
VALID_GRAINS = ("day", "week", "month", "quarter", "year")

PERIOD_CODES = {"day": "D", "week": "W", "month": "M", "quarter": "Q", "year": "Y"}

ORDER_SORTS = {
    "id": "t.trade_id",
    "time": "t.trade_time",
    "status": "t.status",
    "ticker": "i.ticker",
    "quantity": "t.quantity",
    "price": "t.price",
    "value": "t.notional",
}


def where(start=None, end=None, statuses=None, client_id=None, asset_class=None,
          instrument_id=None, trade_id=None, side=None, min_value=None, max_value=None):
    conditions = ["TRUE"]
    params = {}

    if start is not None:
        conditions.append("t.trade_time >= %(start)s")
        params["start"] = start
    if end is not None:
        conditions.append("t.trade_time < %(end)s")
        params["end"] = end
    if statuses:
        conditions.append("t.status = ANY(%(statuses)s)")
        params["statuses"] = list(statuses)
    if client_id is not None:
        conditions.append("t.account_id IN (SELECT account_id FROM client_accounts WHERE client_id = %(client_id)s)")
        params["client_id"] = client_id
    if asset_class is not None:
        conditions.append("t.instrument_id IN (SELECT instrument_id FROM instruments WHERE asset_class = %(asset_class)s)")
        params["asset_class"] = asset_class
    if instrument_id is not None:
        conditions.append("t.instrument_id = %(instrument_id)s")
        params["instrument_id"] = instrument_id
    if trade_id is not None:
        conditions.append("t.trade_id = %(trade_id)s")
        params["trade_id"] = trade_id
    if side is not None:
        conditions.append("t.trade_type = %(side)s")
        params["side"] = side
    if min_value is not None:
        conditions.append("t.notional >= %(min_value)s")
        params["min_value"] = min_value
    if max_value is not None:
        conditions.append("t.notional <= %(max_value)s")
        params["max_value"] = max_value

    return "WHERE " + " AND ".join(conditions), params


def fill_missing_periods(df, start, end, grain, fill_value=0):
    last_day = pd.Timestamp(end) - pd.Timedelta(days=1)
    every_period = pd.period_range(pd.Timestamp(start), last_day, freq=PERIOD_CODES[grain]).to_timestamp()
    df = df.set_index("period").reindex(every_period.rename("period"), fill_value=fill_value)
    return df.reset_index()


def reporting_window(engine, months_back=12):
    with engine.connect() as conn:
        first, last = conn.execute(text("SELECT MIN(trade_time)::date, MAX(trade_time)::date FROM trades")).one()

    if last is None:
        raise RuntimeError("The trades table is empty.")

    end = last + timedelta(days=1)
    if months_back is None:
        return first, end

    start = (pd.Timestamp(end) - pd.DateOffset(months=months_back)).date().replace(day=1)
    return max(start, first), end


def trade_volume_by_period(engine, start, end, grain="month", statuses=COUNTED_STATUSES, **filters):
    where_sql, params = where(start, end, statuses, **filters)
    sql = f"""
        SELECT date_trunc(%(grain)s, t.trade_time)::date AS period,
               COUNT(*)                     AS trade_count,
               SUM(t.notional)::float       AS notional,
               SUM(t.quantity)::float       AS units,
               COUNT(DISTINCT t.account_id) AS active_accounts
        FROM trades t
        {where_sql}
        GROUP BY 1
        ORDER BY 1
    """
    df = pd.read_sql(sql, engine, params={**params, "grain": grain}, parse_dates=["period"])
    return fill_missing_periods(df, start, end, grain)


def volume_by_trade_type(engine, start, end, grain="month", statuses=COUNTED_STATUSES, **filters):
    where_sql, params = where(start, end, statuses, **filters)
    sql = f"""
        SELECT date_trunc(%(grain)s, t.trade_time)::date AS period,
               COALESCE(SUM(t.notional) FILTER (WHERE t.trade_type = 'BUY'), 0)::float  AS "BUY",
               COALESCE(SUM(t.notional) FILTER (WHERE t.trade_type = 'SELL'), 0)::float AS "SELL",
               COUNT(*) FILTER (WHERE t.trade_type = 'BUY')  AS buy_trades,
               COUNT(*) FILTER (WHERE t.trade_type = 'SELL') AS sell_trades
        FROM trades t
        {where_sql}
        GROUP BY 1
        ORDER BY 1
    """
    df = pd.read_sql(sql, engine, params={**params, "grain": grain}, parse_dates=["period"])
    df = fill_missing_periods(df, start, end, grain)
    df["net"] = df["BUY"] - df["SELL"]
    return df


def status_by_period(engine, start, end, grain="month", **filters):
    where_sql, params = where(start, end, **filters)
    sql = f"""
        SELECT date_trunc(%(grain)s, t.trade_time)::date AS period,
               COUNT(*) FILTER (WHERE t.status = 'PENDING')   AS "PENDING",
               COUNT(*) FILTER (WHERE t.status = 'ACCEPTED')  AS "ACCEPTED",
               COUNT(*) FILTER (WHERE t.status = 'FULFILLED') AS "FULFILLED",
               COUNT(*) FILTER (WHERE t.status = 'REJECTED')  AS "REJECTED",
               COUNT(*)                                       AS orders
        FROM trades t
        {where_sql}
        GROUP BY 1
        ORDER BY 1
    """
    df = pd.read_sql(sql, engine, params={**params, "grain": grain}, parse_dates=["period"])
    df = fill_missing_periods(df, start, end, grain)

    orders = df["orders"].replace(0, float("nan"))
    df["fill_rate"] = (df["FULFILLED"] / orders * 100).round(1)
    df["reject_rate"] = (df["REJECTED"] / orders * 100).round(1)
    return df


def price_by_period(engine, start, end, grain="month", statuses=COUNTED_STATUSES, **filters):
    where_sql, params = where(start, end, statuses, **filters)
    sql = f"""
        SELECT date_trunc(%(grain)s, t.trade_time)::date AS period,
               (SUM(t.notional) / SUM(t.quantity))::float AS avg_price,
               MIN(t.price)::float                       AS low_price,
               MAX(t.price)::float                       AS high_price
        FROM trades t
        {where_sql}
        GROUP BY 1
        ORDER BY 1
    """
    df = pd.read_sql(sql, engine, params={**params, "grain": grain}, parse_dates=["period"])
    return fill_missing_periods(df, start, end, grain, fill_value=float("nan"))


def volume_by_asset_class(engine, start, end, statuses=COUNTED_STATUSES, **filters):
    where_sql, params = where(start, end, statuses, **filters)
    sql = f"""
        SELECT i.asset_class,
               COUNT(*)                        AS trade_count,
               SUM(t.notional)::float          AS notional,
               SUM(t.quantity)::float          AS units,
               COUNT(DISTINCT i.instrument_id) AS instruments
        FROM trades t
        JOIN instruments i ON i.instrument_id = t.instrument_id
        {where_sql}
        GROUP BY i.asset_class
        ORDER BY notional DESC
    """
    df = pd.read_sql(sql, engine, params=params)
    df["pct_of_notional"] = (df["notional"] / df["notional"].sum() * 100).round(2)
    return df


def top_clients(engine, start, end, top_n=10, statuses=COUNTED_STATUSES, **filters):
    where_sql, params = where(start, end, statuses, **filters)
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
        {where_sql}
        GROUP BY c.client_id, c.first_name, c.last_name
        ORDER BY notional DESC
        LIMIT %(top_n)s
    """
    return pd.read_sql(sql, engine, params={**params, "top_n": top_n}, parse_dates=["last_trade"])


def top_instruments(engine, start, end, top_n=10, statuses=COUNTED_STATUSES, **filters):
    where_sql, params = where(start, end, statuses, **filters)
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
        {where_sql}
        GROUP BY i.instrument_id, i.ticker, i.name, i.asset_class
        ORDER BY notional DESC
        LIMIT %(top_n)s
    """
    return pd.read_sql(sql, engine, params={**params, "top_n": top_n})


def recent_trades(engine, limit, **filters):
    where_sql, params = where(**filters)
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
        ORDER BY t.trade_time DESC
        LIMIT %(limit)s
    """
    return pd.read_sql(sql, engine, params={**params, "limit": limit}, parse_dates=["trade_time"])


def order_history(engine, sort="time", descending=True, page=1, page_size=50, **filters):
    where_sql, params = where(**filters)

    with engine.connect() as conn:
        total = conn.exec_driver_sql(f"SELECT COUNT(*) FROM trades t {where_sql}", params).scalar()

    direction = "DESC" if descending else "ASC"
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
        ORDER BY {ORDER_SORTS[sort]} {direction} NULLS LAST, t.trade_id {direction}
        LIMIT %(page_size)s OFFSET %(offset)s
    """
    params = {**params, "page_size": page_size, "offset": (page - 1) * page_size}
    df = pd.read_sql(sql, engine, params=params, parse_dates=["trade_time", "status_time"])
    return total, df


def order_status_history(engine, trade_id):
    sql = """
        SELECT status, status_time
        FROM trade_status_history
        WHERE trade_id = %(trade_id)s
        ORDER BY status_time
    """
    return pd.read_sql(sql, engine, params={"trade_id": trade_id}, parse_dates=["status_time"])


def search_clients(engine, search, limit):
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
    params = {"pattern": f"%{search}%", "search": search, "limit": limit}
    return pd.read_sql(sql, engine, params=params)


def search_instruments(engine, search, limit, asset_class=None):
    class_condition = "AND asset_class = %(asset_class)s" if asset_class else ""
    sql = f"""
        SELECT instrument_id, ticker, name, asset_class
        FROM instruments
        WHERE (ticker ILIKE %(pattern)s OR name ILIKE %(pattern)s)
          {class_condition}
        ORDER BY upper(ticker) = upper(%(search)s) DESC, ticker
        LIMIT %(limit)s
    """
    params = {"pattern": f"%{search}%", "search": search, "asset_class": asset_class, "limit": limit}
    return pd.read_sql(sql, engine, params=params)


def asset_classes(engine):
    with engine.connect() as conn:
        return list(conn.execute(text("SELECT DISTINCT asset_class FROM instruments ORDER BY 1")).scalars())


def last_sync(engine):
    sql = "SELECT finished_at, watermark, trades_copied FROM sync_runs ORDER BY run_id DESC LIMIT 1"
    with engine.connect() as conn:
        row = conn.execute(text(sql)).mappings().first()
    return dict(row) if row else None


def period_labels(df, column="period", fmt="%b %Y"):
    return pd.to_datetime(df[column]).dt.strftime(fmt)


def shorten(names, width=22):
    trimmed = names.str.slice(0, width).str.strip()
    return trimmed + names.str.len().gt(width).map({True: "…", False: ""})
